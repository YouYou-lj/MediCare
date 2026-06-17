package com.medicare.auth;

import com.medicare.entity.SysUser;
import com.medicare.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 角色校验切面 — 检查 @RequireRole 注解声明的角色要求
 */
@Slf4j
@Aspect
@Component
public class RoleCheckAspect {

    @Around("@annotation(com.medicare.auth.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireRole requireRole = signature.getMethod().getAnnotation(RequireRole.class);
        if (requireRole == null) {
            return joinPoint.proceed();
        }

        String[] requiredRoles = requireRole.value();
        if (requiredRoles.length == 0) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        SysUser currentUser = AuthInterceptor.getCurrentUser(request);
        if (currentUser == null) {
            throw new BusinessException("未登录");
        }

        boolean hasRole = Arrays.asList(requiredRoles).contains(currentUser.getRole());
        if (!hasRole) {
            throw new BusinessException("权限不足，需要角色：" + String.join("/", requiredRoles));
        }

        return joinPoint.proceed();
    }
}
