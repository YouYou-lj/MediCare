package com.medicare.service;

import com.medicare.entity.SysUser;
import com.medicare.exception.BusinessException;
import com.medicare.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统用户服务 — 用户 CRUD + 登录校验 + 密码管理
 * <p>
 * 密码策略：创建时 BCrypt 加密；登录时兼容明文和 BCrypt（迁移过渡期）；
 * 修改密码需验证旧密码
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserRepository sysUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<SysUser> findAll() {
        return null;
    }

    public SysUser findById(Long id) {
        return null;
    }

    public SysUser findByUsername(String username) {
        return null;
    }

    /** 创建用户 — 密码 BCrypt 加密 + 用户名唯一性校验 */
    public SysUser create(SysUser user) {
        return null;
    }

    public SysUser update(Long id, SysUser user) {
        return null;
    }

    /** 删除用户 — 禁止删除超级管理员 admin */
    public void delete(Long id) {
    }

    public void changePassword(Long id, String oldPassword, String newPassword) {
    }

    /**
     * 登录校验（兼容明文密码和 BCrypt 密码）
     */
    public SysUser login(String username, String rawPassword) {
        return null;
    }
}
