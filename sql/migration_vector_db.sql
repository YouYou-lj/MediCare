-- ========================================================-- MediCare 向量数据库迁移脚本-- 用途：将向量存储从 MySQL knowledge_chunk.embedding 字段迁移到专用向量数据库（Qdrant）。--        执行本脚本后，需在知识库管理页面点击「重建全部索引」，由后端重新生成向量并写入 Qdrant。-- ========================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 添加向量记录关联字段
ALTER TABLE knowledge_chunk
    ADD COLUMN IF NOT EXISTS vector_id VARCHAR(64) DEFAULT NULL COMMENT 'Qdrant 向量记录 UUID' AFTER keywords,
    ADD UNIQUE INDEX IF NOT EXISTS idx_knowledge_chunk_vector_id (vector_id);

-- 2. 删除原 MySQL 中存放的向量数据（释放空间，后续由 Qdrant 负责存储）
ALTER TABLE knowledge_chunk DROP COLUMN IF EXISTS embedding;

-- 3. 可选：清空已有分块记录，强制在重建索引时重新生成向量
--        如果希望保留文档元数据，请注释掉下面两行。
-- DELETE FROM knowledge_chunk WHERE source_path NOT LIKE 'uploads/%' AND source_path NOT LIKE 'assistant-uploads/%';
-- DELETE FROM knowledge_document WHERE source_path NOT LIKE 'uploads/%' AND source_path NOT LIKE 'assistant-uploads/%';

SET FOREIGN_KEY_CHECKS = 1;
