-- ============================================================
-- MediCare 智慧医疗门诊管理系统 - 数据库基线脚本
-- 版本: v1.0.0
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS medicare
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE medicare;

-- ------------------------------------------------------------
-- 1. 科室表 (department)
-- ------------------------------------------------------------
CREATE TABLE department (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '科室ID',
    name            VARCHAR(50) NOT NULL COMMENT '科室名称',
    location        VARCHAR(100) COMMENT '科室位置',
    phone           VARCHAR(20) COMMENT '联系电话',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_department_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室表';

-- ------------------------------------------------------------
-- 2. 医生表 (doctor)
-- ------------------------------------------------------------
CREATE TABLE doctor (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '医生ID',
    name            VARCHAR(50) NOT NULL COMMENT '医生姓名',
    department_id   BIGINT UNSIGNED NOT NULL COMMENT '所属科室ID',
    title           VARCHAR(30) COMMENT '职称（主任医师/副主任医师/主治医师/医师）',
    status          TINYINT DEFAULT 1 COMMENT '状态：0-停用 1-在职',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    CONSTRAINT fk_doctor_department FOREIGN KEY (department_id) REFERENCES department(id) ON DELETE RESTRICT,
    KEY idx_doctor_dept (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生表';

-- ------------------------------------------------------------
-- 3. 患者表 (patient)
-- ------------------------------------------------------------
CREATE TABLE patient (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '患者ID',
    id_card         VARCHAR(18) NOT NULL COMMENT '身份证号',
    name            VARCHAR(50) NOT NULL COMMENT '患者姓名',
    gender          TINYINT NOT NULL COMMENT '性别：0-女 1-男 2-其他',
    birth_date      DATE COMMENT '出生日期',
    phone           VARCHAR(20) COMMENT '手机号',
    address         VARCHAR(200) COMMENT '住址',
    allergy_info    VARCHAR(500) COMMENT '过敏史',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_patient_id_card (id_card),
    KEY idx_patient_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者表';

-- ------------------------------------------------------------
-- 4. 排班/号源表 (schedule)
-- ------------------------------------------------------------
CREATE TABLE schedule (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '排班ID',
    doctor_id       BIGINT UNSIGNED NOT NULL COMMENT '医生ID',
    work_date       DATE NOT NULL COMMENT '出诊日期',
    time_slot       VARCHAR(20) NOT NULL COMMENT '时段（上午/下午/晚上）',
    total_slots     INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总号源数',
    remain_slots    INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '剩余号源数',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id) ON DELETE RESTRICT,
    UNIQUE KEY uk_schedule_doctor_date_slot (doctor_id, work_date, time_slot),
    KEY idx_schedule_date (work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班号源表';

-- ------------------------------------------------------------
-- 5. 挂号表 (registration)
-- ------------------------------------------------------------
CREATE TABLE registration (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '挂号ID',
    patient_id      BIGINT UNSIGNED NOT NULL COMMENT '患者ID',
    schedule_id     BIGINT UNSIGNED NOT NULL COMMENT '排班ID',
    doctor_id       BIGINT UNSIGNED COMMENT '医生ID（冗余，挂号时从排班复制）',
    reg_time        DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '挂号时间',
    status          TINYINT DEFAULT 0 COMMENT '状态：0-候诊 1-就诊中 2-已完成 3-已取消',
    seq_no          INT UNSIGNED COMMENT '序号',
    fee             DECIMAL(10,2) DEFAULT 0.00 COMMENT '挂号费',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    CONSTRAINT fk_reg_patient FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reg_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id) ON DELETE RESTRICT,
    KEY idx_reg_patient (patient_id),
    KEY idx_reg_schedule (schedule_id),
    KEY idx_reg_status (status),
    KEY idx_reg_doctor (doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='挂号表';

-- ------------------------------------------------------------
-- 6. 病历表 (medical_record)
-- ------------------------------------------------------------
CREATE TABLE medical_record (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '病历ID',
    registration_id BIGINT UNSIGNED NOT NULL COMMENT '挂号ID',
    patient_id      BIGINT UNSIGNED NOT NULL COMMENT '患者ID',
    doctor_id       BIGINT UNSIGNED NOT NULL COMMENT '医生ID',
    chief_complaint VARCHAR(500) COMMENT '主诉',
    present_illness TEXT COMMENT '现病史',
    past_history    VARCHAR(1000) COMMENT '既往史',
    physical_exam   VARCHAR(1000) COMMENT '体格检查',
    diagnosis       VARCHAR(500) COMMENT '诊断',
    advice          VARCHAR(1000) COMMENT '医嘱',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    CONSTRAINT fk_mr_registration FOREIGN KEY (registration_id) REFERENCES registration(id) ON DELETE RESTRICT,
    CONSTRAINT fk_mr_patient FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE RESTRICT,
    CONSTRAINT fk_mr_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id) ON DELETE RESTRICT,
    KEY idx_mr_patient_time (patient_id, create_time),
    KEY idx_mr_reg (registration_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历表';

-- ------------------------------------------------------------
-- 7. 处方表 (prescription)
-- ------------------------------------------------------------
CREATE TABLE prescription (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '处方ID',
    record_id       BIGINT UNSIGNED NOT NULL COMMENT '病历ID',
    patient_id      BIGINT UNSIGNED NOT NULL COMMENT '患者ID',
    doctor_id       BIGINT UNSIGNED NOT NULL COMMENT '医生ID',
    total_amount    DECIMAL(10,2) DEFAULT 0.00 COMMENT '处方总金额',
    status          TINYINT DEFAULT 0 COMMENT '状态：0-待缴费 1-已缴费 2-已取药 3-已作废',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    CONSTRAINT fk_presc_record FOREIGN KEY (record_id) REFERENCES medical_record(id) ON DELETE RESTRICT,
    CONSTRAINT fk_presc_patient FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE RESTRICT,
    CONSTRAINT fk_presc_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id) ON DELETE RESTRICT,
    KEY idx_presc_record (record_id),
    KEY idx_presc_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方表';

-- ------------------------------------------------------------
-- 8. 药品表 (medicine)
-- ------------------------------------------------------------
CREATE TABLE medicine (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '药品ID',
    name            VARCHAR(100) NOT NULL COMMENT '药品名称',
    spec            VARCHAR(100) COMMENT '规格',
    unit            VARCHAR(20) COMMENT '单位（盒/瓶/支/片）',
    stock           INT UNSIGNED DEFAULT 0 COMMENT '当前库存',
    safety_stock    INT UNSIGNED DEFAULT 10 COMMENT '安全库存阈值',
    expiry_date     DATE COMMENT '有效期至',
    batch_no        VARCHAR(50) COMMENT '生产批号',
    pinyin_code     VARCHAR(50) COMMENT '拼音简码',
    price           DECIMAL(10,2) DEFAULT 0.00 COMMENT '零售价',
    manufacturer    VARCHAR(200) COMMENT '生产厂家',
    status          TINYINT DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_medicine_name_spec (name, spec),
    KEY idx_medicine_pinyin (pinyin_code),
    KEY idx_medicine_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品表';

-- ------------------------------------------------------------
-- 9. 处方明细表 (prescription_item)
-- ------------------------------------------------------------
CREATE TABLE prescription_item (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '明细ID',
    prescription_id BIGINT UNSIGNED NOT NULL COMMENT '处方ID',
    medicine_id     BIGINT UNSIGNED NOT NULL COMMENT '药品ID',
    quantity        INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '数量',
    dosage          VARCHAR(200) COMMENT '用法用量',
    usage_desc      VARCHAR(200) COMMENT '用药说明',
    unit_price      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    amount          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    CONSTRAINT fk_pi_prescription FOREIGN KEY (prescription_id) REFERENCES prescription(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pi_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(id) ON DELETE RESTRICT,
    KEY idx_pi_prescription (prescription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方明细表';

-- ------------------------------------------------------------
-- 10. 库存记录表 (inventory_log)
-- ------------------------------------------------------------
CREATE TABLE inventory_log (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    medicine_id     BIGINT UNSIGNED NOT NULL COMMENT '药品ID',
    type            TINYINT NOT NULL COMMENT '类型：1-入库 2-出库 3-盘盈 4-盘亏',
    quantity        INT NOT NULL DEFAULT 0 COMMENT '数量（正数入库/负数出库）',
    batch_no        VARCHAR(50) COMMENT '批号',
    expiry_date     DATE COMMENT '有效期',
    operator        VARCHAR(50) COMMENT '操作人',
    remark          VARCHAR(500) COMMENT '备注',
    log_time        DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '记录时间',
    CONSTRAINT fk_invlog_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(id) ON DELETE RESTRICT,
    KEY idx_invlog_medicine_time (medicine_id, log_time),
    KEY idx_invlog_time (log_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存变动日志表';

-- ------------------------------------------------------------
-- 11. 系统用户表 (sys_user) - 用于医生工作站登录
-- ------------------------------------------------------------
CREATE TABLE sys_user (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username        VARCHAR(50) NOT NULL COMMENT '登录账号',
    password        VARCHAR(100) NOT NULL COMMENT '密码（需加密存储）',
    real_name       VARCHAR(50) COMMENT '真实姓名',
    role            VARCHAR(20) DEFAULT 'doctor' COMMENT '角色：admin/administrator/doctor/pharmacist',
    status          TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    doctor_id       BIGINT UNSIGNED COMMENT '关联医生ID（医生角色时）',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_doctor (doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 科室初始数据
INSERT INTO department (name, location, phone) VALUES
('内科', '门诊楼 1 层 A 区', '025-88880001'),
('外科', '门诊楼 1 层 B 区', '025-88880002'),
('儿科', '门诊楼 2 层 C 区', '025-88880003'),
('妇产科', '门诊楼 2 层 D 区', '025-88880004'),
('中医科', '门诊楼 3 层 E 区', '025-88880005');

-- 医生初始数据
INSERT INTO doctor (name, department_id, title, status) VALUES
('张伟', 1, '主任医师', 1),
('李娜', 1, '副主任医师', 1),
('王强', 2, '主治医师', 1),
('刘洋', 3, '主任医师', 1),
('陈静', 4, '副主任医师', 1),
('赵敏', 5, '医师', 1);

-- 管理员账号（密码: admin123，实际应使用加密存储）
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
('admin', 'admin123', '系统管理员', 'admin', 1);

-- 药品初始数据
INSERT INTO medicine (name, spec, unit, stock, safety_stock, pinyin_code, price, manufacturer, status) VALUES
('阿莫西林胶囊', '0.25g*24粒', '盒', 500, 50, 'AMXL', 12.50, '华北制药', 1),
('布洛芬缓释胶囊', '0.3g*20粒', '盒', 300, 30, 'BLF', 18.00, '芬必得', 1),
('感冒清热颗粒', '12g*10袋', '盒', 200, 20, 'GMQRKL', 15.80, '同仁堂', 1),
('头孢克肟片', '0.1g*6片', '盒', 150, 15, 'TBKW', 28.50, '白云山', 1),
('维生素C片', '0.1g*100片', '瓶', 100, 10, 'WSSC', 6.50, '东北制药', 1);
