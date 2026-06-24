-- ========================================================
-- MediCare v3.0 AI 功能数据库迁移脚本
-- 适用范围：在 medicare.sql 基础上追加 AI 相关表
-- 包含：AI 对话历史、个性化推荐、用户行为分析
-- ========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_chat_session
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '前端会话标识（唯一）',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话标题（首条消息摘要）',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_session_key` (`session_key`),
  KEY `idx_ai_session_user` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 对话会话表';

-- ----------------------------
-- Table structure for ai_chat_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint unsigned NOT NULL COMMENT '所属会话ID',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息角色：user / assistant',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_msg_session` (`session_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 对话消息表';

-- ----------------------------
-- Table structure for ai_recommendation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_recommendation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '推荐ID',
  `user_id` bigint unsigned NOT NULL COMMENT '目标用户ID',
  `role` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标角色',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '推荐类型：stock / waiting / expiring / peak / template',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '推荐标题',
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '推荐理由',
  `priority` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'normal' COMMENT '优先级：high / normal / low',
  `target_route` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转路由',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-未处理 1-已处理',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_rec_user` (`user_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 个性化推荐表';

-- ----------------------------
-- Table structure for user_behavior_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_behavior_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint unsigned DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `role` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色',
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件类型：page_view / search / business / ai / error',
  `event_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件名称',
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '对象类型',
  `target_id` bigint DEFAULT NULL COMMENT '对象ID',
  `route` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前路由',
  `detail_json` json DEFAULT NULL COMMENT '扩展详情',
  `ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP 地址',
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'User-Agent',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_behavior_user_time` (`user_id`, `create_time`),
  KEY `idx_behavior_event_time` (`event_name`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为日志表';

SET FOREIGN_KEY_CHECKS = 1;
