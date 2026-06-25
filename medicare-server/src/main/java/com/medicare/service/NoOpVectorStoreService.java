package com.medicare.service;

import com.medicare.dto.VectorRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量数据库禁用时使用的空实现（检索将回退到关键词匹配）。
 */
@Service
@ConditionalOnProperty(prefix = "ai.vector", name = "enabled", havingValue = "false")
public class NoOpVectorStoreService implements VectorStoreService {

    @Override
    public void initializeCollection() {
    }

    @Override
    public void clearCollection() {
    }

    @Override
    public void upsertVectors(List<VectorRecord> records) {
    }

    @Override
    public List<String> search(List<Double> embedding, String sourcePath, int topK) {
        return List.of();
    }

    @Override
    public void deleteVectors(List<String> vectorIds) {
    }

    @Override
    public void deleteVectorsBySourcePaths(List<String> sourcePaths) {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
