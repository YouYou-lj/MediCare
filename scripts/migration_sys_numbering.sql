/*
 * File: migration_sys_numbering.sql
 * Description: 系统文件标题编号标准化脚本
 * Author: MediCare Team
 * Date: 2026-06-26
 * Version: 1.0.0
 * Notes: 将非用户上传知识库文档标题统一为 SYS-### - 文件名格式。
 */

-- 处理对象：所有非用户上传、非助手上传的知识库文档

-- ============================================================
-- 去除旧版 SYS 前缀
-- ============================================================

-- 第一步：去掉已有的 SYS### / SYS-### 前缀，还原为原始文件名
UPDATE knowledge_document
SET title = REGEXP_REPLACE(title, '^SYS-?[0-9]{3}\\s+-\\s+', '')
WHERE status = 1
  AND source_path NOT LIKE 'uploads/%'
  AND source_path NOT LIKE 'assistant-uploads/%'
  AND title REGEXP '^SYS-?[0-9]{3}\\s+-\\s+';

-- ============================================================
-- 生成新版 SYS-### 前缀
-- ============================================================

-- 第二步：按 id 排序为所有系统文件生成 SYS-### - 前缀
SET @seq = 0;
UPDATE knowledge_document kd
JOIN (
  SELECT id, (@seq := @seq + 1) AS seq
  FROM knowledge_document
  WHERE status = 1
    AND source_path NOT LIKE 'uploads/%'
    AND source_path NOT LIKE 'assistant-uploads/%'
  ORDER BY id
) numbered ON kd.id = numbered.id
SET kd.title = CONCAT('SYS-', LPAD(numbered.seq, 3, '0'), ' - ', kd.title);
