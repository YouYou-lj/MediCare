package com.medicare.dao;

import com.medicare.model.SysUser;

import java.sql.SQLException;

/**
 * 系统用户数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class SysUserDAO extends BaseDAO<SysUser> {

    private static final String SQL_SELECT_BY_USERNAME =
            "SELECT id, username, password, real_name, role, status, doctor_id, create_time, update_time " +
            "FROM sys_user WHERE username = ? AND status = 1";

    public SysUser findByUsername(String username) throws SQLException {
        return querySingle(SQL_SELECT_BY_USERNAME, username);
    }
}
