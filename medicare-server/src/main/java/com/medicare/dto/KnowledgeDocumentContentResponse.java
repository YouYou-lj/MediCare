package com.medicare.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeDocumentContentResponse {

    private Long id;

    private String filename;

    private String sourcePath;

    private String sourceType;

    private String content;

    private Integer chunkCount;

    private Integer status;

    private Boolean isSystem;

    private Long uploadedBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
