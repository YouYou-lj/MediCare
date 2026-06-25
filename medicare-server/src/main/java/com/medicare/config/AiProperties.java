package com.medicare.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 助手配置。
 * <p>
 * Step 7 只实现基础对话：通过 OpenAI 兼容接口调用阿里百炼等模型服务。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** provider=openai 时调用 OpenAI 兼容 Chat Completions 接口；local 表示本地兜底。 */
    private String provider = "local";

    private String apiKey = "";

    private String model = "gpt-4o-mini";

    private String embeddingModel = "text-embedding-v4";

    private String baseUrl = "https://api.openai.com/v1";

    private Duration timeout = Duration.ofSeconds(20);

    private ExecutorProperties executor = new ExecutorProperties();

    private VectorProperties vector = new VectorProperties();

    @Data
    public static class VectorProperties {

        /** 是否启用专用向量数据库；false 时回退到原 MySQL embedding 字段（仅兼容，不建议生产使用） */
        private boolean enabled = true;

        /** 向量数据库类型，目前仅支持 qdrant */
        private String store = "qdrant";

        private String host = "localhost";

        private int port = 6334;

        /** gRPC 是否使用明文（Docker 本地部署建议 true） */
        private boolean useTls = false;

        /** Qdrant collection 名称 */
        private String collection = "medicare_knowledge";

        /** 向量维度，需与 embedding 模型输出一致 */
        private int dimension = 1024;

        /** 相似度度量：cosine / euclid / dot */
        private String distance = "cosine";
    }

    @Data
    public static class ExecutorProperties {

        private int generationCoreSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

        private int generationMaxSize = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

        private int retrievalCoreSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

        private int retrievalMaxSize = Math.max(4, Runtime.getRuntime().availableProcessors());

        private int queueCapacity = 100;

        private int retrievalMinParallelChunks = 32;

        private int vectorCoreSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

        private int vectorMaxSize = Math.max(4, Runtime.getRuntime().availableProcessors());
    }
}
