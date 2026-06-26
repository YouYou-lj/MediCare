/*
 * File: migration_sys_user_management.sql
 * Description: 系统用户管理增强迁移脚本
 * Author: MediCare Team
 * Date: 2026-06-26
 * Version: 1.0.0
 * Notes: 解决新建系统用户时的约束冲突，调整用户编号字段可空性；执行前请确认应用已停止。
 */

-- ============================================================
-- 调整用户编号字段约束
-- ============================================================

-- code 保持可空，兼容“先 insert 取 id、再 update 回填 code”的两段式写入
ALTER TABLE sys_user MODIFY COLUMN code VARCHAR(32) NULL UNIQUE COMMENT '用户编号';
