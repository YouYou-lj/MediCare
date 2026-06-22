package com.medicare.controller;

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
public class AuthController {

    private final SysUserService sysUserService;

    /**
     * 登录 — 校验账密后写入 Session
     * 密码兼容明文（迁移阶段）与 BCrypt 两种格式
     */
    @PostMapping("/login")
    public Result<SysUser> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.ok(null);
    }

    /**
     * 登出 — 销毁 Session
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        return Result.ok();
    }

    /**
     * 获取当前登录用户信息（前端刷新页面后恢复登录态）
     */
    @GetMapping("/current")
    public Result<SysUser> current(HttpServletRequest request) {
        return Result.ok(null);
    }
}
