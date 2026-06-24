package com.medicare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "AI 对话会话信息")
public class ChatSessionResponse {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话标识")
    private String sessionKey;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
