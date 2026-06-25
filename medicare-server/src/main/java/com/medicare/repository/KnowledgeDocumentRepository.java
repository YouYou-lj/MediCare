package com.medicare.repository;

import com.medicare.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    Optional<KnowledgeDocument> findBySourcePath(String sourcePath);

    List<KnowledgeDocument> findTop20ByOrderByUpdateTimeDesc();

    List<KnowledgeDocument> findBySourceType(String sourceType);

    List<KnowledgeDocument> findByStatus(Integer status);

    List<KnowledgeDocument> findBySourcePathStartingWith(String prefix);

    @Query("SELECT d.sourcePath FROM KnowledgeDocument d WHERE d.sourcePath NOT LIKE 'uploads/%' AND d.sourcePath NOT LIKE 'assistant-uploads/%'")
    List<String> findAllSystemSourcePaths();

    @Modifying
    @Query("DELETE FROM KnowledgeDocument d WHERE d.sourcePath NOT LIKE 'uploads/%' AND d.sourcePath NOT LIKE 'assistant-uploads/%'")
    int deleteAllSystemDocuments();
}
