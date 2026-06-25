package com.medicare.service;

import com.medicare.dto.VectorRecord;

import java.util.List;

/**
 * 向量数据库存储抽象。
 */
public interface VectorStoreService {

    /**
     * 初始化集合（不存在则创建）。
     */
    void initializeCollection();

    /**
     * 清空集合中所有向量数据。
     */
    void clearCollection();

    /**
     * 批量写入或更新向量记录。
     */
    void upsertVectors(List<VectorRecord> records);

    /**
     * 根据查询向量检索最相似的记录 ID（vectorId）。
     *
     * @param embedding 查询向量
     * @param sourcePath 可选的 sourcePath 过滤条件，null 表示不过滤
     * @param topK 返回数量上限
     * @return 按相似度排序的 vectorId 列表
     */
    List<String> search(List<Double> embedding, String sourcePath, int topK);

    /**
     * 删除指定 vectorId 的向量记录。
     */
    void deleteVectors(List<String> vectorIds);

    /**
     * 根据 sourcePath payload 删除向量记录。
     */
    void deleteVectorsBySourcePaths(List<String> sourcePaths);

    /**
     * 向量存储是否可用。
     */
    boolean isAvailable();
}
