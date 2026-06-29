package com.medicare.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交注解 — 基于 Redis 对同一用户在指定时间窗口内的相同请求做幂等控制。
 * <p>
 * 典型场景：前端按钮快速双击、网络重试导致同一表单多次提交。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmitLock {

    /**
     * 锁定的最大时间。
     */
    long timeout() default 3;

    /**
     * 锁定时间单位。
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * key 前缀。
     */
    String prefix() default "medicare:repeat:";

    /**
     * 提示信息。
     */
    String message() default "请勿重复提交";
}
