package com.medicare.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeUploadResponse {

    private String filename;

    private String sourcePath;

    private String sourceType;

    private Integer chunkCount;

    private String message;
}
