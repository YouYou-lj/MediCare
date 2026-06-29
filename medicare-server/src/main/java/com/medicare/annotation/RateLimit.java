package com.medicare.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解 — 基于 Redis 滑动窗口计数器实现。
 * <p>
 * 可作用于类或方法，方法级配置覆盖类级配置。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大请求数。
     */
    int limit() default 100;

    /**
     * 时间窗口长度。
     */
    long window() default 60;

    /**
     * 时间窗口单位。
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 限流维度 key 前缀。
     */
    String prefix() default "medicare:rate:";

    /**
     * 限流维度：IP 或 USER。USER 按当前登录用户ID，未登录回退到 IP。
     */
    Type type() default Type.IP;

    /**
     * 提示信息。
     */
    String message() default "请求过于频繁，请稍后再试";

    enum Type {
        IP, USER
    }
}
