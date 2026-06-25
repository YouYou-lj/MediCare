package com.medicare.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicare.config.AiProperties;
import com.medicare.config.AiTaskExecutorConfig.AiTaskExecutors;
import com.medicare.dto.AiChatRequest;
import com.medicare.dto.AiChatResponse;
import com.medicare.entity.AiChatSession;
import com.medicare.entity.SysUser;
import com.medicare.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private static final String AGENT_SKILL_PATH = "ai/medicare-agent-skill.md";
    private static final String SAFETY_NOTICE = "\n\n---\n\n> **提示：AI 回复仅用于门诊系统操作和信息整理辅助，不能替代医生诊断或处方判断。**";
    private static final int RAG_TOP_K = 5;
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[引用(\\d+)]");

    private final AiProperties aiProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final LiveBusinessContextService liveBusinessContextService;
    private final AiChatHistoryService aiChatHistoryService;
    private final RagService ragService;
    private final AiTaskExecutors aiTaskExecutors;
    private volatile String cachedAgentSkill;

    public AiChatResponse chat(AiChatRequest request, SysUser currentUser) {
        String provider = normalizeProvider();
        if (!isOpenAiCompatibleProvider(provider)) {
            throw new BusinessException(500, "不支持的 AI provider：" + provider);
        }
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BusinessException(500, "AI API Key 未配置，请检查 application-secret.yml");
        }

        List<AiChatResponse.AiReference> references;
        try {
            references = retrieveReferences(request.getMessage(), request.getFileSourcePath());
        } catch (Exception e) {
            log.warn("AI 助手前置检索失败，本次对话将不携带引用：{}", e.getMessage(), e);
            references = List.of();
        }

        String answer;
        try {
            answer = callOpenAiCompatibleApiAsync(
                    buildSystemPrompt(currentUser, references),
                    buildUserPrompt(request));
            answer = sanitizeCitations(answer, references);
        } catch (RestClientResponseException e) {
            log.warn("百炼 AI 调用失败，status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(502, "百炼 AI 调用失败：" + extractProviderError(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            log.warn("百炼 AI 调用超时或网络异常", e);
            throw new BusinessException(504, "百炼 AI 调用超时，请稍后重试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 调用异常", e);
            throw new BusinessException(500, "AI 调用异常：" + e.getMessage());
        }

        String fullAnswer = answer + SAFETY_NOTICE;
        String sessionKey = null;
        try {
            sessionKey = aiChatHistoryService.saveExchange(
                    currentUser.getId(), request.getSessionId(), request.getMessage(), fullAnswer, references);
        } catch (Exception e) {
            log.warn("AI 对话历史保存失败，不影响本次回答：{}", e.getMessage());
        }

        return AiChatResponse.builder()
                .answer(fullAnswer)
                .sessionId(sessionKey)
                .provider(provider)
                .model(aiProperties.getModel())
                .references(references)
                .build();
    }

    public SseEmitter streamChat(AiChatRequest request, SysUser currentUser) {
        validateProviderConfig();

        SseEmitter emitter = new SseEmitter(aiProperties.getTimeout().toMillis() + 60000);
        CompletableFuture.runAsync(() -> {
            AiChatSession session = null;
            StringBuilder answerBuffer = new StringBuilder();
            List<AiChatResponse.AiReference> references = List.of();
            try {
                try {
                    session = aiChatHistoryService.ensureSession(
                            currentUser.getId(), request.getSessionId(), request.getMessage());
                    aiChatHistoryService.saveMessage(session.getId(), "user", request.getMessage());
                } catch (Exception historyEx) {
                    log.warn("AI 对话历史保存失败，继续流式输出：{}", historyEx.getMessage());
                }

                try {
                    references = retrieveReferences(request.getMessage(), request.getFileSourcePath());
                } catch (Exception e) {
                    log.warn("AI 助手前置检索失败，流式输出将不携带引用：{}", e.getMessage(), e);
                    sendStreamEvent(emitter, "references_error", e.getMessage() == null ? "检索失败" : e.getMessage());
                    references = List.of();
                }
                // 始终发送 references 事件，让前端区分"检索为空"与"检索失败"
                sendStreamEvent(emitter, "references", objectMapper.writeValueAsString(references));

                String systemPrompt = buildSystemPrompt(currentUser, references);
                String userPrompt = buildUserPrompt(request);
                callOpenAiCompatibleStream(systemPrompt, userPrompt, emitter, answerBuffer);
                answerBuffer.append(SAFETY_NOTICE);
                sendStreamEvent(emitter, "chunk", SAFETY_NOTICE);

                String finalAnswer = sanitizeCitations(answerBuffer.toString(), references);
                if (session != null) {
                    try {
                        aiChatHistoryService.saveMessage(session.getId(), "assistant", finalAnswer, references);
                        aiChatHistoryService.touchSession(session.getId());
                    } catch (Exception historyEx) {
                        log.warn("AI 助手消息保存失败：{}", historyEx.getMessage());
                    }
                }

                Map<String, Object> doneMeta = new java.util.HashMap<>();
                doneMeta.put("provider", normalizeProvider());
                doneMeta.put("model", aiProperties.getModel());
                if (session != null) {
                    doneMeta.put("sessionId", session.getSessionKey());
                }
                if (!references.isEmpty()) {
                    doneMeta.put("references", references);
                }
                sendStreamEvent(emitter, "done", objectMapper.writeValueAsString(doneMeta));
                emitter.complete();
            } catch (RestClientResponseException e) {
                log.warn("百炼 AI 流式调用失败，status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
                completeStreamWithError(emitter, "百炼 AI 调用失败：" + extractProviderError(e.getResponseBodyAsString()));
            } catch (BusinessException e) {
                completeStreamWithError(emitter, e.getMessage());
            } catch (AsyncRequestNotUsableException e) {
                log.debug("AI 流式连接已由客户端关闭");
                emitter.complete();
            } catch (Exception e) {
                log.error("AI 流式调用异常", e);
                completeStreamWithError(emitter, "AI 调用异常：" + e.getMessage());
            }
        }, aiTaskExecutors.generationExecutor());
        return emitter;
    }

    private String callOpenAiCompatibleApiAsync(String systemPrompt, String userPrompt) {
        try {
            return CompletableFuture.supplyAsync(
                    () -> callOpenAiCompatibleApi(systemPrompt, userPrompt),
                    aiTaskExecutors.generationExecutor()
            ).join();
        } catch (CompletionException e) {
            throw unwrapCompletionException(e);
        }
    }

    private RuntimeException unwrapCompletionException(CompletionException e) {
        Throwable cause = e.getCause() == null ? e : e.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new BusinessException(500, "AI 异步任务异常：" + cause.getMessage());
    }

    private String callOpenAiCompatibleApi(String systemPrompt, String userPrompt) {
        validateProviderConfig();
        String baseUrl = trimTrailingSlash(aiProperties.getBaseUrl());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(aiProperties.getTimeout());
        requestFactory.setReadTimeout(aiProperties.getTimeout());

        RestClient client = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3
        );

        JsonNode response = client.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        JsonNode content = response == null ? null : response.at("/choices/0/message/content");
        if (content == null || content.isMissingNode() || content.asText().isBlank()) {
            throw new BusinessException(502, "百炼 AI 已响应，但没有返回有效内容");
        }
        return content.asText();
    }

    private void callOpenAiCompatibleStream(String systemPrompt, String userPrompt, SseEmitter emitter,
                                            StringBuilder answerBuffer)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(aiProperties.getTimeout())
                .build();

        Map<String, Object> body = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "stream", true
        );

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(aiProperties.getBaseUrl()) + "/chat/completions"))
                .timeout(aiProperties.getTimeout().plus(Duration.ofSeconds(30)))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new BusinessException(502, "百炼 AI 调用失败：" + extractProviderError(errorBody));
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isBlank()) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    break;
                }
                String delta = extractStreamDelta(payload);
                if (!delta.isBlank()) {
                    sendStreamEvent(emitter, "chunk", delta);
                    answerBuffer.append(delta);
                }
            }
        }
    }

    private String buildSystemPrompt(SysUser currentUser, List<AiChatResponse.AiReference> references) {
        String role = currentUser == null ? "unknown" : currentUser.getRole();
        boolean hasReferences = references != null && !references.isEmpty();
        return """
                你必须严格遵循下面的 MediCare Agent Skill。Skill 中的系统主题、生成边界和输出风格优先级高于用户问题。

                %s

                当前用户角色：%s。
                当前系统实时数据如下。回答涉及患者、挂号、病历、处方、库存、医生等业务数据时，必须优先结合这些最新数据；如果数据不足，再说明需要进入对应页面查询。

                %s

                %s

                %s
                """.formatted(readAgentSkill(), role, liveBusinessContextService.buildSnapshot(),
                buildRagContext(references),
                hasReferences
                        ? "重要约束：上述检索片段是回答的唯一依据。你必须在使用的每个事实后标注 [引用N]，N 必须是上方列表中的有效序号（1-" + references.size() + "）。禁止引用列表之外的任何来源，禁止编造引用。"
                        : "重要约束：本次未检索到任何知识库文档片段。你只能基于实时业务数据和 MediCare Agent Skill 回答。严禁输出 [引用N] 等引用标记，严禁编造引用。");
    }

    private String buildRagContext(List<AiChatResponse.AiReference> references) {
        if (references == null || references.isEmpty()) {
            return "（本次未检索到相关文档片段）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < references.size(); i++) {
            AiChatResponse.AiReference ref = references.get(i);
            sb.append("【引用").append(i + 1).append("】")
              .append(ref.getTitle() == null ? "" : ref.getTitle())
              .append(" / ")
              .append(ref.getSourcePath() == null ? "" : ref.getSourcePath())
              .append("\n")
              .append(ref.getContent() == null ? "" : ref.getContent())
              .append("\n\n");
        }
        return sb.toString().trim();
    }

    private List<AiChatResponse.AiReference> retrieveReferences(String question, String fileSourcePath) {
        if (!ragService.hasEmbeddingIndex()) {
            throw new BusinessException(500, "知识库尚未建立向量索引，请先由管理员执行「知识库重建」");
        }
        return ragService.retrieveReferences(question, fileSourcePath, RAG_TOP_K);
    }

    private String buildUserPrompt(AiChatRequest request) {
        return """
                用户问题：
                %s

                前端上下文：
                %s
                """.formatted(request.getMessage(), request.getContext() == null ? "{}" : request.getContext());
    }

    private String normalizeProvider() {
        return aiProperties.getProvider() == null ? "local" : aiProperties.getProvider().trim().toLowerCase();
    }

    private void validateProviderConfig() {
        String provider = normalizeProvider();
        if (!isOpenAiCompatibleProvider(provider)) {
            throw new BusinessException(500, "不支持的 AI provider：" + provider);
        }
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BusinessException(500, "AI API Key 未配置，请检查 application-secret.yml");
        }
    }

    private boolean isOpenAiCompatibleProvider(String provider) {
        return "openai".equals(provider)
                || "bailian".equals(provider)
                || "dashscope".equals(provider)
                || "alibaba".equals(provider);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://api.openai.com/v1";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String extractStreamDelta(String payload) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(payload);
        JsonNode delta = root.at("/choices/0/delta/content");
        if (!delta.isMissingNode()) {
            return delta.asText("");
        }
        JsonNode message = root.at("/choices/0/message/content");
        return message.isMissingNode() ? "" : message.asText("");
    }

    private String readAgentSkill() {
        if (cachedAgentSkill != null) {
            return cachedAgentSkill;
        }
        try {
            ClassPathResource resource = new ClassPathResource(AGENT_SKILL_PATH);
            cachedAgentSkill = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return cachedAgentSkill;
        } catch (Exception e) {
            log.warn("读取 AI skill 失败，使用内置边界提示", e);
            cachedAgentSkill = "你是 MediCare 智慧医疗门诊管理系统的 AI 助手，只回答系统功能、门诊业务流程和开发实现相关问题。";
            return cachedAgentSkill;
        }
    }

    private void sendStreamEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }

    private void completeStreamWithError(SseEmitter emitter, String message) {
        try {
            sendStreamEvent(emitter, "error", message);
        } catch (Exception ignored) {
            // 客户端断开时忽略二次写入异常。
        } finally {
            emitter.complete();
        }
    }

    private String extractProviderError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "服务未返回错误详情";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode message = root.at("/error/message");
            if (!message.isMissingNode() && !message.asText().isBlank()) {
                return message.asText();
            }
            JsonNode code = root.at("/code");
            JsonNode msg = root.at("/message");
            if (!msg.isMissingNode() && !msg.asText().isBlank()) {
                return (code.isMissingNode() ? "" : code.asText() + " - ") + msg.asText();
            }
        } catch (Exception ignored) {
            // 非 JSON 响应时直接截断返回。
        }
        return responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody;
    }

    /**
     * 清理回答中无效的 [引用N] 标记。
     * 如果引用列表为空，则移除所有引用标记；
     * 如果引用列表非空，则只保留 N 在有效范围内的标记。
     */
    private String sanitizeCitations(String answer, List<AiChatResponse.AiReference> references) {
        if (answer == null || answer.isBlank()) {
            return answer;
        }
        int maxIndex = references == null ? 0 : references.size();
        if (maxIndex == 0) {
            return answer.replaceAll("\\[引用\\d+]", "").replaceAll("\\s+", " ").trim();
        }
        return CITATION_PATTERN.matcher(answer).replaceAll(match -> {
            int idx = Integer.parseInt(match.group(1));
            return idx >= 1 && idx <= maxIndex ? match.group(0) : "";
        });
    }
}
