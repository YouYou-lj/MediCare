package com.medicare.auth;

import com.medicare.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器 — 从 HttpSession 获取用户，未登录返回 401
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_KEY = "currentUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(CURRENT_USER_KEY) == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\",\"data\":null}");
            } catch (Exception e) {
                log.error("写入401响应失败", e);
            }
            return false;
        }
        return true;
    }

    /**
     * 从 Session 获取当前用户
     */
    public static SysUser getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        return (SysUser) session.getAttribute(CURRENT_USER_KEY);
    }
}
