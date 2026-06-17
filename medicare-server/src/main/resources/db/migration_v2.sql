-- ============================================================
-- MediCare V2 迁移脚本 — 从 JavaFX 桌面版迁移到 Vue+SpringBoot Web 版
-- 执行前提：已运行 schema_baseline.sql
-- ============================================================

USE medicare;

-- 1. Registration 表添加 doctor_id 字段
-- 修复：原表缺少此字段，导致 PrescriptionController 需要 workaround
ALTER TABLE registration ADD COLUMN doctor_id BIGINT UNSIGNED COMMENT '医生ID' AFTER schedule_id;

-- 2. 为已有挂号记录回填 doctor_id（从 schedule 关联获取）
UPDATE registration r
    INNER JOIN schedule s ON r.schedule_id = s.id
SET r.doctor_id = s.doctor_id
WHERE r.doctor_id IS NULL;

-- 3. 添加 doctor_id 索引
ALTER TABLE registration ADD KEY idx_reg_doctor (doctor_id);

-- 4. 添加 doctor_id 外键约束
ALTER TABLE registration ADD CONSTRAINT fk_reg_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id) ON DELETE RESTRICT;

-- 5. 更新 sys_user 密码为 BCrypt 格式（由 DataMigrationRunner 在首次启动时自动完成）
-- 此处仅作记录，不直接执行，避免重复加密

-- 完成
SELECT 'MediCare V2 迁移完成' AS message;
