package com.medicare.controller;

import com.medicare.annotation.RateLimit;
import com.medicare.auth.AuthInterceptor;
import com.medicare.dto.LoginRequest;
import com.medicare.dto.Result;
import com.medicare.entity.SysUser;
import com.medicare.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 认证控制器 — 登录 / 登出 / 获取当前用户
 * <p>
 * 登录流程：前端提交账密 → SysUserService.login() 校验 → 成功后存入 HttpSession → 返回用户信息（密码置空）
 * 后续请求通过 AuthInterceptor 从 Session 取用户，未登录返回 401。
 * 角色权限由 @RequireRole + RoleCheckAspect 切面控制。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "认证管理相关接口")
public class AuthController {

    private final SysUserService sysUserService;

    /**
     * 登录 — 校验账密后写入 Session
     * 密码兼容明文（迁移阶段）与 BCrypt 两种格式
     */
    @PostMapping("/login")
    @RateLimit(limit = 10, window = 60, message = "登录尝试过于频繁，请稍后再试")
    @Operation(summary = "用户登录")
    public Result<SysUser> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        SysUser user = sysUserService.login(request.getUsername(), request.getPassword());
        // 密码清空，不返回给前端
        user.setPassword(null);
        // 存入 Session，后续 AuthInterceptor 据此判断登录状态
        httpRequest.getSession(true).setAttribute(AuthInterceptor.CURRENT_USER_KEY, user);
        return Result.ok(user);
    }

    /**
     * 登出 — 销毁 Session
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.ok();
    }

    /**
     * 获取当前登录用户信息（前端刷新页面后恢复登录态）
     * <p>每次从数据库重新读取，确保修改个人信息后前端能拿到最新数据。
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前登录用户信息")
    public Result<SysUser> current(HttpServletRequest request) {
        SysUser sessionUser = AuthInterceptor.getCurrentUser(request);
        if (sessionUser == null) {
            return Result.ok(null);
        }
        // 从数据库重新读取，避免 Session 中缓存旧数据
        SysUser user = sysUserService.findById(sessionUser.getId());
        user.setPassword(null);
        // 同步更新 Session，保证后续拦截器拿到的也是最新信息
        request.getSession(false).setAttribute(AuthInterceptor.CURRENT_USER_KEY, user);
        return Result.ok(user);
    }
}
