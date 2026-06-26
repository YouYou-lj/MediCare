/*
 * File: medicare.sql
 * Description: MediCare 基础业务数据库初始化脚本（纯净版）
 * Author: MediCare Team
 * Date: 2026-06-26
 * Version: 1.0.0
 * Notes: 仅创建核心表结构与主管理员账号，不预置任何业务数据；请在空库中执行。
 */

-- ============================================================
-- 环境初始化
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 创建表：department
-- ============================================================
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '科室ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '科室名称',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '科室位置',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_department_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室信息表：存储医院科室基础信息';

-- ============================================================
-- 创建表：doctor
-- ============================================================
DROP TABLE IF EXISTS `doctor`;
CREATE TABLE `doctor` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '医生ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '医生姓名',
  `department_id` bigint unsigned NOT NULL COMMENT '所属科室ID',
  `title` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职称（主任医师/副主任医师/主治医师/医师）',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-停用 1-在职',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_doctor_dept` (`department_id`),
  CONSTRAINT `fk_doctor_department` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生信息表：存储医生资料及所属科室';

-- ============================================================
-- 创建表：inventory_log
-- ============================================================
DROP TABLE IF EXISTS `inventory_log`;
CREATE TABLE `inventory_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `medicine_id` bigint unsigned NOT NULL COMMENT '药品ID',
  `type` tinyint NOT NULL COMMENT '类型：1-入库 2-出库 3-盘盈 4-盘亏',
  `quantity` int NOT NULL DEFAULT '0' COMMENT '数量（正数入库/负数出库）',
  `batch_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批号',
  `expiry_date` date DEFAULT NULL COMMENT '有效期',
  `operator` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `log_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_invlog_medicine_time` (`medicine_id`,`log_time`),
  KEY `idx_invlog_time` (`log_time`),
  CONSTRAINT `fk_invlog_medicine` FOREIGN KEY (`medicine_id`) REFERENCES `medicine` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存变动日志表：记录药品入库、出库、盘盈、盘亏等库存变动';

-- ============================================================
-- 创建表：medical_record
-- ============================================================
DROP TABLE IF EXISTS `medical_record`;
CREATE TABLE `medical_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '病历ID',
  `registration_id` bigint unsigned NOT NULL COMMENT '挂号ID',
  `patient_id` bigint unsigned NOT NULL COMMENT '患者ID',
  `doctor_id` bigint unsigned NOT NULL COMMENT '医生ID',
  `chief_complaint` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主诉',
  `present_illness` text COLLATE utf8mb4_unicode_ci COMMENT '现病史',
  `past_history` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '既往史',
  `physical_exam` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '体格检查',
  `diagnosis` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '诊断',
  `advice` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '医嘱',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `fk_mr_doctor` (`doctor_id`),
  KEY `idx_mr_patient_time` (`patient_id`,`create_time`),
  KEY `idx_mr_reg` (`registration_id`),
  CONSTRAINT `fk_mr_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_mr_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_mr_registration` FOREIGN KEY (`registration_id`) REFERENCES `registration` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历信息表：存储患者就诊病历内容';

