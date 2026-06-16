mysqldump: [Warning] Using a password on the command line interface can be insecure.
-- MySQL dump 10.13  Distrib 8.0.46, for macos14.8 (x86_64)
--
-- Host: localhost    Database: medicare
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
INSERT INTO `department` (`id`, `name`, `location`, `phone`, `create_time`, `update_time`) VALUES (1,'内科','门诊楼 1 层 A 区','025-88880001','2026-06-10 22:11:24.001','2026-06-10 22:11:24.001'),(2,'外科','门诊楼 1 层 B 区','025-88880002','2026-06-10 22:11:24.001','2026-06-10 22:11:24.001'),(3,'儿科','门诊楼 2 层 C 区','025-88880003','2026-06-10 22:11:24.001','2026-06-10 22:11:24.001'),(4,'妇产科','门诊楼 2 层 D 区','025-88880004','2026-06-10 22:11:24.001','2026-06-10 22:11:24.001'),(5,'中医科','门诊楼 3 层 E 区','025-88880005','2026-06-10 22:11:24.001','2026-06-10 22:11:24.001');
/*!40000 ALTER TABLE `department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `doctor`
--

LOCK TABLES `doctor` WRITE;
/*!40000 ALTER TABLE `doctor` DISABLE KEYS */;
INSERT INTO `doctor` (`id`, `name`, `department_id`, `title`, `status`, `create_time`, `update_time`) VALUES (1,'张伟',1,'主任医师',1,'2026-06-10 22:11:24.002','2026-06-10 22:11:24.002'),(2,'李娜',1,'副主任医师',1,'2026-06-10 22:11:24.002','2026-06-10 22:11:24.002'),(3,'王强',2,'主治医师',1,'2026-06-10 22:11:24.002','2026-06-10 22:11:24.002'),(4,'刘洋',3,'主任医师',1,'2026-06-10 22:11:24.002','2026-06-10 22:11:24.002'),(5,'陈静',4,'副主任医师',1,'2026-06-10 22:11:24.002','2026-06-10 22:11:24.002'),(6,'赵敏',5,'医师',1,'2026-06-10 22:11:24.002','2026-06-10 22:11:24.002');
/*!40000 ALTER TABLE `doctor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `inventory_log`
--

LOCK TABLES `inventory_log` WRITE;
/*!40000 ALTER TABLE `inventory_log` DISABLE KEYS */;
INSERT INTO `inventory_log` (`id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `remark`, `log_time`) VALUES (9,1,1,50,'B2026002','2027-12-31','admin','测试入库50','2026-06-11 00:29:18.449'),(10,1,2,-20,NULL,NULL,'admin','测试出库20','2026-06-11 00:29:18.465'),(11,1,2,-30,NULL,NULL,'admin','恢复库存','2026-06-11 00:29:18.471'),(12,4,1,1000,'H221022','2029-06-30','老王','','2026-06-11 00:30:35.121'),(13,1,1,10,'B2026030','2028-06-30','admin','测试更新批号有效期','2026-06-11 00:33:48.017'),(14,1,1,5,NULL,NULL,'admin','测试保留原值','2026-06-11 00:33:48.023'),(15,1,2,-15,'B2026030','2028-06-30','admin','恢复库存','2026-06-11 00:33:48.027'),(16,4,1,100,'h20000','2028-06-30','老苏','','2026-06-11 00:34:41.129'),(17,4,2,-100,'h20000','2028-06-30','老王','','2026-06-11 00:35:03.070'),(18,6,1,100,'国药122322','2028-06-30','','','2026-06-11 07:48:09.954');
/*!40000 ALTER TABLE `inventory_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `medical_record`
--

LOCK TABLES `medical_record` WRITE;
/*!40000 ALTER TABLE `medical_record` DISABLE KEYS */;
INSERT INTO `medical_record` (`id`, `registration_id`, `patient_id`, `doctor_id`, `chief_complaint`, `present_illness`, `past_history`, `physical_exam`, `diagnosis`, `advice`, `create_time`, `update_time`) VALUES (1,1,1,1,'嗓子疼,轻微咳嗽','无','无','扁桃体轻微肿大','季节性感冒','复用999感冒灵7日','2026-06-11 07:38:58.343','2026-06-11 07:38:58.343'),(2,4,22,2,'头疼','无','无','健康','神经衰弱','布若芬分散片1盒,头疼时服用1颗,注意休息','2026-06-11 07:45:17.825','2026-06-11 07:45:17.825');
/*!40000 ALTER TABLE `medical_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `medicine`
--

LOCK TABLES `medicine` WRITE;
/*!40000 ALTER TABLE `medicine` DISABLE KEYS */;
INSERT INTO `medicine` (`id`, `name`, `spec`, `unit`, `stock`, `safety_stock`, `expiry_date`, `batch_no`, `pinyin_code`, `price`, `manufacturer`, `status`, `create_time`, `update_time`) VALUES (1,'阿莫西林胶囊','0.25g*24粒','盒',500,50,'2028-06-30','B2026030','AMXL',12.50,'华北制药',1,'2026-06-10 22:11:24.003','2026-06-11 00:33:48.026'),(2,'布洛芬缓释胶囊','0.3g*20粒','盒',300,30,NULL,NULL,'BLF',18.00,'芬必得',1,'2026-06-10 22:11:24.003','2026-06-10 22:11:24.003'),(3,'感冒清热颗粒','12g*10袋','盒',200,20,NULL,NULL,'GMQRKL',15.80,'同仁堂',1,'2026-06-10 22:11:24.003','2026-06-10 22:11:24.003'),(4,'头孢克肟片','0.1g*6片','盒',1150,15,'2028-06-30','h20000','TBKW',28.50,'白云山',1,'2026-06-10 22:11:24.003','2026-06-11 00:35:03.069'),(5,'维生素C片','0.1g*100片','瓶',100,10,NULL,NULL,'WSSC',6.50,'东北制药',1,'2026-06-10 22:11:24.003','2026-06-10 22:11:24.003'),(6,'阿莫西林','0.25g*12片','盒',100,10,'2028-06-30','国药122322','',7.60,'AMXL',1,'2026-06-10 23:06:19.249','2026-06-11 07:48:09.952'),(11,'左氧氟沙星胶囊','0.5g*24粒','盒',0,10,'2028-06-30','HW11200','ZYFSX',23.40,'太极制药集团',1,'2026-06-10 23:51:08.560','2026-06-10 23:51:08.560');
/*!40000 ALTER TABLE `medicine` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `patient`
--

LOCK TABLES `patient` WRITE;
/*!40000 ALTER TABLE `patient` DISABLE KEYS */;
INSERT INTO `patient` (`id`, `id_card`, `name`, `gender`, `birth_date`, `phone`, `address`, `allergy_info`, `create_time`, `update_time`) VALUES (1,'110101197001011234','张伟',1,'1970-01-01','13800138001','北京市朝阳区建国路1号','青霉素过敏','2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(2,'310101198502153321','王芳',0,'1985-02-15','13900139002','上海市黄浦区南京东路2号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(3,'440106199008207891','李强',1,'1990-08-20','15000150003','广州市天河区珠江新城3号','磺胺类药物过敏','2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(4,'510107197512054512','刘洋',0,'1975-12-05','18600186004','成都市武侯区人民南路4号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(5,'330106198803122337','陈静',0,'1988-03-12','13800138005','杭州市西湖区文三路5号','花粉过敏','2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(6,'420106199510018876','杨帆',1,'1995-10-01','13900139006','武汉市武昌区中南路6号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(7,'610104197209204429','赵敏',0,'1972-09-20','15000150007','西安市碑林区解放路7号','海鲜过敏','2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(8,'500103198110158113','黄磊',1,'1981-10-15','18600186008','重庆市渝中区解放碑8号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(9,'370102199307224055','周杰',1,'1993-07-22','13800138009','济南市历下区泉城路9号','阿司匹林过敏','2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(10,'410105197806301988','吴丽',0,'1978-06-30','13900139010','郑州市金水区花园路10号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(11,'320106198912157621','徐鹏',1,'1989-12-15','15000150011','南京市鼓楼区中山路11号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(12,'430103199202083452','孙婷',0,'1992-02-08','18600186012','长沙市芙蓉区五一路12号','头孢过敏','2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(13,'230103197411204113','马丽',0,'1974-11-20','13800138013','哈尔滨市南岗区果戈里大街13号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(14,'120101198605172776','朱伟',1,'1986-05-17','13900139014','天津市和平区南京路14号','花生过敏','2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(15,'530102199409085631','胡军',1,'1994-09-08','15000150015','昆明市五华区东风西路15号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(16,'210102197305224829','郭静',0,'1973-05-22','18600186016','沈阳市沈河区青年大街16号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(17,'340102198701153914','林峰',1,'1987-01-15','13800138017','合肥市庐阳区长江中路17号','尘螨过敏','2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(18,'520102199611304226','何萍',0,'1996-11-30','13900139018','贵阳市云岩区中华北路18号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(19,'130102197904121557','高翔',1,'1979-04-12','15000150019','石家庄市长安区建设大街19号',NULL,'2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(20,'140107198305068743','梁雨',0,'1983-05-06','18600186020','太原市小店区长风街20号','芒果过敏','2026-06-11 00:38:06.124','2026-06-11 00:38:06.124'),(21,'321282200801040918','王小二',1,'2008-06-27','13951027333','','','2026-06-11 07:26:25.120','2026-06-11 07:26:25.120'),(22,'32128219901003021x','刘欣',0,'1990-10-01','13302210021','','','2026-06-11 07:36:42.425','2026-06-11 07:36:42.425');
/*!40000 ALTER TABLE `patient` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `prescription`
--

LOCK TABLES `prescription` WRITE;
/*!40000 ALTER TABLE `prescription` DISABLE KEYS */;
/*!40000 ALTER TABLE `prescription` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `prescription_item`
--

LOCK TABLES `prescription_item` WRITE;
/*!40000 ALTER TABLE `prescription_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `prescription_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `registration`
--

LOCK TABLES `registration` WRITE;
/*!40000 ALTER TABLE `registration` DISABLE KEYS */;
INSERT INTO `registration` (`id`, `patient_id`, `schedule_id`, `reg_time`, `status`, `seq_no`, `fee`, `create_time`, `update_time`) VALUES (1,1,1,'2026-06-11 07:25:13.000',2,1,10.00,'2026-06-11 07:25:13.340','2026-06-11 07:39:06.244'),(3,21,3,'2026-06-11 07:35:34.000',2,2,10.00,'2026-06-11 07:35:34.432','2026-06-11 07:37:15.488'),(4,22,2,'2026-06-11 07:36:45.000',2,3,10.00,'2026-06-11 07:36:45.750','2026-06-11 07:45:24.207');
/*!40000 ALTER TABLE `registration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `schedule`
--

LOCK TABLES `schedule` WRITE;
/*!40000 ALTER TABLE `schedule` DISABLE KEYS */;
INSERT INTO `schedule` (`id`, `doctor_id`, `work_date`, `time_slot`, `total_slots`, `remain_slots`, `create_time`, `update_time`) VALUES (1,1,'2026-06-11','上午',20,19,'2026-06-11 00:27:04.118','2026-06-11 07:35:11.245'),(2,2,'2026-06-11','上午',20,18,'2026-06-11 00:27:09.250','2026-06-11 07:36:45.748'),(3,4,'2026-06-11','上午',20,16,'2026-06-11 00:27:12.614','2026-06-11 07:35:34.430');
/*!40000 ALTER TABLE `schedule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `status`, `doctor_id`, `create_time`, `update_time`) VALUES (1,'admin','12345','老板','admin',1,NULL,'2026-06-10 22:11:24.002','2026-06-10 23:02:49.178'),(8,'admin2','222222','老徐','admin',1,NULL,'2026-06-10 23:03:12.202','2026-06-10 23:03:24.200');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-11 13:29:00
