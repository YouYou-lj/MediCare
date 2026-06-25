package com.medicare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AiTaskExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public AiTaskExecutors aiTaskExecutors(AiProperties aiProperties) {
        AiProperties.ExecutorProperties properties = aiProperties.getExecutor();
        int queueCapacity = Math.max(16, properties.getQueueCapacity());
        int generationCoreSize = Math.max(1, properties.getGenerationCoreSize());
        int generationMaxSize = Math.max(generationCoreSize, properties.getGenerationMaxSize());
        int retrievalCoreSize = Math.max(1, properties.getRetrievalCoreSize());
        int retrievalMaxSize = Math.max(retrievalCoreSize, properties.getRetrievalMaxSize());

        ExecutorService generationExecutor = buildExecutor(
                "ai-generation",
                generationCoreSize,
                generationMaxSize,
                queueCapacity
        );
        ExecutorService retrievalExecutor = buildExecutor(
                "ai-retrieval",
                retrievalCoreSize,
                retrievalMaxSize,
                queueCapacity
        );
        int vectorCoreSize = Math.max(1, properties.getVectorCoreSize());
        int vectorMaxSize = Math.max(vectorCoreSize, properties.getVectorMaxSize());
        ExecutorService vectorExecutor = buildExecutor(
                "ai-vector",
                vectorCoreSize,
                vectorMaxSize,
                queueCapacity
        );
        return new AiTaskExecutors(generationExecutor, retrievalExecutor, retrievalMaxSize, vectorExecutor);
    }

    private ExecutorService buildExecutor(String threadNamePrefix, int coreSize, int maxSize, int queueCapacity) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                coreSize,
                maxSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new NamedThreadFactory(threadNamePrefix),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    public record AiTaskExecutors(
            ExecutorService generationExecutor,
            ExecutorService retrievalExecutor,
            int retrievalParallelism,
            ExecutorService vectorExecutor
    ) {
        public void shutdown() {
            generationExecutor.shutdown();
            retrievalExecutor.shutdown();
            vectorExecutor.shutdown();
        }

        public ExecutorService vectorExecutor() {
            return vectorExecutor;
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {

        private final String prefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}
