package com.medicare.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * HikariCP 连接池配置类
 * 提供统一的数据源（DataSource），供 Apache Commons DbUtils 使用
 * 本类仅负责连接池初始化，所有 SQL 操作均通过 Apache Commons DbUtils 完成
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class ConnectionConfig {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionConfig.class);

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/medicare?"
            + "useSSL=false"
            + "&serverTimezone=Asia/Shanghai"
            + "&characterEncoding=utf8mb4"
            + "&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "mysql";

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 连接池优化参数
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1200000);
        config.setPoolName("MediCare-HikariCP");
        config.setConnectionTestQuery("SELECT 1");

        dataSource = new HikariDataSource(config);
        logger.info("HikariCP 连接池初始化完成: {}", config.getPoolName());
    }

    private ConnectionConfig() {
        // 禁止实例化
    }

    /**
     * 获取数据源（供 Apache Commons DbUtils 使用）
     */
    public static DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 获取数据库连接（事务控制时使用）
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * 关闭连接池（应用退出时调用）
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP 连接池已关闭");
        }
    }
}
