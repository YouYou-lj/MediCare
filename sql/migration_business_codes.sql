-- ========================================================
-- 全系统业务编号迁移脚本
-- 为各核心业务表新增 code 字段，并按 id 回填 PREFIX-000001 格式编号
-- 执行前请确认应用已停止，避免 JPA 自动建表与脚本冲突
-- ========================================================

-- 患者
ALTER TABLE patient ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '患者编号';
UPDATE patient SET code = CONCAT('PAT-', LPAD(id, 6, '0'));
ALTER TABLE patient MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '患者编号';

-- 医生
ALTER TABLE doctor ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '医生编号';
UPDATE doctor SET code = CONCAT('DOC-', LPAD(id, 6, '0'));
ALTER TABLE doctor MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '医生编号';

-- 科室
ALTER TABLE department ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '科室编号';
UPDATE department SET code = CONCAT('DEP-', LPAD(id, 6, '0'));
ALTER TABLE department MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '科室编号';

-- 药品
ALTER TABLE medicine ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '药品编号';
UPDATE medicine SET code = CONCAT('MED-', LPAD(id, 6, '0'));
ALTER TABLE medicine MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '药品编号';

-- 病历
ALTER TABLE medical_record ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '病历编号';
UPDATE medical_record SET code = CONCAT('REC-', LPAD(id, 6, '0'));
ALTER TABLE medical_record MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '病历编号';

-- 处方
ALTER TABLE prescription ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '处方编号';
UPDATE prescription SET code = CONCAT('PRE-', LPAD(id, 6, '0'));
ALTER TABLE prescription MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '处方编号';

-- 处方明细
ALTER TABLE prescription_item ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '处方明细编号';
UPDATE prescription_item SET code = CONCAT('PIT-', LPAD(id, 6, '0'));
ALTER TABLE prescription_item MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '处方明细编号';

-- 挂号
ALTER TABLE registration ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '挂号编号';
UPDATE registration SET code = CONCAT('REG-', LPAD(id, 6, '0'));
ALTER TABLE registration MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '挂号编号';

-- 排班号源
ALTER TABLE schedule ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '排班编号';
UPDATE schedule SET code = CONCAT('SCH-', LPAD(id, 6, '0'));
ALTER TABLE schedule MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '排班编号';

-- 系统用户
ALTER TABLE sys_user ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '用户编号';
UPDATE sys_user SET code = CONCAT('USR-', LPAD(id, 6, '0'));
ALTER TABLE sys_user MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '用户编号';

-- 库存日志
ALTER TABLE inventory_log ADD COLUMN code VARCHAR(32) UNIQUE COMMENT '库存日志编号';
UPDATE inventory_log SET code = CONCAT('INV-', LPAD(id, 6, '0'));
ALTER TABLE inventory_log MODIFY COLUMN code VARCHAR(32) NOT NULL UNIQUE COMMENT '库存日志编号';
