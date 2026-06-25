-- ========================================================-- MediCare 清空所有系统文件脚本-- 用途：直接删除关系库中所有系统文件（含 systemRAGFiles 与 system-uploads）--        及其分块记录，并给出清理 Qdrant 向量的命令。-- 注意：用户上传文件（source_path 以 uploads/ 或 assistant-uploads/ 开头）不会被删除。-- ========================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 删除系统文件分块记录
DELETE FROM knowledge_chunk
WHERE source_path NOT LIKE 'uploads/%'
  AND source_path NOT LIKE 'assistant-uploads/%';

-- 删除系统文件文档记录
DELETE FROM knowledge_document
WHERE source_path NOT LIKE 'uploads/%'
  AND source_path NOT LIKE 'assistant-uploads/%';

SET FOREIGN_KEY_CHECKS = 1;

-- 清理 Qdrant 向量数据（请在命令行执行，默认端口 6333）
-- curl -X DELETE http://localhost:6333/collections/medicare_knowledge
