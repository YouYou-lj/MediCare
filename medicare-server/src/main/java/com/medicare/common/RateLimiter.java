package com.medicare.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis + Lua 的滑动窗口限流器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 滑动窗口限流 Lua 脚本。
     * KEYS[1]: 限流 key
     * ARGV[1]: 当前时间戳（毫秒）
     * ARGV[2]: 窗口起始时间戳（毫秒）
     * ARGV[3]: 窗口最大请求数
     * ARGV[4]: 过期时间（秒）
     * 返回 1 表示允许通过，0 表示被限流。
     */
    private static final String SLIDE_WINDOW_SCRIPT =
            "redis.call('zremrangeByScore', KEYS[1], 0, ARGV[2]) " +
            "local current = redis.call('zcard', KEYS[1]) " +
            "if tonumber(current) < tonumber(ARGV[3]) then " +
            "    redis.call('zadd', KEYS[1], ARGV[1], ARGV[1]) " +
            "    redis.call('expire', KEYS[1], ARGV[4]) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 判断是否允许通过。
     *
     * @param key     限流 key
     * @param limit   窗口最大请求数
     * @param window  窗口时长
     * @param unit    窗口单位
     * @return true 表示允许通过
     */
    public boolean allow(String key, int limit, long window, TimeUnit unit) {
        long windowMillis = unit.toMillis(window);
        long now = Instant.now().toEpochMilli();
        long windowStart = now - windowMillis;
        long expireSeconds = Math.max(unit.toSeconds(window), 1);

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(SLIDE_WINDOW_SCRIPT);
            script.setResultType(Long.class);
            Long result = stringRedisTemplate.execute(script,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(windowStart),
                    String.valueOf(limit),
                    String.valueOf(expireSeconds));
            return Long.valueOf(1L).equals(result);
        } catch (Exception e) {
            log.warn("限流判断异常，默认放行: key={}", key, e);
            return true;
        }
    }

    /**
     * 基于令牌桶的限流判断（备选实现）。
     * 当前版本使用滑动窗口，保留此方法用于后续扩展。
     */
    @SuppressWarnings("unused")
    public boolean allowTokenBucket(String key, int rate, int capacity) {
        long now = Instant.now().toEpochMilli();
        String lastKey = key + ":last";
        String tokensKey = key + ":tokens";

        String script =
                "local last = redis.call('get', KEYS[1]) " +
                "local tokens = redis.call('get', KEYS[2]) " +
                "local now = tonumber(ARGV[1]) " +
                "local rate = tonumber(ARGV[2]) " +
                "local capacity = tonumber(ARGV[3]) " +
                "if not tokens then tokens = capacity else tokens = tonumber(tokens) end " +
                "if not last then last = now else last = tonumber(last) end " +
                "local delta = math.max(0, now - last) " +
                "tokens = math.min(capacity, tokens + delta * rate / 1000) " +
                "if tokens >= 1 then " +
                "    tokens = tokens - 1 " +
                "    redis.call('set', KEYS[1], now) " +
                "    redis.call('set', KEYS[2], tokens) " +
                "    return 1 " +
                "else " +
                "    redis.call('set', KEYS[1], last) " +
                "    redis.call('set', KEYS[2], tokens) " +
                "    return 0 " +
                "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        Long result = stringRedisTemplate.execute(redisScript,
                Arrays.asList(lastKey, tokensKey),
                String.valueOf(now), String.valueOf(rate), String.valueOf(capacity));
        return Long.valueOf(1L).equals(result);
    }
}
