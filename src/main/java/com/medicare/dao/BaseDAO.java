package com.medicare.dao;

import com.medicare.util.ConnectionConfig;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 基础 DAO 类（泛型 + 反射）
 * 所有 DAO 必须继承此类，禁止直接 new QueryRunner
 * 事务控制统一在 Service 层，DAO 层禁止自行管理事务
 *
 * @param <T> 实体类型
 * @author MediCare Team
 * @date 2026-06-10
 */
public abstract class BaseDAO<T> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final QueryRunner queryRunner = new QueryRunner();
    private final Class<T> clazz;

    @SuppressWarnings("unchecked")
    protected BaseDAO() {
        // 通过反射获取泛型类型
        this.clazz = (Class<T>) ((java.lang.reflect.ParameterizedType)
                getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    // ============================================================
    // 查询方法（无事务，自动获取连接）
    // ============================================================

    /**
     * 查询单条记录
     */
    protected T querySingle(String sql, Object... params) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            return queryRunner.query(conn, sql, new BeanHandler<>(clazz), params);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * 查询多条记录
     */
    protected List<T> queryList(String sql, Object... params) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            return queryRunner.query(conn, sql, new BeanListHandler<>(clazz), params);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * 查询单个值（如 COUNT、SUM）
     */
    protected <E> E queryScalar(String sql, Object... params) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            return queryRunner.query(conn, sql, new ScalarHandler<>(), params);
        } finally {
            closeQuietly(conn);
        }
    }

    // ============================================================
    // 更新方法（无事务，自动获取连接）
    // ============================================================

    /**
     * 执行增删改操作
     * @return 影响的行数
     */
    protected int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            return queryRunner.update(conn, sql, params);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * 执行插入并返回生成的主键
     */
    protected Long executeInsert(String sql, Object... params) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            return queryRunner.insert(conn, sql, new ScalarHandler<>(), params);
        } finally {
            closeQuietly(conn);
        }
    }

    // ============================================================
    // 事务控制方法（供 Service 层调用）
    // ============================================================

    /**
     * 获取连接（用于 Service 层手动事务控制）
     */
    public Connection getConnection() throws SQLException {
        Connection conn = ConnectionConfig.getConnection();
        conn.setAutoCommit(false);
        return conn;
    }

    /**
     * 在指定连接上执行更新（用于事务中）
     */
    protected int executeUpdate(Connection conn, String sql, Object... params) throws SQLException {
        return queryRunner.update(conn, sql, params);
    }

    /**
     * 在指定连接上执行查询（用于事务中）
     */
    protected T querySingle(Connection conn, String sql, Object... params) throws SQLException {
        return queryRunner.query(conn, sql, new BeanHandler<>(clazz), params);
    }

    /**
     * 在指定连接上查询列表（用于事务中）
     */
    protected List<T> queryList(Connection conn, String sql, Object... params) throws SQLException {
        return queryRunner.query(conn, sql, new BeanListHandler<>(clazz), params);
    }

    /**
     * 在指定连接上查询标量（用于事务中）
     */
    protected <E> E queryScalar(Connection conn, String sql, Object... params) throws SQLException {
        return queryRunner.query(conn, sql, new ScalarHandler<>(), params);
    }

    /**
     * 在指定连接上执行插入并返回主键（用于事务中）
     */
    protected Long executeInsert(Connection conn, String sql, Object... params) throws SQLException {
        return queryRunner.insert(conn, sql, new ScalarHandler<>(), params);
    }

    // ============================================================
    // 事务辅助方法
    // ============================================================

    public void commit(Connection conn) {
        if (conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                logger.error("事务提交失败", e);
            }
        }
    }

    public void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
                logger.warn("事务已回滚");
            } catch (SQLException e) {
                logger.error("事务回滚失败", e);
            }
        }
    }

    public void closeConnection(Connection conn) {
        closeQuietly(conn);
    }

    // ============================================================
    // 私有辅助方法
    // ============================================================

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warn("关闭连接异常", e);
            }
        }
    }
}
