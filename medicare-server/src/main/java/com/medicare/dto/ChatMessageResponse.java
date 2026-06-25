package com.medicare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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

    @Builder.Default
    @Schema(description = "AI 回复引用来源")
    private List<AiChatResponse.AiReference> references = Collections.emptyList();

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
