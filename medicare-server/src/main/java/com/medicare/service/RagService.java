package com.medicare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicare.config.AiProperties;
import com.medicare.dto.AiChatResponse;
import com.medicare.dto.KnowledgeDocumentContentResponse;
import org.springframework.util.StringUtils;
import com.medicare.dto.KnowledgeDocumentResponse;
import com.medicare.dto.KnowledgeUploadResponse;
import com.medicare.dto.RagQueryRequest;
import com.medicare.dto.RagReindexResponse;
import com.medicare.entity.KnowledgeChunk;
import com.medicare.entity.KnowledgeDocument;
import com.medicare.exception.BusinessException;
import com.medicare.repository.KnowledgeChunkRepository;
import com.medicare.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private static final int CHUNK_SIZE = 1800;
    private static final int CHUNK_OVERLAP = 240;
    private static final String INDEX_VERSION = "rag-v3-bailian-embedding-v4";

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final AiProperties aiProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final DocumentTextExtractionService documentTextExtractionService;
    private final LiveBusinessContextService liveBusinessContextService;

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> listDocuments() {
        return documentRepository.findTop20ByOrderByUpdateTimeDesc().stream()
                .map(document -> KnowledgeDocumentResponse.builder()
                        .id(document.getId())
                        .filename(document.getTitle())
                        .sourcePath(document.getSourcePath())
                        .sourceType(document.getSourceType())
                        .chunkCount(document.getChunkCount())
                        .status(document.getStatus())
                        .isSystem(!document.getSourcePath().startsWith("uploads/"))
                        .createTime(document.getCreateTime())
                        .updateTime(document.getUpdateTime())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> listAllDocuments() {
        return documentRepository.findByStatus(1).stream()
                .map(document -> KnowledgeDocumentResponse.builder()
                        .id(document.getId())
                        .filename(document.getTitle())
                        .sourcePath(document.getSourcePath())
                        .sourceType(document.getSourceType())
                        .chunkCount(document.getChunkCount())
                        .status(document.getStatus())
                        .isSystem(!document.getSourcePath().startsWith("uploads/"))
                        .createTime(document.getCreateTime())
                        .updateTime(document.getUpdateTime())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentContentResponse getDocument(Long id) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
        String content = buildDocumentContent(document.getId());
        boolean isSystem = !document.getSourcePath().startsWith("uploads/");
        return KnowledgeDocumentContentResponse.builder()
                .id(document.getId())
                .filename(document.getTitle())
                .sourcePath(document.getSourcePath())
                .sourceType(document.getSourceType())
                .content(content)
                .chunkCount(document.getChunkCount())
                .status(document.getStatus())
                .isSystem(isSystem)
                .createTime(document.getCreateTime())
                .updateTime(document.getUpdateTime())
                .build();
    }

    @Transactional
    public void deleteDocument(Long id) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
        chunkRepository.deleteByDocumentId(document.getId());
        documentRepository.delete(document);
        log.info("知识库文档已删除: id={}, title={}", id, document.getTitle());
    }

    @Transactional
    public KnowledgeDocumentResponse updateDocument(Long id, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "文档内容不能为空");
        }
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
        String sourcePath = document.getSourcePath();
        String sourceType = document.getSourceType();
        String title = document.getTitle();
        boolean isSystem = !sourcePath.startsWith("uploads/");
        try {
            IndexResult result = indexContent(title, sourcePath, sourceType, content, true);
            return KnowledgeDocumentResponse.builder()
                    .id(document.getId())
                    .filename(title)
                    .sourcePath(sourcePath)
                    .sourceType(sourceType)
                    .chunkCount(result.chunkCount())
                    .status(1)
                    .updateTime(document.getUpdateTime())
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "文档更新失败：" + e.getMessage());
        }
    }

    private String buildDocumentContent(Long documentId) {
        List<KnowledgeChunk> chunks = chunkRepository.findByDocumentId(documentId);
        if (chunks.isEmpty()) {
            return "";
        }
        chunks.sort(Comparator.comparingInt(KnowledgeChunk::getChunkIndex));
        StringBuilder sb = new StringBuilder();
        for (KnowledgeChunk chunk : chunks) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(chunk.getContent());
        }
        return sb.toString();
    }

    @Transactional
    public RagReindexResponse reindex(Path projectRoot) {
        List<Path> files = collectKnowledgeFiles(projectRoot);
        int documents = 0;
        int chunks = 0;

        for (Path file : files) {
            try {
                String content = normalizeContent(file, Files.readString(file, StandardCharsets.UTF_8));
                if (content.isBlank()) {
                    continue;
                }
                String sourcePath = projectRoot.relativize(file).toString();
                IndexResult result = indexContent(file.getFileName().toString(), sourcePath, resolveSourceType(file), content, false);
                if (!result.updated()) {
                    documents++;
                    chunks += result.chunkCount();
                    continue;
                }
                documents++;
                chunks += result.chunkCount();
            } catch (Exception e) {
                log.warn("知识库文档处理失败: {}", file, e);
            }
        }

        return RagReindexResponse.builder()
                .documentCount(documents)
                .chunkCount(chunks)
                .message("知识库重建完成")
                .build();
    }

    @Transactional
    public KnowledgeUploadResponse uploadDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的知识库文档");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("document").trim();
        if (filename.isBlank()) {
            filename = "document";
        }
        String content = normalizeUploadedContent(documentTextExtractionService.extractText(file));
        if (content.isBlank()) {
            throw new BusinessException(400, "文档中未解析到可用于知识库的文本内容");
        }
        String sourceType = documentTextExtractionService.resolveSourceType(filename);
        String sourcePath = "uploads/" + System.currentTimeMillis() + "-" + sanitizeFilename(filename);
        try {
            IndexResult result = indexContent(filename, sourcePath, sourceType, content, true);
            return KnowledgeUploadResponse.builder()
                    .filename(filename)
                    .sourcePath(sourcePath)
                    .sourceType(sourceType)
                    .chunkCount(result.chunkCount())
                    .message("文档上传并写入知识库成功")
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "文档写入知识库失败：" + e.getMessage());
        }
    }

    public AiChatResponse query(RagQueryRequest request) {
        int topK = request.getTopK() == null ? 5 : Math.max(1, Math.min(request.getTopK(), 8));
        List<KnowledgeChunk> chunks = retrieve(request.getQuestion(), topK);
        if (chunks.isEmpty()) {
            return AiChatResponse.builder()
                    .answer("知识库中暂未检索到相关向量数据。请先由管理员执行知识库重建，或换一个更具体的问题。")
                    .provider("rag")
                    .model(aiProperties.getEmbeddingModel())
                    .references(List.of())
                    .build();
        }

        String prompt = buildRagPrompt(request.getQuestion(), chunks);
        String answer = callModel(prompt);
        return AiChatResponse.builder()
                .answer(answer)
                .provider(normalizeProvider())
                .model(aiProperties.getModel())
                .references(toReferences(chunks))
                .build();
    }

    /**
     * 判断当前配置的 embedding 模型是否已有向量索引数据。
     */
    @Transactional(readOnly = true)
    public boolean hasEmbeddingIndex() {
        return chunkRepository.countByEmbeddingModelAndEmbeddingIsNotNull(aiProperties.getEmbeddingModel()) > 0;
    }

    /**
     * 仅检索知识库，返回引用列表（供 AI 助手对话前置检索使用）。
     */
    @Transactional(readOnly = true)
    public List<AiChatResponse.AiReference> retrieveReferences(String question, int topK) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        int limit = Math.max(1, Math.min(topK, 8));
        return toReferences(retrieve(question, limit));
    }

    private List<AiChatResponse.AiReference> toReferences(List<KnowledgeChunk> chunks) {
        return chunks.stream()
                .map(chunk -> AiChatResponse.AiReference.builder()
                        .type("document")
                        .id(String.valueOf(chunk.getDocumentId()))
                        .title(chunk.getTitle() + " #" + chunk.getChunkIndex())
                        .sourcePath(chunk.getSourcePath())
                        .content(abbreviate(chunk.getContent(), 600))
                        .build())
                .toList();
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLen) + "…";
    }

    private List<Path> collectKnowledgeFiles(Path projectRoot) {
        List<Path> roots = List.of(
                projectRoot.resolve("DOC"),
                projectRoot.resolve("docs"),
                projectRoot.resolve("medicare-server/src/main/java/com/medicare/controller"),
                projectRoot.resolve("medicare-server/src/main/java/com/medicare/dto"),
                projectRoot.resolve("medicare-server/src/main/java/com/medicare/entity"),
                projectRoot.resolve("medicare-web/src/api"),
                projectRoot.resolve("medicare-web/src/types"),
                projectRoot.resolve("medicare-web/src/router")
        );
        List<Path> files = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) continue;
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(this::isSupportedDocument)
                        .forEach(files::add);
            } catch (IOException e) {
                log.warn("扫描知识库目录失败: {}", root, e);
            }
        }
        List.of("plan.md", "step.md", "push-requirements.md", "skill.md").forEach(name -> {
            Path file = projectRoot.resolve(name);
            if (Files.exists(file)) {
                files.add(file);
            }
        });
        return files.stream().distinct().sorted().toList();
    }

    private boolean isSupportedDocument(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".md")
                || name.endsWith(".html")
                || name.endsWith(".txt")
                || name.endsWith(".java")
                || name.endsWith(".ts")
                || name.endsWith(".vue")
                || name.endsWith(".yml")
                || name.endsWith(".yaml")
                || name.endsWith(".json");
    }

    private String normalizeContent(Path file, String raw) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        String text = raw;
        if (name.endsWith(".html")) {
            text = text.replaceAll("(?is)<script.*?</script>", " ")
                    .replaceAll("(?is)<style.*?</style>", " ")
                    .replaceAll("(?s)<[^>]+>", " ");
        }
        return text.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll(" {2,}", " ")
                .trim();
    }

    private String normalizeUploadedContent(String raw) {
        return raw == null ? "" : raw
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll(" {2,}", " ")
                .trim();
    }

    private IndexResult indexContent(String title, String sourcePath, String sourceType, String content, boolean forceUpdate)
            throws Exception {
        String contentHash = sha256(INDEX_VERSION + "\n" + content);
        KnowledgeDocument document = documentRepository.findBySourcePath(sourcePath)
                .orElseGet(KnowledgeDocument::new);
        if (!forceUpdate && document.getId() != null && contentHash.equals(document.getContentHash())) {
            return new IndexResult(false, document.getChunkCount());
        }

        document.setTitle(title);
        document.setSourcePath(sourcePath);
        document.setSourceType(sourceType);
        document.setContentHash(contentHash);
        document.setStatus(1);
        document = documentRepository.save(document);
        chunkRepository.deleteByDocumentId(document.getId());

        List<String> pieces = splitIntoChunks(content);
        int index = 0;
        for (String piece : pieces) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(index++);
            chunk.setTitle(document.getTitle());
            chunk.setSourcePath(sourcePath);
            chunk.setContent(piece);
            chunk.setKeywords(extractKeywords(piece));
            chunk.setEmbeddingModel(aiProperties.getEmbeddingModel());
            chunk.setEmbedding(serializeEmbedding(createEmbedding(piece)));
            chunkRepository.save(chunk);
        }
        document.setChunkCount(pieces.size());
        documentRepository.save(document);
        return new IndexResult(true, pieces.size());
    }

    private record IndexResult(boolean updated, int chunkCount) {
    }

    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + CHUNK_SIZE);
            if (end < content.length()) {
                int newline = content.lastIndexOf('\n', end);
                if (newline > start + 300) {
                    end = newline;
                }
            }
            String chunk = content.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
    }

    private List<KnowledgeChunk> retrieve(String question, int topK) {
        List<Double> queryEmbedding = createEmbedding(question);
        List<KnowledgeChunk> embeddedChunks = chunkRepository.findByEmbeddingModelAndEmbeddingIsNotNull(aiProperties.getEmbeddingModel());
        if (!embeddedChunks.isEmpty()) {
            Set<String> terms = tokenize(question);
            return embeddedChunks.stream()
                    .map(chunk -> new ScoredChunk(chunk, cosineSimilarity(queryEmbedding, parseEmbedding(chunk.getEmbedding()))
                            + lexicalBoost(chunk, terms, question)))
                    .filter(scored -> scored.score() > 0)
                    .sorted((a, b) -> Double.compare(b.score(), a.score()))
                    .limit(topK)
                    .map(ScoredChunk::chunk)
                    .toList();
        }

        Set<String> terms = tokenize(question);
        Map<Long, KnowledgeChunk> chunkMap = new LinkedHashMap<>();
        Map<Long, Integer> scoreMap = new HashMap<>();
        for (String term : terms) {
            List<KnowledgeChunk> matches = chunkRepository.searchByKeyword(term, 20);
            for (KnowledgeChunk chunk : matches) {
                chunkMap.put(chunk.getId(), chunk);
                scoreMap.merge(chunk.getId(), scoreChunk(chunk, term, question), Integer::sum);
            }
        }
        return chunkMap.values().stream()
                .sorted((a, b) -> Integer.compare(scoreMap.getOrDefault(b.getId(), 0), scoreMap.getOrDefault(a.getId(), 0)))
                .limit(topK)
                .toList();
    }

    private record ScoredChunk(KnowledgeChunk chunk, double score) {
    }

    private double lexicalBoost(KnowledgeChunk chunk, Set<String> terms, String question) {
        int score = 0;
        for (String term : terms) {
            score += scoreChunk(chunk, term, question);
        }
        return Math.min(score, 60) / 1000.0;
    }

    private Set<String> tokenize(String question) {
        String normalized = question.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsHan}a-z0-9]+", " ");
        Set<String> terms = new LinkedHashSet<>();
        for (String part : normalized.split("\\s+")) {
            if (part.length() >= 2) {
                terms.add(part);
            }
            if (part.matches(".*\\p{IsHan}.*") && part.length() > 4) {
                for (int i = 0; i <= part.length() - 2; i++) {
                    terms.add(part.substring(i, i + 2));
                }
            }
        }
        if (question.contains("患者")) {
            terms.add("Patient");
            terms.add("patients");
        }
        if (question.contains("接口")) {
            terms.add("Controller");
            terms.add("RequestMapping");
            terms.add("GetMapping");
            terms.add("PostMapping");
            terms.add("PutMapping");
            terms.add("DeleteMapping");
        }
        return terms.isEmpty() ? Set.of(question.trim()) : terms;
    }

    private int scoreChunk(KnowledgeChunk chunk, String term, String question) {
        String haystack = (chunk.getTitle() + "\n" + chunk.getKeywords() + "\n" + chunk.getContent()).toLowerCase(Locale.ROOT);
        int score = 0;
        int index = haystack.indexOf(term.toLowerCase(Locale.ROOT));
        while (index >= 0) {
            score++;
            index = haystack.indexOf(term.toLowerCase(Locale.ROOT), index + term.length());
        }
        String sourcePath = Optional.ofNullable(chunk.getSourcePath()).orElse("").toLowerCase(Locale.ROOT);
        String title = Optional.ofNullable(chunk.getTitle()).orElse("").toLowerCase(Locale.ROOT);
        if (question.contains("接口") && (sourcePath.contains("/controller/") || sourcePath.contains("/src/api/"))) {
            score += 20;
        }
        if (question.contains("患者") && (title.contains("patient") || sourcePath.contains("patient"))) {
            score += 30;
        }
        return score;
    }

    private String buildRagPrompt(String question, List<KnowledgeChunk> chunks) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            context.append("【引用").append(i + 1).append("】")
                    .append(chunk.getTitle()).append(" / ").append(chunk.getSourcePath()).append("\n")
                    .append(chunk.getContent()).append("\n\n");
        }
        return """
                你是 MediCare 项目的文档知识库问答助手。
                只能根据下方检索到的文档片段回答问题；如果片段不足，请明确说明“当前知识库没有足够依据”。
                同时必须结合“实时业务数据快照”中的最新患者、挂号、病历、处方、库存等系统数据；当文档片段与实时数据冲突时，以实时业务数据为准。
                回答要简洁、可执行，并在关键结论后标注引用编号，例如 [引用1]。

                用户问题：
                %s

                实时业务数据快照：
                %s

                检索片段：
                %s
                """.formatted(question, liveBusinessContextService.buildSnapshot(), context);
    }

    private String callModel(String prompt) {
        validateProviderConfig();
        Map<String, Object> body = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是 MediCare 文档知识库 RAG 助手，必须基于引用回答。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        JsonNode response = createAiClient().post()
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

    private List<Double> createEmbedding(String input) {
        validateProviderConfig();
        Map<String, Object> body = Map.of(
                "model", aiProperties.getEmbeddingModel(),
                "input", input
        );

        JsonNode response = createAiClient().post()
                .uri("/embeddings")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        JsonNode embeddingNode = response == null ? null : response.at("/data/0/embedding");
        if (embeddingNode == null || !embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new BusinessException(502, "百炼 embedding 已响应，但没有返回有效向量");
        }
        List<Double> embedding = new ArrayList<>(embeddingNode.size());
        for (JsonNode value : embeddingNode) {
            embedding.add(value.asDouble());
        }
        return embedding;
    }

    private RestClient createAiClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(aiProperties.getTimeout());
        requestFactory.setReadTimeout(aiProperties.getTimeout());

        return restClientBuilder
                .baseUrl(trimTrailingSlash(aiProperties.getBaseUrl()))
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String serializeEmbedding(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new BusinessException(500, "向量序列化失败");
        }
    }

    private List<Double> parseEmbedding(String embedding) {
        if (embedding == null || embedding.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(embedding);
            if (!node.isArray()) {
                return List.of();
            }
            List<Double> values = new ArrayList<>(node.size());
            for (JsonNode value : node) {
                values.add(value.asDouble());
            }
            return values;
        } catch (Exception e) {
            log.warn("知识库向量解析失败，chunk embedding 将被跳过", e);
            return List.of();
        }
    }

    private double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left.isEmpty() || right.isEmpty() || left.size() != right.size()) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.size(); i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private void validateProviderConfig() {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BusinessException(500, "AI API Key 未配置，请检查 application-secret.yml");
        }
        if (aiProperties.getEmbeddingModel() == null || aiProperties.getEmbeddingModel().isBlank()) {
            throw new BusinessException(500, "AI embedding 模型未配置，请检查 ai.embedding-model");
        }
    }

    private String normalizeProvider() {
        return aiProperties.getProvider() == null ? "rag" : aiProperties.getProvider().trim().toLowerCase(Locale.ROOT);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://api.openai.com/v1";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String resolveSourceType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html")) return "html";
        if (name.endsWith(".md")) return "markdown";
        return "text";
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^\\p{IsHan}a-zA-Z0-9._-]+", "_");
    }

    private String extractKeywords(String text) {
        return String.join(" ", tokenize(text).stream().limit(40).toList());
    }

    private String sha256(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
