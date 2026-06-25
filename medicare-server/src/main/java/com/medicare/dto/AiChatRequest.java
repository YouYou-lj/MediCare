package com.medicare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "AI 助手对话请求")
public class AiChatRequest {

    @NotBlank(message = "请输入要咨询的问题")
    @Size(max = 2000, message = "问题内容不能超过2000个字符")
    @Schema(description = "用户输入的问题", example = "今天有哪些待办事项？")
    private String message;

    @Size(max = 64, message = "会话ID不能超过64个字符")
    @Schema(description = "前端生成的临时会话ID", example = "chat-1719123456789")
    private String sessionId;

    @Schema(description = "当前页面上下文，如 route、patientId、recordId 等")
    private Map<String, Object> context;

    @Schema(description = "用户上传并关联本轮对话的文件 sourcePath，RAG 检索将优先/限定该文件")
    private String fileSourcePath;
}
