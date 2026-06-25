package com.medicare.dto;

import java.util.List;

/**
 * 向量数据库写入记录。
 */
public record VectorRecord(
        String vectorId,
        List<Double> embedding,
        String documentId,
        String sourcePath,
        String title,
        Integer chunkIndex
) {
}
