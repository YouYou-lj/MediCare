/*
 * File: rag_system_docs_cleanup.sql
 * Description: RAG 系统文件清理脚本
 * Author: MediCare Team
 * Date: 2026-06-26
 * Version: 1.0.0
 * Notes: 在知识库重建前清理旧版系统内置文档在关系库中的分块与文档记录；不删除用户上传文件；向量数据请通过后端重建索引自动清空。
 */

-- ============================================================
-- 环境初始化
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 删除系统文件分块记录
-- ============================================================

-- 删除系统文件的分块记录（向量数据保存在 knowledge_chunk.embedding 中，同步清理）
DELETE FROM knowledge_chunk
WHERE source_path NOT LIKE 'uploads/%'
  AND source_path NOT LIKE 'assistant-uploads/%';

-- ============================================================
-- 删除系统文件文档记录
-- ============================================================

-- 删除系统文件文档记录
DELETE FROM knowledge_document
WHERE source_path NOT LIKE 'uploads/%'
  AND source_path NOT LIKE 'assistant-uploads/%';

-- ============================================================
-- 恢复外键约束检查
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;
