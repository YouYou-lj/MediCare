/*
 Navicat Premium Data Transfer

 Source Server         : local8.0
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : medicare

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 18/06/2026 15:20:17
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for department
-- ----------------------------
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室表';

-- ----------------------------
-- Records of department
-- ----------------------------
BEGIN;
INSERT INTO `department` (`id`, `name`, `location`, `phone`, `create_time`, `update_time`) VALUES (1, '内科', '门诊楼 1 层 A 区', '025-88880001', '2026-06-10 22:11:24.001', '2026-06-10 22:11:24.001');
INSERT INTO `department` (`id`, `name`, `location`, `phone`, `create_time`, `update_time`) VALUES (2, '外科', '门诊楼 1 层 B 区', '025-88880002', '2026-06-10 22:11:24.001', '2026-06-10 22:11:24.001');
INSERT INTO `department` (`id`, `name`, `location`, `phone`, `create_time`, `update_time`) VALUES (3, '儿科', '门诊楼 2 层 C 区', '025-88880003', '2026-06-10 22:11:24.001', '2026-06-10 22:11:24.001');
INSERT INTO `department` (`id`, `name`, `location`, `phone`, `create_time`, `update_time`) VALUES (4, '妇产科', '门诊楼 2 层 D 区', '025-88880004', '2026-06-10 22:11:24.001', '2026-06-10 22:11:24.001');
INSERT INTO `department` (`id`, `name`, `location`, `phone`, `create_time`, `update_time`) VALUES (5, '中医科', '门诊楼 3 层 E 区', '025-88880005', '2026-06-10 22:11:24.001', '2026-06-10 22:11:24.001');
COMMIT;

-- ----------------------------
-- Table structure for doctor
-- ----------------------------
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生表';

-- ----------------------------
-- Records of doctor
-- ----------------------------
BEGIN;
INSERT INTO `doctor` (`id`, `name`, `department_id`, `title`, `status`, `create_time`, `update_time`) VALUES (1, '张伟', 1, '主任医师', 1, '2026-06-10 22:11:24.002', '2026-06-10 22:11:24.002');
INSERT INTO `doctor` (`id`, `name`, `department_id`, `title`, `status`, `create_time`, `update_time`) VALUES (2, '李娜', 1, '副主任医师', 1, '2026-06-10 22:11:24.002', '2026-06-10 22:11:24.002');
INSERT INTO `doctor` (`id`, `name`, `department_id`, `title`, `status`, `create_time`, `update_time`) VALUES (3, '王强', 2, '主治医师', 1, '2026-06-10 22:11:24.002', '2026-06-10 22:11:24.002');
INSERT INTO `doctor` (`id`, `name`, `department_id`, `title`, `status`, `create_time`, `update_time`) VALUES (4, '刘洋', 3, '主任医师', 1, '2026-06-10 22:11:24.002', '2026-06-10 22:11:24.002');
INSERT INTO `doctor` (`id`, `name`, `department_id`, `title`, `status`, `create_time`, `update_time`) VALUES (5, '陈静', 4, '副主任医师', 1, '2026-06-10 22:11:24.002', '2026-06-10 22:11:24.002');
INSERT INTO `doctor` (`id`, `name`, `department_id`, `title`, `status`, `create_time`, `update_time`) VALUES (6, '赵敏', 5, '医师', 1, '2026-06-10 22:11:24.002', '2026-06-10 22:11:24.002');
COMMIT;

-- ----------------------------
-- Table structure for inventory_log
-- ----------------------------
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
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存变动日志表';

-- ----------------------------
-- Records of inventory_log
-- ----------------------------
BEGIN;
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (9, 1, 1, 50, 'B2026002', '2027-12-31', 'admin', '测试入库50', '2026-06-11 00:29:18.449');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (10, 1, 2, -20, NULL, NULL, 'admin', '测试出库20', '2026-06-11 00:29:18.465');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (11, 1, 2, -30, NULL, NULL, 'admin', '恢复库存', '2026-06-11 00:29:18.471');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (12, 4, 1, 1000, 'H221022', '2029-06-30', '老王', '', '2026-06-11 00:30:35.121');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (13, 1, 1, 10, 'B2026030', '2028-06-30', 'admin', '测试更新批号有效期', '2026-06-11 00:33:48.017');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (14, 1, 1, 5, NULL, NULL, 'admin', '测试保留原值', '2026-06-11 00:33:48.023');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (15, 1, 2, -15, 'B2026030', '2028-06-30', 'admin', '恢复库存', '2026-06-11 00:33:48.027');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (16, 4, 1, 100, 'h20000', '2028-06-30', '老苏', '', '2026-06-11 00:34:41.129');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (17, 4, 2, -100, 'h20000', '2028-06-30', '老王', '', '2026-06-11 00:35:03.070');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (18, 6, 1, 100, '国药122322', '2028-06-30', '', '', '2026-06-11 07:48:09.954');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (19, 1, 1, 100, 'B20260601', '2027-06-01', 'admin', '测试入库', '2026-06-17 16:01:32.192');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (20, 1, 2, 10, 'B20260601', '2027-06-01', 'admin', '测试出库', '2026-06-17 16:01:32.216');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (21, 1, 2, 2, NULL, NULL, 'system', '处方出库 - 处方ID:3', '2026-06-17 16:07:41.456');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (22, 2, 2, 1, NULL, NULL, 'system', '处方出库 - 处方ID:3', '2026-06-17 16:07:41.459');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (23, 1, 1, 100, 'B20260601', '2027-06-01', 'admin', '测试入库', '2026-06-17 16:07:41.503');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (24, 1, 2, 10, 'B20260601', '2027-06-01', 'admin', '测试出库', '2026-06-17 16:07:41.510');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (25, 1, 2, 2, NULL, NULL, 'system', '处方出库 - 处方ID:4', '2026-06-17 16:11:02.039');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (26, 2, 2, 1, NULL, NULL, 'system', '处方出库 - 处方ID:4', '2026-06-17 16:11:02.042');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (27, 1, 1, 100, 'B20260601', '2027-06-01', 'admin', '测试入库', '2026-06-17 16:11:02.081');
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (28, 1, 2, 10, 'B20260601', '2027-06-01', 'admin', '测试出库', '2026-06-17 16:11:02.087');
COMMIT;

-- ----------------------------
-- Table structure for medical_record
-- ----------------------------
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历表';

-- ----------------------------
-- Records of medical_record
-- ----------------------------
BEGIN;
INSERT INTO `medical_record` (`id`, `registration_id`, `patient_id`, `doctor_id`, `chief_complaint`, `present_illness`, `past_history`, `physical_exam`, `diagnosis`, `advice`, `create_time`, `update_time`) VALUES (1, 1, 1, 1, '嗓子疼,轻微咳嗽', '无', '无', '扁桃体轻微肿大', '季节性感冒', '复用999感冒灵7日', '2026-06-11 07:38:58.343', '2026-06-11 07:38:58.343');
INSERT INTO `medical_record` (`id`, `registration_id`, `patient_id`, `doctor_id`, `chief_complaint`, `present_illness`, `past_history`, `physical_exam`, `diagnosis`, `advice`, `create_time`, `update_time`) VALUES (2, 4, 22, 2, '头疼', '无', '无', '健康', '神经衰弱', '布若芬分散片1盒,头疼时服用1颗,注意休息', '2026-06-11 07:45:17.825', '2026-06-11 07:45:17.825');
INSERT INTO `medical_record` (`id`, `registration_id`, `patient_id`, `doctor_id`, `chief_complaint`, `present_illness`, `past_history`, `physical_exam`, `diagnosis`, `advice`, `create_time`, `update_time`) VALUES (3, 5, 25, 1, '头痛三天-修改', NULL, NULL, NULL, '高血压2级', NULL, '2026-06-17 16:07:41.407', '2026-06-17 16:07:41.424');
INSERT INTO `medical_record` (`id`, `registration_id`, `patient_id`, `doctor_id`, `chief_complaint`, `present_illness`, `past_history`, `physical_exam`, `diagnosis`, `advice`, `create_time`, `update_time`) VALUES (4, 6, 26, 1, '头痛三天-修改', NULL, NULL, NULL, '高血压2级', NULL, '2026-06-17 16:11:01.990', '2026-06-17 16:11:02.008');
COMMIT;

-- ----------------------------
-- Table structure for medicine
-- ----------------------------
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
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品表';

-- ----------------------------
-- Records of medicine
-- ----------------------------
BEGIN;
INSERT INTO `medicine` (`id`, `name`, `spec`, `unit`, `stock`, `safety_stock`, `expiry_date`, `batch_no`, `pinyin_code`, `price`, `manufacturer`, `status`, `create_time`, `update_time`) VALUES (1, '阿莫西林胶囊', '0.25g*24粒', '盒', 766, 50, '2028-06-30', 'B2026030', 'AMXL', 12.50, '华北制药', 1, '2026-06-10 22:11:24.003', '2026-06-17 16:11:02.086');
INSERT INTO `medicine` (`id`, `name`, `spec`, `unit`, `stock`, `safety_stock`, `expiry_date`, `batch_no`, `pinyin_code`, `price`, `manufacturer`, `status`, `create_time`, `update_time`) VALUES (2, '布洛芬缓释胶囊', '0.3g*20粒', '盒', 298, 30, NULL, NULL, 'BLF', 18.00, '芬必得', 1, '2026-06-10 22:11:24.003', '2026-06-17 16:11:02.040');
INSERT INTO `medicine` (`id`, `name`, `spec`, `unit`, `stock`, `safety_stock`, `expiry_date`, `batch_no`, `pinyin_code`, `price`, `manufacturer`, `status`, `create_time`, `update_time`) VALUES (3, '感冒清热颗粒', '12g*10袋', '盒', 200, 20, NULL, NULL, 'GMQRKL', 15.80, '同仁堂', 1, '2026-06-10 22:11:24.003', '2026-06-10 22:11:24.003');
INSERT INTO `medicine` (`id`, `name`, `spec`, `unit`, `stock`, `safety_stock`, `expiry_date`, `batch_no`, `pinyin_code`, `price`, `manufacturer`, `status`, `create_time`, `update_time`) VALUES (4, '头孢克肟片', '0.1g*6片', '盒', 1150, 15, '2028-06-30', 'h20000', 'TBKW', 28.50, '白云山', 1, '2026-06-10 22:11:24.003', '2026-06-11 00:35:03.069');
INSERT INTO `medicine` (`id`, `name`, `spec`, `unit`, `stock`, `safety_stock`, `expiry_date`, `batch_no`, `pinyin_code`, `price`, `manufacturer`, `status`, `create_time`, `update_time`) VALUES (5, '维生素C片', '0.1g*100片', '瓶', 100, 10, NULL, NULL, 'WSSC', 6.50, '东北制药', 1, '2026-06-10 22:11:24.003', '2026-06-10 22:11:24.003');
INSERT INTO `medicine` (`id`, `name`, `spec`, `unit`, `stock`, `safety_stock`, `expiry_date`, `batch_no`, `pinyin_code`, `price`, `manufacturer`, `status`, `create_time`, `update_time`) VALUES (6, '阿莫西林', '0.25g*12片', '盒', 100, 10, '2028-06-30', '国药122322', '', 7.60, 'AMXL', 1, '2026-06-10 23:06:19.249', '2026-06-11 07:48:09.952');
INSERT INTO `medicine` (`id`, `name`, `spec`, `unit`, `stock`, `safety_stock`, `expiry_date`, `batch_no`, `pinyin_code`, `price`, `manufacturer`, `status`, `create_time`, `update_time`) VALUES (11, '左氧氟沙星胶囊', '0.5g*24粒', '盒', 0, 10, '2028-06-30', 'HW11200', 'ZYFSX', 23.40, '太极制药集团', 1, '2026-06-10 23:51:08.560', '2026-06-10 23:51:08.560');
COMMIT;

-- ----------------------------
-- Table structure for patient
-- ----------------------------
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
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者表';

-- ----------------------------
-- Records of patient
-- ----------------------------
BEGIN;
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (1, '110101197001011234', '张伟', 1, '1979-07-11', '13800138001', '北京市朝阳区建国路1号', '青霉素过敏', '2026-06-11 00:38:06.124', '2026-06-17 13:56:20.492');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (2, '310101198502153321', '王芳', 0, '1985-02-15', '13900139002', '上海市黄浦区南京东路2号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (3, '440106199008207891', '李强', 1, '1990-08-20', '15000150003', '广州市天河区珠江新城3号', '磺胺类药物过敏', '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (4, '510107197512054512', '刘洋', 0, '1975-12-05', '18600186004', '成都市武侯区人民南路4号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (5, '330106198803122337', '陈静', 0, '1988-03-12', '13800138005', '杭州市西湖区文三路5号', '花粉过敏', '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (6, '420106199510018876', '杨帆', 1, '1995-10-01', '13900139006', '武汉市武昌区中南路6号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (7, '610104197209204429', '赵敏', 0, '1972-09-20', '15000150007', '西安市碑林区解放路7号', '海鲜过敏', '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (8, '500103198110158113', '黄磊', 1, '1981-10-15', '18600186008', '重庆市渝中区解放碑8号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (9, '370102199307224055', '周杰', 1, '1993-07-22', '13800138009', '济南市历下区泉城路9号', '阿司匹林过敏', '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (10, '410105197806301988', '吴丽', 0, '1978-06-30', '13900139010', '郑州市金水区花园路10号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (11, '320106198912157621', '徐鹏', 1, '1989-12-15', '15000150011', '南京市鼓楼区中山路11号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (12, '430103199202083452', '孙婷', 0, '1992-02-08', '18600186012', '长沙市芙蓉区五一路12号', '头孢过敏', '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (13, '230103197411204113', '马丽', 0, '1974-11-20', '13800138013', '哈尔滨市南岗区果戈里大街13号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (14, '120101198605172776', '朱伟', 1, '1986-05-17', '13900139014', '天津市和平区南京路14号', '花生过敏', '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (15, '530102199409085631', '胡军', 1, '1994-09-08', '15000150015', '昆明市五华区东风西路15号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (16, '210102197305224829', '郭静', 0, '1973-05-22', '18600186016', '沈阳市沈河区青年大街16号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (17, '340102198701153914', '林峰', 1, '1987-01-15', '13800138017', '合肥市庐阳区长江中路17号', '尘螨过敏', '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (18, '520102199611304226', '何萍', 0, '1996-11-30', '13900139018', '贵阳市云岩区中华北路18号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (19, '130102197904121557', '高翔', 1, '1979-04-12', '15000150019', '石家庄市长安区建设大街19号', NULL, '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (20, '140107198305068743', '梁雨', 0, '1983-05-06', '18600186020', '太原市小店区长风街20号', '芒果过敏', '2026-06-11 00:38:06.124', '2026-06-11 00:38:06.124');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (21, '321282200801040918', '王小二', 1, '2008-06-27', '13951027333', '', '', '2026-06-11 07:26:25.120', '2026-06-11 07:26:25.120');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (22, '32128219901003021x', '刘欣', 0, '1990-10-01', '13302210021', '', '', '2026-06-11 07:36:42.425', '2026-06-11 07:36:42.425');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (23, '320106199901011234', '测试患者', 1, '1999-01-01', '13800138000', '南京市鼓楼区', '无', '2026-06-17 16:00:59.121', '2026-06-17 16:00:59.121');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (24, '320106200001011111', '临时患者', 0, NULL, '13700000000', NULL, NULL, '2026-06-17 16:01:32.327', '2026-06-17 16:01:32.327');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (25, '32010681683661', '测试患者', 1, '1999-01-01', '13800138000', '南京市鼓楼区', '无', '2026-06-17 16:07:41.299', '2026-06-17 16:07:41.299');
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (26, '32010681683861', '测试患者-修改', 1, NULL, '13900139000', '南京市玄武区', NULL, '2026-06-17 16:11:01.898', '2026-06-17 16:11:01.923');
COMMIT;

-- ----------------------------
-- Table structure for prescription
-- ----------------------------
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方表';

-- ----------------------------
-- Records of prescription
-- ----------------------------
BEGIN;
INSERT INTO `prescription` (`id`, `record_id`, `patient_id`, `doctor_id`, `total_amount`, `status`, `create_time`, `update_time`) VALUES (3, 3, 25, 1, 43.00, 0, '2026-06-17 16:07:41.449', '2026-06-17 16:07:41.449');
INSERT INTO `prescription` (`id`, `record_id`, `patient_id`, `doctor_id`, `total_amount`, `status`, `create_time`, `update_time`) VALUES (4, 4, 26, 1, 43.00, 0, '2026-06-17 16:11:02.033', '2026-06-17 16:11:02.033');
COMMIT;

-- ----------------------------
-- Table structure for prescription_item
-- ----------------------------
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方明细表';

-- ----------------------------
-- Records of prescription_item
-- ----------------------------
BEGIN;
INSERT INTO `prescription_item` (`id`, `prescription_id`, `medicine_id`, `quantity`, `dosage`, `usage_desc`, `unit_price`, `amount`, `create_time`) VALUES (1, 3, 1, 2, '一日三次', '饭后服用', 12.50, 25.00, '2026-06-17 16:07:41.454');
INSERT INTO `prescription_item` (`id`, `prescription_id`, `medicine_id`, `quantity`, `dosage`, `usage_desc`, `unit_price`, `amount`, `create_time`) VALUES (2, 3, 2, 1, '一日一次', '睡前服用', 18.00, 18.00, '2026-06-17 16:07:41.458');
INSERT INTO `prescription_item` (`id`, `prescription_id`, `medicine_id`, `quantity`, `dosage`, `usage_desc`, `unit_price`, `amount`, `create_time`) VALUES (3, 4, 1, 2, '一日三次', '饭后服用', 12.50, 25.00, '2026-06-17 16:11:02.038');
INSERT INTO `prescription_item` (`id`, `prescription_id`, `medicine_id`, `quantity`, `dosage`, `usage_desc`, `unit_price`, `amount`, `create_time`) VALUES (4, 4, 2, 1, '一日一次', '睡前服用', 18.00, 18.00, '2026-06-17 16:11:02.041');
COMMIT;

-- ----------------------------
-- Table structure for registration
-- ----------------------------
DROP TABLE IF EXISTS `registration`;
CREATE TABLE `registration` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '挂号ID',
  `patient_id` bigint unsigned NOT NULL COMMENT '患者ID',
  `schedule_id` bigint unsigned NOT NULL COMMENT '排班ID',
  `doctor_id` bigint unsigned DEFAULT NULL COMMENT '医生ID',
  `reg_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '挂号时间',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-候诊 1-就诊中 2-已完成 3-已取消',
  `seq_no` int unsigned DEFAULT NULL COMMENT '序号',
  `fee` decimal(10,2) DEFAULT '0.00' COMMENT '挂号费',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_reg_patient` (`patient_id`),
  KEY `idx_reg_schedule` (`schedule_id`),
  KEY `idx_reg_status` (`status`),
  CONSTRAINT `fk_reg_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_reg_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `schedule` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='挂号表';

-- ----------------------------
-- Records of registration
-- ----------------------------
BEGIN;
INSERT INTO `registration` (`id`, `patient_id`, `schedule_id`, `doctor_id`, `reg_time`, `status`, `seq_no`, `fee`, `create_time`, `update_time`) VALUES (1, 1, 1, 1, '2026-06-11 07:25:13.000', 2, 1, 10.00, '2026-06-11 07:25:13.340', '2026-06-17 13:52:41.251');
INSERT INTO `registration` (`id`, `patient_id`, `schedule_id`, `doctor_id`, `reg_time`, `status`, `seq_no`, `fee`, `create_time`, `update_time`) VALUES (3, 21, 3, 4, '2026-06-11 07:35:34.000', 2, 2, 10.00, '2026-06-11 07:35:34.432', '2026-06-17 13:52:41.251');
INSERT INTO `registration` (`id`, `patient_id`, `schedule_id`, `doctor_id`, `reg_time`, `status`, `seq_no`, `fee`, `create_time`, `update_time`) VALUES (4, 22, 2, 2, '2026-06-11 07:36:45.000', 2, 3, 10.00, '2026-06-11 07:36:45.750', '2026-06-17 13:52:41.251');
INSERT INTO `registration` (`id`, `patient_id`, `schedule_id`, `doctor_id`, `reg_time`, `status`, `seq_no`, `fee`, `create_time`, `update_time`) VALUES (5, 25, 4, 1, '2026-06-17 16:07:41.383', 2, 1, 10.00, '2026-06-17 16:07:41.383', '2026-06-17 16:07:41.395');
INSERT INTO `registration` (`id`, `patient_id`, `schedule_id`, `doctor_id`, `reg_time`, `status`, `seq_no`, `fee`, `create_time`, `update_time`) VALUES (6, 26, 4, 1, '2026-06-17 16:11:01.975', 2, 2, 10.00, '2026-06-17 16:11:01.975', '2026-06-17 16:11:01.983');
INSERT INTO `registration` (`id`, `patient_id`, `schedule_id`, `doctor_id`, `reg_time`, `status`, `seq_no`, `fee`, `create_time`, `update_time`) VALUES (7, 8, 4, 1, '2026-06-17 22:37:39.769', 0, 3, 10.00, '2026-06-17 22:37:39.769', '2026-06-17 22:37:39.769');
INSERT INTO `registration` (`id`, `patient_id`, `schedule_id`, `doctor_id`, `reg_time`, `status`, `seq_no`, `fee`, `create_time`, `update_time`) VALUES (8, 2, 4, 1, '2026-06-17 22:37:51.856', 0, 4, 10.00, '2026-06-17 22:37:51.856', '2026-06-17 22:37:51.856');
INSERT INTO `registration` (`id`, `patient_id`, `schedule_id`, `doctor_id`, `reg_time`, `status`, `seq_no`, `fee`, `create_time`, `update_time`) VALUES (9, 21, 4, 1, '2026-06-17 22:56:23.310', 0, 5, 10.00, '2026-06-17 22:56:23.310', '2026-06-17 22:56:23.310');
COMMIT;

-- ----------------------------
-- Table structure for schedule
-- ----------------------------
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班号源表';

-- ----------------------------
-- Records of schedule
-- ----------------------------
BEGIN;
INSERT INTO `schedule` (`id`, `doctor_id`, `work_date`, `time_slot`, `total_slots`, `remain_slots`, `create_time`, `update_time`) VALUES (1, 1, '2026-06-11', '上午', 20, 19, '2026-06-11 00:27:04.118', '2026-06-11 07:35:11.245');
INSERT INTO `schedule` (`id`, `doctor_id`, `work_date`, `time_slot`, `total_slots`, `remain_slots`, `create_time`, `update_time`) VALUES (2, 2, '2026-06-11', '上午', 20, 18, '2026-06-11 00:27:09.250', '2026-06-11 07:36:45.748');
INSERT INTO `schedule` (`id`, `doctor_id`, `work_date`, `time_slot`, `total_slots`, `remain_slots`, `create_time`, `update_time`) VALUES (3, 4, '2026-06-11', '上午', 20, 16, '2026-06-11 00:27:12.614', '2026-06-11 07:35:34.430');
INSERT INTO `schedule` (`id`, `doctor_id`, `work_date`, `time_slot`, `total_slots`, `remain_slots`, `create_time`, `update_time`) VALUES (4, 1, '2026-06-17', '上午', 20, 15, '2026-06-17 16:07:41.367', '2026-06-17 22:56:23.290');
COMMIT;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
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
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `status`, `doctor_id`, `create_time`, `update_time`) VALUES (1, 'admin', '$2a$10$fJGYeJJLLTkIa/dEfYBHkuiV0KGwr6x/D1ch26AU7dRhXqcHbqY7.', '老板', 'admin', 1, NULL, '2026-06-10 22:11:24.002', '2026-06-17 16:11:02.309');
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `status`, `doctor_id`, `create_time`, `update_time`) VALUES (8, 'admin2', '$2a$10$oXH5bPFAqoexHjj/9q0IZeRmYIIsAu7C/LTPchBH4YBw5Kd05HI3C', '老徐', 'admin', 1, NULL, '2026-06-10 23:03:12.202', '2026-06-17 13:36:47.910');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
