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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 用户管理控制器 — 系统用户 CRUD + 修改密码
 * <p>
 * 用户角色：admin / doctor / pharmacist；创建时密码经 BCrypt 加密后存储；
 * 修改密码限制只能改自己的，需要验证旧密码
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    private final SysUserService sysUserService;

    /** 用户列表 — 返回所有用户（密码置空） */
    @GetMapping
    @RequireRole("admin")
    @Operation(summary = "查询用户列表")
    public Result<List<SysUser>> list() {
        List<SysUser> users = sysUserService.findAll();
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }

    /** 创建用户 — 密码 BCrypt 加密 + 用户名唯一性校验 */
    @PostMapping
    @RequireRole("admin")
    @Operation(summary = "新增用户")
    public Result<SysUser> create(@Valid @RequestBody SysUser user, HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        // 非主管理员禁止创建管理员账号
        if (!isMainAdmin(currentUser) && SysUser.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error(403, "只有主管理员可以创建管理员账号");
        }
        SysUser created = sysUserService.create(user);
        created.setPassword(null);
        return Result.ok(created);
    }

    @PutMapping("/{id}")
    @RequireRole("admin")
    @Operation(summary = "更新用户")
    public Result<SysUser> update(@PathVariable Long id, @Valid @RequestBody SysUser user, HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        String checkMsg = checkUserEditPermission(currentUser, id, user.getRole());
        if (checkMsg != null) {
            return Result.error(403, checkMsg);
        }
        SysUser updated = sysUserService.update(id, user);
        updated.setPassword(null);
        return Result.ok(updated);
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    @Operation(summary = "删除用户")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        String checkMsg = checkUserDeletePermission(currentUser, id);
        if (checkMsg != null) {
            return Result.error(403, checkMsg);
        }
        sysUserService.delete(id);
        return Result.ok();
    }

    /** 判断是否为系统主管理员（id=1，即初始 admin） */
    private boolean isMainAdmin(SysUser user) {
        return user != null && user.getId() != null && user.getId() == 1L;
    }

    /** 校验当前用户是否有权修改目标用户 */
    private String checkUserEditPermission(SysUser currentUser, Long targetId, String newRole) {
        if (isMainAdmin(currentUser)) {
            return null; // 主管理员可修改任意用户
        }
        SysUser target = sysUserService.findById(targetId);
        // 禁止修改主管理员
        if (isMainAdmin(target)) {
            return "无权修改主管理员";
        }
        // 管理员之间不能相互修改
        if (SysUser.ROLE_ADMIN.equals(target.getRole())) {
            return "无权修改其他管理员";
        }
        // 非主管理员不能将普通用户提升为管理员
        if (SysUser.ROLE_ADMIN.equals(newRole) && !SysUser.ROLE_ADMIN.equals(target.getRole())) {
            return "只有主管理员可以将用户设置为管理员";
        }
        return null;
    }

    /** 校验当前用户是否有权删除目标用户 */
    private String checkUserDeletePermission(SysUser currentUser, Long targetId) {
        if (isMainAdmin(currentUser)) {
            return null; // 主管理员可删除任意用户（系统保留 admin 自身在 service 层拦截）
        }
        SysUser target = sysUserService.findById(targetId);
        if (isMainAdmin(target)) {
            return "无权删除主管理员";
        }
        if (SysUser.ROLE_ADMIN.equals(target.getRole())) {
            return "无权删除其他管理员";
        }
        return null;
    }

    /** 修改密码 — 只能改自己的密码，需验证旧密码 */
    @PutMapping("/{id}/password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(@PathVariable Long id,
                                       @Valid @RequestBody ChangePasswordRequest request,
                                       HttpServletRequest httpRequest) {
        // 只能改自己的密码
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        if (currentUser == null || !currentUser.getId().equals(id)) {
            return Result.error(403, "只能修改自己的密码");
        }
        sysUserService.changePassword(id, request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }
}
