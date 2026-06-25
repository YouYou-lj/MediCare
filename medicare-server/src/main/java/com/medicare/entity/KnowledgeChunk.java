package com.medicare.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, length = 500)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 1000)
    private String keywords;

    @Column(name = "vector_id", length = 64, unique = true)
    private String vectorId;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "source_path", nullable = false, length = 500)
    private String sourcePath;

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;
}
