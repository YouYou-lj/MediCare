package com.medicare.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KnowledgeSystemUploadBatchResponse {

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private Integer totalChunkCount;

    private String message;

    private List<KnowledgeUploadResponse> files;
}
