/*
 * File: migration_knowledge_uploaded_by.sql
 * Description: 知识库文档增加上传人字段并初始化历史数据
 * Author: MediCare Team
 * Date: 2026-06-26
 * Version: 1.0.0
 * Notes: 为 knowledge_document 表添加 uploaded_by 字段，用于记录文档上传者，支撑按角色/所有权访问控制。
 */

-- ============================================================
-- 环境初始化
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 添加上传人字段
-- ============================================================

DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS ensure_uploaded_by_column()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'knowledge_document'
      AND COLUMN_NAME = 'uploaded_by'
  ) THEN
    ALTER TABLE knowledge_document
      ADD COLUMN uploaded_by BIGINT DEFAULT NULL COMMENT '文档上传用户ID' AFTER status;
  END IF;
END$$
DELIMITER ;
CALL ensure_uploaded_by_column();
DROP PROCEDURE ensure_uploaded_by_column;

-- ============================================================
-- 初始化历史数据
-- ============================================================

-- 系统文件统一归为主管理员（id=1）上传
UPDATE knowledge_document
SET uploaded_by = 1
WHERE uploaded_by IS NULL
  AND source_path NOT LIKE 'uploads/%'
  AND source_path NOT LIKE 'assistant-uploads/%';

-- 用户上传文件若无法确定上传者，暂归为主管理员，后续由业务层按实际上传者覆盖
UPDATE knowledge_document
SET uploaded_by = 1
WHERE uploaded_by IS NULL
  AND (source_path LIKE 'uploads/%' OR source_path LIKE 'assistant-uploads/%');

-- ============================================================
-- 恢复外键约束检查
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;
