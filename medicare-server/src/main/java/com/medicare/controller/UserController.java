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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    @GetMapping
    @RequireRole("admin")
    public Result<List<SysUser>> list() {
        List<SysUser> users = sysUserService.findAll();
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }

    @PostMapping
    @RequireRole("admin")
    public Result<SysUser> create(@Valid @RequestBody SysUser user) {
        SysUser created = sysUserService.create(user);
        created.setPassword(null);
        return Result.ok(created);
    }

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<SysUser> update(@PathVariable Long id, @Valid @RequestBody SysUser user) {
        SysUser updated = sysUserService.update(id, user);
        updated.setPassword(null);
        return Result.ok(updated);
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
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
