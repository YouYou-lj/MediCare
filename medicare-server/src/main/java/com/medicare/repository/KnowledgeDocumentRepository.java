package com.medicare.repository;

import com.medicare.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    Optional<KnowledgeDocument> findBySourcePath(String sourcePath);

    List<KnowledgeDocument> findTop20ByOrderByUpdateTimeDesc();

    List<KnowledgeDocument> findBySourceType(String sourceType);

    List<KnowledgeDocument> findByStatus(Integer status);
}