-- ============================================================
-- 创建表：medicine
-- ============================================================
DROP TABLE IF EXISTS `medicine`;
CREATE TABLE `medicine` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '药品名称',
  `spec` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格',
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位（盒/瓶/支/片）',
  `stock` int unsigned DEFAULT '0' COMMENT '当前库存',
  `safety_stock` int unsigned DEFAULT '10' COMMENT '安全库存阈值',
  `expiry_date` date DEFAULT NULL COMMENT '有效期至',
  `batch_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产批号',
  `pinyin_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拼音简码',
  `price` decimal(10,2) DEFAULT '0.00' COMMENT '零售价',
  `manufacturer` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产厂家',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_medicine_name_spec` (`name`,`spec`),
  KEY `idx_medicine_pinyin` (`pinyin_code`),
  KEY `idx_medicine_expiry` (`expiry_date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品信息表：存储药品基础信息、库存及价格';

-- ============================================================
-- 创建表：patient
-- ============================================================
DROP TABLE IF EXISTS `patient`;
CREATE TABLE `patient` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '患者ID',
  `id_card` varchar(18) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '身份证号',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '患者姓名',
  `gender` tinyint NOT NULL COMMENT '性别：0-女 1-男 2-其他',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `address` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '住址',
  `allergy_info` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '过敏史',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_id_card` (`id_card`),
  KEY `idx_patient_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者信息表：存储患者基本信息与过敏史';

-- ============================================================
-- 创建表：prescription
-- ============================================================
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '处方ID',
  `record_id` bigint unsigned NOT NULL COMMENT '病历ID',
  `patient_id` bigint unsigned NOT NULL COMMENT '患者ID',
  `doctor_id` bigint unsigned NOT NULL COMMENT '医生ID',
  `total_amount` decimal(10,2) DEFAULT '0.00' COMMENT '处方总金额',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-待缴费 1-已缴费 2-已取药 3-已作废',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `fk_presc_doctor` (`doctor_id`),
  KEY `idx_presc_record` (`record_id`),
  KEY `idx_presc_patient` (`patient_id`),
  CONSTRAINT `fk_presc_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_presc_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_presc_record` FOREIGN KEY (`record_id`) REFERENCES `medical_record` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方信息表：存储处方状态及金额';

-- ============================================================
-- 创建表：prescription_item
-- ============================================================
DROP TABLE IF EXISTS `prescription_item`;
CREATE TABLE `prescription_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `prescription_id` bigint unsigned NOT NULL COMMENT '处方ID',
  `medicine_id` bigint unsigned NOT NULL COMMENT '药品ID',
  `quantity` int unsigned NOT NULL DEFAULT '1' COMMENT '数量',
  `dosage` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用法用量',
  `usage_desc` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用药说明',
  `unit_price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '单价',
  `amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '金额',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_pi_medicine` (`medicine_id`),
  KEY `idx_pi_prescription` (`prescription_id`),
  CONSTRAINT `fk_pi_medicine` FOREIGN KEY (`medicine_id`) REFERENCES `medicine` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_pi_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescription` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方明细表：存储处方药品明细、用法用量及金额';

-- ============================================================
-- 创建表：registration
-- ============================================================
DROP TABLE IF EXISTS `registration`;
CREATE TABLE `registration` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '挂号ID',
  `patient_id` bigint unsigned NOT NULL COMMENT '患者ID',
  `schedule_id` bigint unsigned NOT NULL COMMENT '排班ID',
  `doctor_id` bigint unsigned DEFAULT NULL COMMENT '医生ID',
  `reg_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '挂号时间',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-候诊 1-就诊中 2-已完成 3-已取消',
  `seq_no` int unsigned NOT NULL COMMENT '序号（按排班从 1 递增，固定不变）',
  `fee` decimal(10,2) DEFAULT '0.00' COMMENT '挂号费',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reg_schedule_seq` (`schedule_id`,`seq_no`),
  KEY `idx_reg_patient` (`patient_id`),
  KEY `idx_reg_schedule` (`schedule_id`),
  KEY `idx_reg_status` (`status`),
  CONSTRAINT `fk_reg_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_reg_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `schedule` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='挂号信息表：存储患者挂号记录及排队序号';

-- ============================================================
-- 创建表：schedule
-- ============================================================
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '排班ID',
  `doctor_id` bigint unsigned NOT NULL COMMENT '医生ID',
  `work_date` date NOT NULL COMMENT '出诊日期',
  `time_slot` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '时段（上午/下午/晚上）',
  `total_slots` int unsigned NOT NULL DEFAULT '0' COMMENT '总号源数',
  `remain_slots` int unsigned NOT NULL DEFAULT '0' COMMENT '剩余号源数',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schedule_doctor_date_slot` (`doctor_id`,`work_date`,`time_slot`),
  KEY `idx_schedule_date` (`work_date`),
  CONSTRAINT `fk_schedule_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班号源表：存储医生出诊排班与可预约号源';

-- ============================================================
-- 创建表：sys_user
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（需加密存储）',
  `real_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '真实姓名',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'doctor' COMMENT '角色：admin/administrator/doctor/pharmacist',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用 1-启用',
  `doctor_id` bigint unsigned DEFAULT NULL COMMENT '关联医生ID（医生角色时）',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表：存储登录账号、角色及关联医生信息';

-- ============================================================
-- 初始化数据：sys_user（仅主管理员）
-- ============================================================
BEGIN;
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `status`, `doctor_id`, `create_time`, `update_time`) VALUES (1, 'admin', '$2a$10$WONCjn94whlUczwJgKUNJOxai4IiYNoqJZNjNZr94IzVap9hpHYOG', '管理员', 'admin', 1, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
