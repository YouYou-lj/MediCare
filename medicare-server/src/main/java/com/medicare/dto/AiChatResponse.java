package com.medicare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@Schema(description = "AI 助手对话响应")
public class AiChatResponse {

    @Schema(description = "AI 回复内容")
    private String answer;

    @Schema(description = "模型供应商")
    private String provider;

    @Schema(description = "模型名称")
    private String model;

    @Builder.Default
    @Schema(description = "引用来源，Step 7 暂为空，RAG 阶段补充")
    private List<AiReference> references = Collections.emptyList();

    @Builder.Default
    @Schema(description = "前端可执行动作，Step 7 暂为空，Agent 阶段补充")
    private List<AiAction> actions = Collections.emptyList();

    @Data
    @Builder
    @Schema(description = "AI 回复引用")
    public static class AiReference {
        private String type;
        private String id;
        private String title;
    }

    @Data
    @Builder
    @Schema(description = "AI 建议动作")
    public static class AiAction {
        private String label;
        private String type;
        private String target;
    }
}
