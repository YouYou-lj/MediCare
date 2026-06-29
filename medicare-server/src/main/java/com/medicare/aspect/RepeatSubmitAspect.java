package com.medicare.aspect;

import com.medicare.annotation.RepeatSubmitLock;
import com.medicare.auth.AuthInterceptor;
import com.medicare.entity.SysUser;
import com.medicare.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * {@link RepeatSubmitLock} 防重复提交切面。
 * <p>
 * 以 "用户 + 请求 URI + 请求体签名" 作为 key，在指定时间窗口内只允许一次提交。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(repeatSubmitLock)")
    public Object around(ProceedingJoinPoint point, RepeatSubmitLock repeatSubmitLock) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return point.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String userPart = resolveUserPart(request);
        String uri = request.getRequestURI();
        String bodySignature = signatureArgs(point.getArgs());
        String key = repeatSubmitLock.prefix() + userPart + ":" + uri + ":" + bodySignature;

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", repeatSubmitLock.timeout(), repeatSubmitLock.unit());
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(409, repeatSubmitLock.message());
        }

        try {
            return point.proceed();
        } catch (Exception e) {
            // 业务异常时删除幂等锁，允许用户重试
            stringRedisTemplate.delete(key);
            throw e;
        }
    }

    private String resolveUserPart(HttpServletRequest request) {
        SysUser user = AuthInterceptor.getCurrentUser(request);
        if (user != null && user.getId() != null) {
            return "u:" + user.getId();
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip.split(",")[0].trim();
    }

    private String signatureArgs(Object[] args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg != null) {
                sb.append(arg.getClass().getName()).append(arg.toString());
            }
        }
        return sha256(sb.toString());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
