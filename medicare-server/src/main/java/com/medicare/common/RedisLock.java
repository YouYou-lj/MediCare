package com.medicare.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式锁实现。
 * <p>
 * 采用 SET NX EX 获取锁，Lua 脚本释放锁，保证原子性。
 * 锁 value 使用线程唯一标识，防止误释放其他线程的锁。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 尝试获取锁。
     *
     * @param key     锁 key
     * @param expire  锁过期时间
     * @return 锁上下文，获取失败返回 null
     */
    public LockContext tryLock(String key, Duration expire) {
        String value = Thread.currentThread().getId() + ":" + UUID.randomUUID();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, expire.getSeconds(), TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(acquired)) {
            return new LockContext(key, value, this);
        }
        return null;
    }

    /**
     * 尝试获取锁，失败时等待重试。
     *
     * @param key       锁 key
     * @param expire    锁过期时间
     * @param waitTime  最大等待时间
     * @param retryInterval 重试间隔
     * @return 锁上下文，获取失败返回 null
     * @throws InterruptedException 等待被中断
     */
    public LockContext tryLock(String key, Duration expire, Duration waitTime, Duration retryInterval)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + waitTime.toMillis();
        while (System.currentTimeMillis() < deadline) {
            LockContext context = tryLock(key, expire);
            if (context != null) {
                return context;
            }
            Thread.sleep(retryInterval.toMillis());
        }
        return null;
    }

    /**
     * 释放锁。仅当 value 匹配时才删除。
     */
    public boolean unlock(LockContext context) {
        if (context == null) {
            return false;
        }
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);
            Long result = stringRedisTemplate.execute(script, Collections.singletonList(context.key), context.value);
            return Long.valueOf(1L).equals(result);
        } catch (Exception e) {
            log.warn("释放分布式锁失败: key={}", context.key, e);
            return false;
        }
    }

    /**
     * 锁上下文，配合 try-with-resources 自动释放。
     */
    public static class LockContext implements AutoCloseable {

        private final String key;
        private final String value;
        private final RedisLock redisLock;
        private boolean released = false;

        public LockContext(String key, String value, RedisLock redisLock) {
            this.key = key;
            this.value = value;
            this.redisLock = redisLock;
        }

        @Override
        public void close() {
            if (!released) {
                redisLock.unlock(this);
                released = true;
            }
        }
    }
}
