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

    private String baseUrl = "https://api.openai.com/v1";

    private Duration timeout = Duration.ofSeconds(20);
}
