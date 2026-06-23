package com.medicare.controller;

import com.medicare.auth.AuthInterceptor;
import com.medicare.dto.AiChatRequest;
import com.medicare.dto.AiChatResponse;
import com.medicare.dto.Result;
import com.medicare.entity.SysUser;
import com.medicare.exception.BusinessException;
import com.medicare.service.AiAssistantService;
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

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 助手", description = "AI 助手基础对话接口")
public class AiController {

    private final AiAssistantService aiAssistantService;

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
}
