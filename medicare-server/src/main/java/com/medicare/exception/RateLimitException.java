package com.medicare.exception;

/**
 * 接口限流异常，对应 HTTP 429 Too Many Requests。
 */
public class RateLimitException extends BusinessException {

    public RateLimitException(String message) {
        super(429, message);
    }
}
