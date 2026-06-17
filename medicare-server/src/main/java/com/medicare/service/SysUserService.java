package com.medicare.service;

import com.medicare.entity.SysUser;
import com.medicare.exception.BusinessException;
import com.medicare.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserRepository sysUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<SysUser> findAll() {
        return sysUserRepository.findAll();
    }

    public SysUser findById(Long id) {
        return sysUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public SysUser findByUsername(String username) {
        return sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
    }

    public SysUser create(SysUser user) {
        if (sysUserRepository.existsByUsername(user.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return sysUserRepository.save(user);
    }

    public SysUser update(Long id, SysUser user) {
        SysUser existing = findById(id);
        if (sysUserRepository.existsByUsernameAndIdNot(user.getUsername(), id)) {
            throw new BusinessException("用户名已存在");
        }
        existing.setUsername(user.getUsername());
        existing.setRealName(user.getRealName());
        existing.setRole(user.getRole());
        existing.setStatus(user.getStatus());
        existing.setDoctorId(user.getDoctorId());
        return sysUserRepository.save(existing);
    }

    public void delete(Long id) {
        SysUser user = findById(id);
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("不能删除超级管理员");
        }
        sysUserRepository.deleteById(id);
    }

    public void changePassword(Long id, String oldPassword, String newPassword) {
        SysUser user = findById(id);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserRepository.save(user);
    }

    /**
     * 登录校验（兼容明文密码和 BCrypt 密码）
     */
    public SysUser login(String username, String rawPassword) {
        SysUser user = findByUsername(username);
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        // 兼容明文密码和BCrypt密码
        boolean match;
        if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$")) {
            match = passwordEncoder.matches(rawPassword, user.getPassword());
        } else {
            // 明文密码兼容（迁移阶段）
            match = rawPassword.equals(user.getPassword());
        }
        if (!match) {
            throw new BusinessException("用户名或密码错误");
        }
        return user;
    }
}
