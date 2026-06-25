-- ========================================================-- MediCare RAG 系统文件清理脚本-- 用途：在知识库重建前，清理旧版系统内置文档（含 SYS-001 ~ SYS-085 等）--        在关系库中的分块记录。向量数据请通过后端「重建全部索引」接口自动清空 Qdrant。-- 注意：用户上传文件（source_path 以 uploads/ 或 assistant-uploads/ 开头）不会被删除。-- ========================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 先删除系统文件的分块记录（向量数据保存在 knowledge_chunk.embedding 中，同步清理）
DELETE FROM knowledge_chunk
WHERE source_path NOT LIKE 'uploads/%'
  AND source_path NOT LIKE 'assistant-uploads/%';

-- 再删除系统文件文档记录
DELETE FROM knowledge_document
WHERE source_path NOT LIKE 'uploads/%'
  AND source_path NOT LIKE 'assistant-uploads/%';

SET FOREIGN_KEY_CHECKS = 1;
