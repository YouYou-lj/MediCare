package com.medicare.config;

import com.medicare.entity.SysUser;
import com.medicare.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据迁移运行器 — 首次启动时将 sys_user 明文密码转为 BCrypt 格式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final SysUserRepository sysUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        List<SysUser> users = sysUserRepository.findAll();
        int migrated = 0;
        for (SysUser user : users) {
            // 如果密码不是 BCrypt 格式，则加密
            if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                sysUserRepository.save(user);
                migrated++;
                log.info("迁移用户密码: {} -> BCrypt", user.getUsername());
            }
        }
        if (migrated > 0) {
            log.info("密码迁移完成，共迁移 {} 个用户", migrated);
        } else {
            log.info("所有用户密码已是 BCrypt 格式，无需迁移");
        }
    }
}
