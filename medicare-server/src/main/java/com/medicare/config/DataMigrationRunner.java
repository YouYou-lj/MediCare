package com.medicare.config;

import com.medicare.entity.SysUser;
import com.medicare.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        ensureKnowledgeTables();
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

    private void ensureKnowledgeTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_document (
                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                  title VARCHAR(500) NOT NULL,
                  source_type VARCHAR(50) NOT NULL,
                  source_path VARCHAR(500) NOT NULL,
                  content_hash CHAR(64) NOT NULL,
                  chunk_count INT NOT NULL DEFAULT 0,
                  status TINYINT NOT NULL DEFAULT 1,
                  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_knowledge_document_source_path (source_path),
                  KEY idx_knowledge_document_type (source_type)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_chunk (
                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                  document_id BIGINT UNSIGNED NOT NULL,
                  chunk_index INT NOT NULL,
                  title VARCHAR(500) NOT NULL,
                  content LONGTEXT NOT NULL,
                  keywords VARCHAR(1000) DEFAULT NULL,
                  embedding LONGTEXT DEFAULT NULL,
                  embedding_model VARCHAR(100) DEFAULT NULL,
                  source_path VARCHAR(500) NOT NULL,
                  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                  PRIMARY KEY (id),
                  KEY idx_knowledge_chunk_document (document_id),
                  KEY idx_knowledge_chunk_embedding_model (embedding_model),
                  KEY idx_knowledge_chunk_source_path (source_path(191)),
                  FULLTEXT KEY ft_knowledge_chunk_content (title, content, keywords)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        addColumnIfAbsent("knowledge_chunk", "embedding", "ALTER TABLE knowledge_chunk ADD COLUMN embedding LONGTEXT DEFAULT NULL");
        addColumnIfAbsent("knowledge_chunk", "embedding_model", "ALTER TABLE knowledge_chunk ADD COLUMN embedding_model VARCHAR(100) DEFAULT NULL");
        addIndexIfAbsent("knowledge_chunk", "idx_knowledge_chunk_embedding_model",
                "ALTER TABLE knowledge_chunk ADD INDEX idx_knowledge_chunk_embedding_model (embedding_model)");
    }

    private void addColumnIfAbsent(String tableName, String columnName, String sql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(sql);
        }
    }

    private void addIndexIfAbsent(String tableName, String indexName, String sql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(sql);
        }
    }
}
