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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    @PostMapping("/login")
    public Result<SysUser> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        SysUser user = sysUserService.login(request.getUsername(), request.getPassword());
        // 密码清空，不返回给前端
        user.setPassword(null);
        // 存入 Session
        httpRequest.getSession(true).setAttribute(AuthInterceptor.CURRENT_USER_KEY, user);
        return Result.ok(user);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.ok();
    }

    @GetMapping("/current")
    public Result<SysUser> current(HttpServletRequest request) {
        SysUser user = AuthInterceptor.getCurrentUser(request);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.ok(user);
    }
}
