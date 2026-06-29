package com.medicare.aspect;

import com.medicare.annotation.RateLimit;
import com.medicare.auth.AuthInterceptor;
import com.medicare.common.RateLimiter;
import com.medicare.entity.SysUser;
import com.medicare.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * {@link RateLimit} 限流注解切面。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiter rateLimiter;

    @Around("@within(rateLimit) || @annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        if (rateLimit == null) {
            return point.proceed();
        }

        String key = buildKey(rateLimit);
        boolean allowed = rateLimiter.allow(key, rateLimit.limit(), rateLimit.window(), rateLimit.unit());
        if (!allowed) {
            throw new RateLimitException(rateLimit.message());
        }
        return point.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return rateLimit.prefix() + "default";
        }
        HttpServletRequest request = attributes.getRequest();
        String dimension;
        if (rateLimit.type() == RateLimit.Type.USER) {
            SysUser user = AuthInterceptor.getCurrentUser(request);
            dimension = user != null ? "u:" + user.getId() : "ip:" + getClientIp(request);
        } else {
            dimension = "ip:" + getClientIp(request);
        }
        return rateLimit.prefix() + request.getRequestURI() + ":" + dimension;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
