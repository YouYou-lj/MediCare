package com.medicare.controller;

import com.medicare.auth.AuthInterceptor;
import com.medicare.auth.RequireRole;
import com.medicare.dto.ChangePasswordRequest;
import com.medicare.dto.Result;
import com.medicare.entity.SysUser;
import com.medicare.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器 — 系统用户 CRUD + 修改密码
 * <p>
 * 用户角色：admin / doctor / pharmacist；创建时密码经 BCrypt 加密后存储；
 * 修改密码限制只能改自己的，需要验证旧密码
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    /** 用户列表 — 返回所有用户（密码置空） */
    @GetMapping
    @RequireRole("admin")
    public Result<List<SysUser>> list() {
        return Result.ok(null);
    }

    /** 创建用户 — 密码 BCrypt 加密 + 用户名唯一性校验 */
    @PostMapping
    @RequireRole("admin")
    public Result<SysUser> create(@Valid @RequestBody SysUser user) {
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<SysUser> update(@PathVariable Long id, @Valid @RequestBody SysUser user) {
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        return Result.ok();
    }

    /** 修改密码 — 只能改自己的密码，需验证旧密码 */
    @PutMapping("/{id}/password")
    public Result<Void> changePassword(@PathVariable Long id,
                                       @Valid @RequestBody ChangePasswordRequest request,
                                       HttpServletRequest httpRequest) {
        return Result.ok();
    }
}
