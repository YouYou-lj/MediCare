package com.medicare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "RAG 文档重建结果")
public class RagReindexResponse {
    private int documentCount;
    private int chunkCount;
    private String message;
}
