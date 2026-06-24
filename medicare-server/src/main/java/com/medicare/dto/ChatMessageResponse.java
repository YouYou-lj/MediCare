package com.medicare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "AI 对话消息信息")
public class ChatMessageResponse {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "消息角色：user / assistant")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
