/*
 * File: migration_registration_seq_no.sql
 * Description: 挂号序号迁移脚本
 * Author: MediCare Team
 * Date: 2026-06-26
 * Version: 1.0.0
 * Notes: 让挂号 seq_no 按排班从 1 递增并与历史数据衔接；请先执行 migration_business_codes.sql，并确认应用已停止。
 */

-- ============================================================
-- 重新分配挂号序号
-- ============================================================

-- 为已有数据按排班重新分配连续序号（按 id 升序），保证与新增数据衔接
-- 导入数据若 seq_no 为空或不连续，此处统一按排班重排
WITH numbered AS (
  SELECT id,
         ROW_NUMBER() OVER (PARTITION BY schedule_id ORDER BY id) AS seq
  FROM registration
)
UPDATE registration r
JOIN numbered n ON r.id = n.id
SET r.seq_no = n.seq;

-- ============================================================
-- 添加排班维度唯一约束
-- ============================================================

-- 将 seq_no 设为 NOT NULL 并添加排班维度唯一约束，防止重复序号
ALTER TABLE registration MODIFY COLUMN seq_no INT UNSIGNED NOT NULL COMMENT '序号（按排班从 1 递增）';

DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS ensure_uk_reg_schedule_seq()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'registration'
      AND INDEX_NAME = 'uk_reg_schedule_seq'
  ) THEN
    ALTER TABLE registration ADD UNIQUE KEY uk_reg_schedule_seq (schedule_id, seq_no);
  END IF;
END$$
DELIMITER ;
CALL ensure_uk_reg_schedule_seq();
DROP PROCEDURE ensure_uk_reg_schedule_seq;

-- ============================================================
-- 调整挂号编号字段约束
-- ============================================================

-- 挂号编号 code 保持可为空，兼容“先 insert 取 id、再 update 回填 code”的两段式写入
ALTER TABLE registration MODIFY COLUMN code VARCHAR(32) NULL COMMENT '挂号编号';
