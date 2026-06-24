package com.medicare.repository;

import com.medicare.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    void deleteByDocumentId(Long documentId);

    @Query(value = """
            SELECT *
            FROM knowledge_chunk c
            WHERE c.content LIKE CONCAT('%', :keyword, '%')
               OR c.title LIKE CONCAT('%', :keyword, '%')
               OR c.keywords LIKE CONCAT('%', :keyword, '%')
            ORDER BY c.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeChunk> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    List<KnowledgeChunk> findByEmbeddingModelAndEmbeddingIsNotNull(String embeddingModel);

    long countByEmbeddingModelAndEmbeddingIsNotNull(String embeddingModel);

    List<KnowledgeChunk> findByDocumentId(Long documentId);

    List<KnowledgeChunk> findByIdIn(Collection<Long> ids);
}
