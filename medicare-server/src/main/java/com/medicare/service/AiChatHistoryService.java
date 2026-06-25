package com.medicare.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicare.dto.AiChatResponse;
import com.medicare.dto.ChatMessageResponse;
import com.medicare.dto.ChatSessionResponse;
import com.medicare.entity.AiChatMessage;
import com.medicare.entity.AiChatSession;
import com.medicare.exception.BusinessException;
import com.medicare.repository.AiChatMessageRepository;
import com.medicare.repository.AiChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 对话历史服务：会话管理、消息持久化、历史查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatHistoryService {

    private static final int TITLE_MAX_LENGTH = 50;
    private static final TypeReference<List<AiChatResponse.AiReference>> REFERENCE_LIST_TYPE = new TypeReference<>() {
    };
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[引用(\\d+)]");

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final RagService ragService;

    /**
     * 确保会话存在。如果 sessionKey 为空则新建；如果已存在则复用并可选更新标题。
     *
     * @return 会话实体
     */
    @Transactional
    public AiChatSession ensureSession(Long userId, String sessionKey, String titleHint) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录，无法保存对话历史");
        }

        String key = StringUtils.hasText(sessionKey) ? sessionKey.trim() : generateSessionKey();
        Optional<AiChatSession> optional = sessionRepository.findByUserIdAndSessionKey(userId, key);

        if (optional.isPresent()) {
            AiChatSession session = optional.get();
            if (!StringUtils.hasText(session.getTitle()) && StringUtils.hasText(titleHint)) {
                session.setTitle(truncateTitle(titleHint));
                sessionRepository.save(session);
            }
            return session;
        }

        AiChatSession session = new AiChatSession();
        session.setSessionKey(key);
        session.setUserId(userId);
        session.setTitle(StringUtils.hasText(titleHint) ? truncateTitle(titleHint) : "新会话");
        return sessionRepository.save(session);
    }

    /**
     * 保存一次完整对话（用户问题 + AI 回复），并返回实际会话标识。
     */
    @Transactional
    public String saveExchange(Long userId, String sessionKey, String userMessage, String assistantMessage,
                               List<AiChatResponse.AiReference> references) {
        AiChatSession session = ensureSession(userId, sessionKey, userMessage);
        saveMessage(session.getId(), "user", userMessage);
        saveMessage(session.getId(), "assistant", assistantMessage, references);
        sessionRepository.touchUpdateTime(session.getId());
        return session.getSessionKey();
    }

    /**
     * 保存单条消息。
     */
    @Transactional
    public void saveMessage(Long sessionId, String role, String content) {
        saveMessage(sessionId, role, content, List.of());
    }

    /**
     * 保存单条消息及引用来源。
     */
    @Transactional
    public void saveMessage(Long sessionId, String role, String content, List<AiChatResponse.AiReference> references) {
        if (sessionId == null || !StringUtils.hasText(content)) {
            return;
        }
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setReferencesJson(serializeReferences(references));
        messageRepository.save(message);
    }

    /**
     * 更新会话活动时间。
     */
    @Transactional
    public void touchSession(Long sessionId) {
        if (sessionId != null) {
            sessionRepository.touchUpdateTime(sessionId);
        }
    }

    /**
     * 查询当前用户的会话列表（按更新时间倒序）。
     */
    public List<ChatSessionResponse> listSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByUpdateTimeDesc(userId)
                .stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询指定会话的历史消息（校验归属用户）。
     */
    public List<ChatMessageResponse> listMessages(Long sessionId, Long userId) {
        if (sessionId == null || userId == null) {
            throw new BusinessException(400, "参数错误");
        }
        if (!sessionRepository.existsByIdAndUserId(sessionId, userId)) {
            throw new BusinessException(403, "无权访问该会话或会话不存在");
        }
        List<AiChatMessage> savedMessages = messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
        List<ChatMessageResponse> responses = new ArrayList<>(savedMessages.size());
        String lastUserQuestion = null;

        for (AiChatMessage message : savedMessages) {
            List<AiChatResponse.AiReference> references = deserializeReferences(message.getReferencesJson());
            if ("assistant".equals(message.getRole()) && references.isEmpty() && StringUtils.hasText(lastUserQuestion)) {
                references = recoverReferencesForLegacyMessage(lastUserQuestion, message.getContent());
            }
            responses.add(toMessageResponse(message, references));
            if ("user".equals(message.getRole())) {
                lastUserQuestion = message.getContent();
            }
        }

        return responses;
    }

    /**
     * 删除会话及其消息（校验归属用户）。
     */
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        if (sessionId == null || userId == null) {
            throw new BusinessException(400, "参数错误");
        }
        if (!sessionRepository.existsByIdAndUserId(sessionId, userId)) {
            throw new BusinessException(403, "无权删除该会话或会话不存在");
        }
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    private String generateSessionKey() {
        return "chat-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String truncateTitle(String title) {
        if (title == null) {
            return "新会话";
        }
        String t = title.replace("\n", " ").trim();
        return t.length() > TITLE_MAX_LENGTH ? t.substring(0, TITLE_MAX_LENGTH) + "..." : t;
    }

    private ChatSessionResponse toSessionResponse(AiChatSession session) {
        return ChatSessionResponse.builder()
                .id(session.getId())
                .sessionKey(session.getSessionKey())
                .title(session.getTitle())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    private ChatMessageResponse toMessageResponse(AiChatMessage message, List<AiChatResponse.AiReference> references) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .references(references)
                .createTime(message.getCreateTime())
                .build();
    }

    private String serializeReferences(List<AiChatResponse.AiReference> references) {
        if (references == null || references.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(references);
        } catch (Exception e) {
            log.warn("AI 引用序列化失败：{}", e.getMessage());
            return null;
        }
    }

    private List<AiChatResponse.AiReference> deserializeReferences(String referencesJson) {
        if (!StringUtils.hasText(referencesJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(referencesJson, REFERENCE_LIST_TYPE);
        } catch (Exception e) {
            log.warn("AI 引用反序列化失败：{}", e.getMessage());
            return List.of();
        }
    }

    private List<AiChatResponse.AiReference> recoverReferencesForLegacyMessage(String question, String answer) {
        int maxCitationIndex = getMaxCitationIndex(answer);
        if (maxCitationIndex == 0) {
            return List.of();
        }
        try {
            return ragService.retrieveReferences(question, null, Math.max(5, maxCitationIndex));
        } catch (Exception e) {
            log.warn("旧 AI 消息引用恢复失败：{}", e.getMessage());
            return List.of();
        }
    }

    private int getMaxCitationIndex(String answer) {
        if (!StringUtils.hasText(answer)) {
            return 0;
        }
        Matcher matcher = CITATION_PATTERN.matcher(answer);
        int maxIndex = 0;
        while (matcher.find()) {
            maxIndex = Math.max(maxIndex, Integer.parseInt(matcher.group(1)));
        }
        return maxIndex;
    }
}
