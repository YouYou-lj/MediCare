/*
 * File: clear_all_system_docs.sql
 * Description: 清空所有系统文件脚本
 * Author: MediCare Team
 * Date: 2026-06-26
 * Version: 1.0.0
 * Notes: 仅清理系统内置文档（systemRAGFiles/system-uploads），不删除用户上传文件；执行后需手动清理 Qdrant 向量。
 */

-- ============================================================
-- 环境初始化
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 删除系统文件分块记录
-- ============================================================

-- 清理非用户上传的系统文件分块记录
DELETE FROM knowledge_chunk
WHERE source_path NOT LIKE 'uploads/%'
  AND source_path NOT LIKE 'assistant-uploads/%';

-- ============================================================
-- 删除系统文件文档记录
-- ============================================================

-- 清理非用户上传的系统文件文档记录
DELETE FROM knowledge_document
WHERE source_path NOT LIKE 'uploads/%'
  AND source_path NOT LIKE 'assistant-uploads/%';

-- ============================================================
-- 恢复外键约束检查
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 清理 Qdrant 向量数据指引
-- ============================================================

-- 清理 Qdrant 向量数据（请在命令行执行，默认端口 6333）
-- curl -X DELETE http://localhost:6333/collections/medicare_knowledge
