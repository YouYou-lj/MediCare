package com.medicare.service;

import com.medicare.dao.SysUserDAO;
import com.medicare.model.SysUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * 系统用户服务
 * 仅支持管理员单一角色登录
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class SysUserService {

    private static final Logger logger = LoggerFactory.getLogger(SysUserService.class);

    private final SysUserDAO sysUserDAO = new SysUserDAO();

    /**
     * 用户登录验证
     *
     * @return 登录成功的用户对象
     * @throws IllegalArgumentException 用户名或密码错误
     */
    public SysUser login(String username, String password) throws SQLException, IllegalArgumentException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        SysUser user = sysUserDAO.findByUsername(username.trim());
        if (user == null) {
            throw new IllegalArgumentException("用户名不存在");
        }
        if (!password.equals(user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        logger.info("用户登录成功: username={}, realName={}", user.getUsername(), user.getRealName());
        return user;
    }
}
