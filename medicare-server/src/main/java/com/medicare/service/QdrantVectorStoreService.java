package com.medicare.service;

import com.google.common.util.concurrent.ListenableFuture;
import com.medicare.config.AiProperties;
import com.medicare.dto.VectorRecord;
import com.medicare.exception.BusinessException;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * 基于 Qdrant 的向量存储实现。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "ai.vector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QdrantVectorStoreService implements VectorStoreService {

    private final AiProperties aiProperties;
    private QdrantClient client;

    public QdrantVectorStoreService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @PostConstruct
    public void init() {
        AiProperties.VectorProperties vector = aiProperties.getVector();
        if (!vector.isEnabled()) {
            return;
        }
        this.client = new QdrantClient(
                QdrantGrpcClient.newBuilder(vector.getHost(), vector.getPort(), vector.isUseTls()).build()
        );
        try {
            initializeCollection();
            log.info("Qdrant 向量存储已连接：{}:{}/{}，维度={}，距离={}",
                    vector.getHost(), vector.getPort(), vector.getCollection(),
                    vector.getDimension(), vector.getDistance());
        } catch (Exception e) {
            log.error("Qdrant 初始化失败: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public void initializeCollection() {
        ensureClient();
        AiProperties.VectorProperties vector = aiProperties.getVector();
        String collection = vector.getCollection();
        try {
            Boolean exists = wait(client.collectionExistsAsync(collection));
            if (Boolean.TRUE.equals(exists)) {
                validateExistingCollection(collection, vector);
                return;
            }
        } catch (Exception e) {
            log.warn("检查 Qdrant collection 是否存在失败，尝试直接创建: {}", e.getMessage());
        }

        createCollection(collection, vector);
    }

    private void validateExistingCollection(String collection, AiProperties.VectorProperties vector) {
        Collections.CollectionInfo info = wait(client.getCollectionInfoAsync(collection));
        Long existingDimension = resolveVectorDimension(info);
        if (existingDimension == null || existingDimension == vector.getDimension()) {
            return;
        }
        if (info.getPointsCount() == 0) {
            log.warn("Qdrant collection 维度不匹配且为空，将重建 collection: collection={}, existing={}, expected={}",
                    collection, existingDimension, vector.getDimension());
            wait(client.deleteCollectionAsync(collection));
            createCollection(collection, vector);
            return;
        }
        throw new BusinessException(500, "Qdrant collection 向量维度不匹配：当前 "
                + existingDimension + "，配置 " + vector.getDimension() + "。请先清空或迁移向量库后重启服务。");
    }

    private Long resolveVectorDimension(Collections.CollectionInfo info) {
        if (info == null || !info.hasConfig() || !info.getConfig().hasParams()) {
            return null;
        }
        Collections.CollectionParams params = info.getConfig().getParams();
        if (!params.hasVectorsConfig()) {
            return null;
        }
        Collections.VectorsConfig vectorsConfig = params.getVectorsConfig();
        if (!vectorsConfig.hasParams()) {
            return null;
        }
        return vectorsConfig.getParams().getSize();
    }

    private void createCollection(String collection, AiProperties.VectorProperties vector) {
        Collections.CreateCollection request = Collections.CreateCollection.newBuilder()
                .setCollectionName(collection)
                .setVectorsConfig(Collections.VectorsConfig.newBuilder()
                        .setParams(Collections.VectorParams.newBuilder()
                                .setSize(vector.getDimension())
                                .setDistance(parseDistance(vector.getDistance()))
                                .build())
                        .build())
                .build();
        wait(client.createCollectionAsync(request));
    }

    @Override
    public void clearCollection() {
        if (client == null) {
            log.warn("Qdrant 客户端未初始化，跳过向量数据库清空");
            return;
        }
        String collection = aiProperties.getVector().getCollection();
        try {
            wait(client.deleteCollectionAsync(collection));
        } catch (Exception e) {
            log.warn("删除 Qdrant collection 失败（可能不存在）: {}", e.getMessage());
        }
        try {
            initializeCollection();
        } catch (Exception e) {
            log.warn("重新创建 Qdrant collection 失败: {}", e.getMessage());
        }
    }

    @Override
    public void upsertVectors(List<VectorRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        ensureClient();
        String collection = aiProperties.getVector().getCollection();
        List<Points.PointStruct> points = records.stream()
                .map(this::toPointStruct)
                .collect(Collectors.toList());
        Points.UpsertPoints upsert = Points.UpsertPoints.newBuilder()
                .setCollectionName(collection)
                .addAllPoints(points)
                .setWait(true)
                .build();
        wait(client.upsertAsync(upsert));
    }

    @Override
    public List<String> search(List<Double> embedding, String sourcePath, int topK) {
        ensureClient();
        String collection = aiProperties.getVector().getCollection();
        Points.SearchPoints.Builder builder = Points.SearchPoints.newBuilder()
                .setCollectionName(collection)
                .addAllVector(toFloats(embedding))
                .setLimit(topK)
                .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build());
        if (sourcePath != null && !sourcePath.isBlank()) {
            builder.setFilter(Points.Filter.newBuilder()
                    .addMust(Points.Condition.newBuilder()
                            .setField(Points.FieldCondition.newBuilder()
                                    .setKey("sourcePath")
                                    .setMatch(Points.Match.newBuilder().setKeyword(sourcePath).build())
                                    .build())
                            .build())
                    .build());
        }
        List<Points.ScoredPoint> results = wait(client.searchAsync(builder.build()));
        return results.stream()
                .map(p -> p.getId().getUuid())
                .collect(Collectors.toList());
    }

    @Override
    public void deleteVectors(List<String> vectorIds) {
        if (vectorIds == null || vectorIds.isEmpty()) {
            return;
        }
        ensureClient();
        String collection = aiProperties.getVector().getCollection();
        List<Points.PointId> ids = vectorIds.stream()
                .map(UUID::fromString)
                .map(id -> id(id))
                .collect(Collectors.toList());
        wait(client.deleteAsync(collection, ids));
    }

    @Override
    public void deleteVectorsBySourcePaths(List<String> sourcePaths) {
        if (sourcePaths == null || sourcePaths.isEmpty()) {
            return;
        }
        ensureClient();
        String collection = aiProperties.getVector().getCollection();
        List<String> distinctSourcePaths = sourcePaths.stream()
                .filter(path -> path != null && !path.isBlank())
                .distinct()
                .toList();
        if (distinctSourcePaths.isEmpty()) {
            return;
        }

        Points.Filter.Builder filter = Points.Filter.newBuilder();
        for (String sourcePath : distinctSourcePaths) {
            filter.addShould(Points.Condition.newBuilder()
                    .setField(Points.FieldCondition.newBuilder()
                            .setKey("sourcePath")
                            .setMatch(Points.Match.newBuilder().setKeyword(sourcePath).build())
                            .build())
                    .build());
        }
        wait(client.deleteAsync(collection, filter.build()));
    }

    @Override
    public boolean isAvailable() {
        if (client == null) {
            return false;
        }
        try {
            wait(client.listCollectionsAsync());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Points.PointStruct toPointStruct(VectorRecord record) {
        return Points.PointStruct.newBuilder()
                .setId(id(UUID.fromString(record.vectorId())))
                .setVectors(vectors(toFloats(record.embedding())))
                .putPayload("documentId", value(record.documentId()))
                .putPayload("sourcePath", value(record.sourcePath()))
                .putPayload("title", value(record.title()))
                .putPayload("chunkIndex", value(record.chunkIndex().longValue()))
                .build();
    }

    private List<Float> toFloats(List<Double> values) {
        List<Float> floats = new ArrayList<>(values.size());
        for (Double value : values) {
            floats.add(value.floatValue());
        }
        return floats;
    }

    private Collections.Distance parseDistance(String distance) {
        return switch (distance == null ? "" : distance.toLowerCase()) {
            case "euclid", "euclidean" -> Collections.Distance.Euclid;
            case "dot", "dotproduct" -> Collections.Distance.Dot;
            default -> Collections.Distance.Cosine;
        };
    }

    private void ensureClient() {
        if (client == null) {
            throw new BusinessException(500, "Qdrant 向量数据库未启用或未初始化");
        }
    }

    private <T> T wait(ListenableFuture<T> future) {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "Qdrant 操作被中断: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(500, "Qdrant 操作失败: " + e.getMessage());
        }
    }
}
