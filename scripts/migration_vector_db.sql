/*
 * File: migration_vector_db.sql
 * Description: 向量数据库迁移脚本
 * Author: MediCare Team
 * Date: 2026-06-26
 * Version: 1.0.0
 * Notes: 将向量存储从 MySQL knowledge_chunk.embedding 字段迁移到专用向量数据库 Qdrant；执行后需在后端重建全部索引。
 */

-- ============================================================
-- 环境初始化
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 添加 Qdrant 向量关联字段
-- ============================================================

-- 添加向量记录关联字段，用于与 Qdrant 中的向量记录建立映射
-- 使用存储过程兼容 MySQL 8.0，避免 IF NOT EXISTS 语法不支持的问题
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS ensure_vector_id_column()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'knowledge_chunk'
      AND COLUMN_NAME = 'vector_id'
  ) THEN
    ALTER TABLE knowledge_chunk
      ADD COLUMN vector_id VARCHAR(64) DEFAULT NULL COMMENT 'Qdrant 向量记录 UUID' AFTER keywords;
  END IF;
END$$
DELIMITER ;
CALL ensure_vector_id_column();
DROP PROCEDURE ensure_vector_id_column;

DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS ensure_vector_id_index()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'knowledge_chunk'
      AND INDEX_NAME = 'idx_knowledge_chunk_vector_id'
  ) THEN
    ALTER TABLE knowledge_chunk ADD UNIQUE INDEX idx_knowledge_chunk_vector_id (vector_id);
  END IF;
END$$
DELIMITER ;
CALL ensure_vector_id_index();
DROP PROCEDURE ensure_vector_id_index;

-- ============================================================
-- 移除 MySQL 向量存储字段
-- ============================================================

-- 删除原 MySQL 中存放的向量数据，释放空间，后续由 Qdrant 负责存储
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS drop_embedding_column()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'knowledge_chunk'
      AND COLUMN_NAME = 'embedding'
  ) THEN
    ALTER TABLE knowledge_chunk DROP COLUMN embedding;
  END IF;
END$$
DELIMITER ;
CALL drop_embedding_column();
DROP PROCEDURE drop_embedding_column;

-- ============================================================
-- 可选：清空系统文件分块
-- ============================================================

-- 可选：清空已有分块记录，强制在重建索引时重新生成向量
--        如果希望保留文档元数据，请注释掉下面两行。
-- DELETE FROM knowledge_chunk WHERE source_path NOT LIKE 'uploads/%' AND source_path NOT LIKE 'assistant-uploads/%';
-- DELETE FROM knowledge_document WHERE source_path NOT LIKE 'uploads/%' AND source_path NOT LIKE 'assistant-uploads/%';

-- ============================================================
-- 恢复外键约束检查
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;
