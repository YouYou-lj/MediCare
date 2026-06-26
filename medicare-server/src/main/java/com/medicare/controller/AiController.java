package com.medicare.controller;

import com.medicare.auth.AuthInterceptor;
import com.medicare.dto.*;
import com.medicare.entity.SysUser;
import com.medicare.exception.BusinessException;
import com.medicare.service.AiAssistantService;
import com.medicare.service.AiChatHistoryService;
import com.medicare.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 助手", description = "AI 助手基础对话接口")
public class AiController {

    private final AiAssistantService aiAssistantService;
    private final RagService ragService;
    private final AiChatHistoryService aiChatHistoryService;

    @PostMapping("/chat")
    @Operation(summary = "AI 助手基础对话")
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request,
                                       HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        if (currentUser == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        return Result.ok(aiAssistantService.chat(request, currentUser));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 助手流式对话")
    public ResponseEntity<SseEmitter> streamChat(@Valid @RequestBody AiChatRequest request,
                                                 HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        if (currentUser == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noCache())
                .header("X-Accel-Buffering", "no")
                .body(aiAssistantService.streamChat(request, currentUser));
    }

    @PostMapping("/rag/reindex")
    @Operation(summary = "重建 RAG 文档知识库")
    public Result<RagReindexResponse> reindexRag(HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        if (currentUser == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        if (!isMainAdmin(currentUser)) {
            throw new BusinessException(403, "只有主管理员可以重建系统知识库");
        }
        return Result.ok(ragService.reindex(resolveProjectRoot(), currentUser.getId()));
    }

    private boolean isMainAdmin(SysUser user) {
        return user != null && user.getId() != null && user.getId() == 1L;
    }

    @PostMapping("/rag/query")
    @Operation(summary = "RAG 文档知识库问答")
    public Result<AiChatResponse> queryRag(@Valid @RequestBody RagQueryRequest request,
                                           HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        if (currentUser == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        return Result.ok(ragService.query(request));
    }

    @GetMapping("/chat/sessions")
    @Operation(summary = "获取当前用户 AI 会话列表")
    public Result<List<ChatSessionResponse>> listSessions(HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        if (currentUser == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        return Result.ok(aiChatHistoryService.listSessions(currentUser.getId()));
    }

    @GetMapping("/chat/sessions/{id}/messages")
    @Operation(summary = "获取指定会话的历史消息")
    public Result<List<ChatMessageResponse>> listMessages(@PathVariable("id") Long sessionId,
                                                          HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        if (currentUser == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        return Result.ok(aiChatHistoryService.listMessages(sessionId, currentUser.getId()));
    }

    @DeleteMapping("/chat/sessions/{id}")
    @Operation(summary = "删除指定 AI 会话")
    public Result<Void> deleteSession(@PathVariable("id") Long sessionId,
                                      HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        if (currentUser == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        aiChatHistoryService.deleteSession(sessionId, currentUser.getId());
        return Result.ok();
    }

    private Path resolveProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (current.getFileName() != null && "medicare-server".equals(current.getFileName().toString())) {
            Path parent = current.getParent();
            return parent == null ? current : parent;
        }
        if (current.resolve("medicare-server").toFile().exists()) {
            return current;
        }
        Path parent = current.getParent();
        return parent == null ? current : parent;
    }
}
