# MediCare SQL 参考手册

> 本手册按模块整理所有数据库 SQL，供学生开发时参考。  
> **规范**：所有 SQL 必须定义为 `private static final String` 常量，禁止在方法内拼接 SQL。

---

## 一、基础数据模块（老师演示）

### 1.1 科室表 (department)

```sql
-- 建表 DDL（已在 schema_baseline.sql 中提供）
CREATE TABLE department (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    location    VARCHAR(100),
    phone       VARCHAR(20),
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_department_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入
INSERT INTO department (name, location, phone) VALUES (?, ?, ?);

-- 更新
UPDATE department SET name = ?, location = ?, phone = ? WHERE id = ?;

-- 删除
DELETE FROM department WHERE id = ?;

-- 按 ID 查询
SELECT id, name, location, phone, create_time, update_time 
FROM department WHERE id = ?;

-- 查询全部
SELECT id, name, location, phone, create_time, update_time 
FROM department ORDER BY id;

-- 按名称模糊查询
SELECT id, name, location, phone, create_time, update_time 
FROM department WHERE name LIKE ? ORDER BY id;

-- 检查名称是否已存在
SELECT COUNT(*) FROM department WHERE name = ?;

-- 检查名称是否已被其他记录使用（编辑时）
SELECT COUNT(*) FROM department WHERE name = ? AND id != ?;

-- 检查科室下是否有医生（删除前校验）
SELECT COUNT(*) FROM doctor WHERE department_id = ?;
```

### 1.2 医生表 (doctor)

```sql
-- JOIN 查询（带科室名称别名 departmentName）
SELECT d.id, d.name, d.department_id, d.title, d.status, 
       d.create_time, d.update_time,
       dep.name AS departmentName
FROM doctor d 
LEFT JOIN department dep ON d.department_id = dep.id 
WHERE d.id = ?;

-- 按科室查询在职医生
SELECT d.id, d.name, d.department_id, d.title, d.status,
       dep.name AS departmentName
FROM doctor d 
LEFT JOIN department dep ON d.department_id = dep.id 
WHERE d.department_id = ? AND d.status = 1 ORDER BY d.id;

-- 按姓名模糊查询
SELECT d.id, d.name, d.department_id, d.title, d.status,
       dep.name AS departmentName
FROM doctor d 
LEFT JOIN department dep ON d.department_id = dep.id 
WHERE d.name LIKE ? ORDER BY d.department_id, d.id;
```

### 1.3 排班表 (schedule)

```sql
-- 三表 JOIN（排班 + 医生 + 科室）
SELECT s.id, s.doctor_id, s.work_date, s.time_slot, 
       s.total_slots, s.remain_slots, s.create_time, s.update_time,
       d.name AS doctorName, dep.name AS departmentName
FROM schedule s
LEFT JOIN doctor d ON s.doctor_id = d.id
LEFT JOIN department dep ON d.department_id = dep.id
WHERE s.work_date = ? ORDER BY s.doctor_id;

-- 查询可用号源（未来日期且剩余>0）
SELECT s.id, s.doctor_id, s.work_date, s.time_slot,
       s.total_slots, s.remain_slots,
       d.name AS doctorName, dep.name AS departmentName
FROM schedule s
LEFT JOIN doctor d ON s.doctor_id = d.id
LEFT JOIN department dep ON d.department_id = dep.id
WHERE s.work_date >= ? AND s.remain_slots > 0 
ORDER BY s.work_date, s.doctor_id;

-- 扣减号源（WHERE 条件防止超卖）
UPDATE schedule SET remain_slots = remain_slots - 1 
WHERE id = ? AND remain_slots > 0;

-- 释放号源（取消挂号时使用）
UPDATE schedule SET remain_slots = remain_slots + 1 WHERE id = ?;

-- 检查排班是否已存在
SELECT COUNT(*) FROM schedule 
WHERE doctor_id = ? AND work_date = ? AND time_slot = ?;
```

---

## 二、患者管理模块（A 组）

### 2.1 患者表 (patient)

```sql
-- 插入（注意：id_card 有唯一索引）
INSERT INTO patient (id_card, name, gender, birth_date, phone, address, allergy_info) 
VALUES (?, ?, ?, ?, ?, ?, ?);

-- 更新（注意：id_card 唯一性校验）
UPDATE patient SET name = ?, gender = ?, birth_date = ?, phone = ?, 
                   address = ?, allergy_info = ? WHERE id = ?;

-- 删除
DELETE FROM patient WHERE id = ?;

-- 按 ID 查询
SELECT id, id_card, name, gender, birth_date, phone, address, allergy_info, 
       create_time, update_time FROM patient WHERE id = ?;

-- 按身份证号查询
SELECT id, id_card, name, gender, birth_date, phone, address, allergy_info,
       create_time, update_time FROM patient WHERE id_card = ?;

-- 查询全部（按建档时间倒序）
SELECT id, id_card, name, gender, birth_date, phone, address, allergy_info,
       create_time, update_time FROM patient ORDER BY create_time DESC;

-- 按姓名模糊查询
SELECT id, id_card, name, gender, birth_date, phone, address, allergy_info,
       create_time, update_time FROM patient WHERE name LIKE ? ORDER BY create_time DESC;

-- 按手机号模糊查询
SELECT id, id_card, name, gender, birth_date, phone, address, allergy_info,
       create_time, update_time FROM patient WHERE phone LIKE ? ORDER BY create_time DESC;

-- 检查身份证号是否已存在
SELECT COUNT(*) FROM patient WHERE id_card = ?;

-- 检查身份证号是否已被其他患者使用（编辑时）
SELECT COUNT(*) FROM patient WHERE id_card = ? AND id != ?;
```

---

## 三、挂号预约模块（B 组）

### 3.1 挂号表 (registration)

```sql
-- 插入（事务方法，需传入 Connection）
INSERT INTO registration (patient_id, schedule_id, reg_time, status, seq_no, fee) 
VALUES (?, ?, NOW(), ?, ?, ?);

-- 更新状态
UPDATE registration SET status = ? WHERE id = ?;

-- 删除（事务方法）
DELETE FROM registration WHERE id = ?;

-- 多表 JOIN 查询（挂号 + 患者 + 排班 + 医生 + 科室）
SELECT r.id, r.patient_id, r.schedule_id, r.reg_time, r.status, r.seq_no, r.fee,
       r.create_time, r.update_time,
       p.name AS patientName, d.name AS doctorName, 
       dep.name AS departmentName, s.time_slot AS timeSlot
FROM registration r
LEFT JOIN patient p ON r.patient_id = p.id
LEFT JOIN schedule s ON r.schedule_id = s.id
LEFT JOIN doctor d ON s.doctor_id = d.id
LEFT JOIN department dep ON d.department_id = dep.id
WHERE r.id = ?;

-- 查询今日挂号记录
SELECT r.id, r.patient_id, r.schedule_id, r.reg_time, r.status, r.seq_no, r.fee,
       p.name AS patientName, d.name AS doctorName, 
       dep.name AS departmentName, s.time_slot AS timeSlot
FROM registration r
LEFT JOIN patient p ON r.patient_id = p.id
LEFT JOIN schedule s ON r.schedule_id = s.id
LEFT JOIN doctor d ON s.doctor_id = d.id
LEFT JOIN department dep ON d.department_id = dep.id
WHERE DATE(r.reg_time) = ? ORDER BY r.reg_time DESC;

-- 按患者查询历史挂号
SELECT ... FROM registration r 
LEFT JOIN patient p ON ... LEFT JOIN schedule s ON ... 
WHERE r.patient_id = ? ORDER BY r.reg_time DESC;

-- 按状态查询
SELECT ... FROM registration r 
LEFT JOIN patient p ON ... LEFT JOIN schedule s ON ... 
WHERE r.status = ? ORDER BY r.reg_time;

-- 查询某医生今日候诊/就诊中的患者
SELECT ... FROM registration r 
LEFT JOIN patient p ON ... LEFT JOIN schedule s ON ... 
WHERE s.doctor_id = ? AND DATE(s.work_date) = ? AND r.status IN (0, 1)
ORDER BY r.seq_no, r.reg_time;

-- 获取今日最大序号（用于分配 seq_no）
SELECT MAX(seq_no) FROM registration WHERE DATE(reg_time) = ?;
```

### 3.2 挂号事务 SQL 组合

```sql
-- 【挂号事务】扣减号源 + 插入挂号记录
START TRANSACTION;
UPDATE schedule SET remain_slots = remain_slots - 1 WHERE id = ? AND remain_slots > 0;
INSERT INTO registration (patient_id, schedule_id, reg_time, status, seq_no, fee) 
VALUES (?, ?, NOW(), 0, ?, 10.00);
COMMIT;

-- 【取消挂号事务】释放号源 + 删除挂号记录
START TRANSACTION;
UPDATE schedule SET remain_slots = remain_slots + 1 WHERE id = ?;
DELETE FROM registration WHERE id = ?;
COMMIT;
```

---

## 四、医生工作站 + 病历管理模块（C 组）

### 4.1 病历表 (medical_record)

```sql
-- 插入
INSERT INTO medical_record (registration_id, patient_id, doctor_id, 
    chief_complaint, present_illness, past_history, physical_exam, diagnosis, advice) 
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

-- 更新（病历通常不允许修改，提供更新方法用于纠错）
UPDATE medical_record SET chief_complaint = ?, present_illness = ?, 
    past_history = ?, physical_exam = ?, diagnosis = ?, advice = ? WHERE id = ?;

-- 删除
DELETE FROM medical_record WHERE id = ?;

-- JOIN 查询（病历 + 患者 + 医生）
SELECT mr.id, mr.registration_id, mr.patient_id, mr.doctor_id, 
       mr.chief_complaint, mr.present_illness, mr.past_history, 
       mr.physical_exam, mr.diagnosis, mr.advice, 
       mr.create_time, mr.update_time,
       p.name AS patientName, d.name AS doctorName
FROM medical_record mr
LEFT JOIN patient p ON mr.patient_id = p.id
LEFT JOIN doctor d ON mr.doctor_id = d.id
WHERE mr.id = ?;

-- 按患者查询病历历史
SELECT mr.id, mr.registration_id, mr.patient_id, mr.doctor_id,
       mr.chief_complaint, mr.present_illness, mr.past_history,
       mr.physical_exam, mr.diagnosis, mr.advice,
       mr.create_time, mr.update_time,
       p.name AS patientName, d.name AS doctorName
FROM medical_record mr
LEFT JOIN patient p ON mr.patient_id = p.id
LEFT JOIN doctor d ON mr.doctor_id = d.id
WHERE mr.patient_id = ? ORDER BY mr.create_time DESC;

-- 按挂号 ID 查询（用于医生工作站就诊时查看/创建）
SELECT ... FROM medical_record mr 
LEFT JOIN patient p ON ... LEFT JOIN doctor d ON ...
WHERE mr.registration_id = ?;

-- 查询某医生今日病历
SELECT ... FROM medical_record mr 
LEFT JOIN patient p ON ... LEFT JOIN doctor d ON ...
WHERE mr.doctor_id = ? AND DATE(mr.create_time) = CURDATE()
ORDER BY mr.create_time DESC;

-- 全量查询（病历管理模块列表）
SELECT ... FROM medical_record mr 
LEFT JOIN patient p ON ... LEFT JOIN doctor d ON ...
ORDER BY mr.create_time DESC;

-- 模糊搜索（按患者姓名/医生姓名/诊断/主诉）
SELECT ... FROM medical_record mr 
LEFT JOIN patient p ON mr.patient_id = p.id
LEFT JOIN doctor d ON mr.doctor_id = d.id
WHERE (p.name LIKE ? OR d.name LIKE ? OR mr.diagnosis LIKE ? OR mr.chief_complaint LIKE ?)
ORDER BY mr.create_time DESC;
```

---

## 五、药品库存 + 处方管理模块（D 组）

### 5.1 药品表 (medicine)

```sql
-- 插入
INSERT INTO medicine (name, spec, unit, stock, safety_stock, expiry_date, 
    batch_no, pinyin_code, price, manufacturer, status) 
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- 更新
UPDATE medicine SET name = ?, spec = ?, unit = ?, stock = ?, safety_stock = ?, 
    expiry_date = ?, batch_no = ?, pinyin_code = ?, price = ?, manufacturer = ?, status = ? 
WHERE id = ?;

-- 库存变动（事务方法）
UPDATE medicine SET stock = stock + ? WHERE id = ?;

-- 删除
DELETE FROM medicine WHERE id = ?;

-- 按 ID 查询
SELECT id, name, spec, unit, stock, safety_stock, expiry_date, batch_no, 
       pinyin_code, price, manufacturer, status, create_time, update_time 
FROM medicine WHERE id = ?;

-- 查询全部
SELECT ... FROM medicine ORDER BY name;

-- 按名称模糊查询
SELECT ... FROM medicine WHERE name LIKE ? ORDER BY name;

-- 按拼音简码查询
SELECT ... FROM medicine WHERE pinyin_code LIKE ? ORDER BY name;

-- 低库存预警（库存 <= 安全库存）
SELECT ... FROM medicine WHERE stock <= safety_stock AND status = 1 ORDER BY stock;

-- 临期预警（30天内过期）
SELECT ... FROM medicine 
WHERE expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) 
  AND expiry_date >= CURDATE() AND status = 1 ORDER BY expiry_date;
```

### 5.2 库存变动日志表 (inventory_log)

```sql
-- 插入（事务方法）
INSERT INTO inventory_log (medicine_id, type, quantity, batch_no, expiry_date, operator, remark) 
VALUES (?, ?, ?, ?, ?, ?, ?);

-- JOIN 查询（日志 + 药品名称）
SELECT l.id, l.medicine_id, l.type, l.quantity, l.batch_no, l.expiry_date, 
       l.operator, l.remark, l.log_time,
       m.name AS medicineName
FROM inventory_log l
LEFT JOIN medicine m ON l.medicine_id = m.id
ORDER BY l.log_time DESC;

-- 按药品查询日志
SELECT ... FROM inventory_log l
LEFT JOIN medicine m ON l.medicine_id = m.id
WHERE l.medicine_id = ? ORDER BY l.log_time DESC;

-- 按类型查询（入库/出库/盘盈/盘亏）
SELECT ... FROM inventory_log l
LEFT JOIN medicine m ON l.medicine_id = m.id
WHERE l.type = ? ORDER BY l.log_time DESC LIMIT 200;
```

### 5.3 处方表 (prescription)

```sql
-- 插入（事务方法）
INSERT INTO prescription (record_id, patient_id, doctor_id, total_amount, status) 
VALUES (?, ?, ?, ?, ?);

-- 更新状态（缴费/取药/作废）
UPDATE prescription SET status = ? WHERE id = ?;

-- JOIN 查询（处方 + 患者 + 医生）
SELECT p.id, p.record_id, p.patient_id, p.doctor_id, p.total_amount, p.status,
       p.create_time, p.update_time,
       pt.name AS patientName, d.name AS doctorName
FROM prescription p
LEFT JOIN patient pt ON p.patient_id = pt.id
LEFT JOIN doctor d ON p.doctor_id = d.id
WHERE p.id = ?;

-- 按患者查询处方历史
SELECT ... FROM prescription p
LEFT JOIN patient pt ON p.patient_id = pt.id
LEFT JOIN doctor d ON p.doctor_id = d.id
WHERE p.patient_id = ? ORDER BY p.create_time DESC;

-- 按病历 ID 查询
SELECT ... FROM prescription p ... WHERE p.record_id = ?;

-- 查询今日处方
SELECT ... FROM prescription p ... WHERE DATE(p.create_time) = CURDATE() ORDER BY p.create_time DESC;
```

### 5.4 处方明细表 (prescription_item)

```sql
-- 插入（事务方法）
INSERT INTO prescription_item (prescription_id, medicine_id, quantity, dosage, usage_desc, unit_price, amount) 
VALUES (?, ?, ?, ?, ?, ?, ?);

-- 按处方删除全部明细（用于作废处方时回滚）
DELETE FROM prescription_item WHERE prescription_id = ?;

-- JOIN 查询（明细 + 药品信息）
SELECT pi.id, pi.prescription_id, pi.medicine_id, pi.quantity, pi.dosage, 
       pi.usage_desc, pi.unit_price, pi.amount, pi.create_time,
       m.name AS medicineName, m.spec AS medicineSpec, m.unit AS medicineUnit
FROM prescription_item pi
LEFT JOIN medicine m ON pi.medicine_id = m.id
WHERE pi.prescription_id = ?;
```

### 5.5 处方事务 SQL 组合

```sql
-- 【开立处方事务】插入处方 + 插入明细 + 扣减库存
START TRANSACTION;
INSERT INTO prescription (record_id, patient_id, doctor_id, total_amount, status) 
VALUES (?, ?, ?, ?, 0);
-- 获取生成的主键 prescription_id
INSERT INTO prescription_item (prescription_id, medicine_id, quantity, dosage, usage_desc, unit_price, amount) 
VALUES (?, ?, ?, ?, ?, ?, ?);
UPDATE medicine SET stock = stock - ? WHERE id = ? AND stock >= ?;
COMMIT;

-- 【作废处方事务】更新处方状态 + 恢复库存 + 删除明细
START TRANSACTION;
UPDATE prescription SET status = 3 WHERE id = ?;
UPDATE medicine SET stock = stock + ? WHERE id = ?;
DELETE FROM prescription_item WHERE prescription_id = ?;
COMMIT;
```

---

## 六、系统用户模块（参考实现）

```sql
-- 按用户名查询（登录验证）
SELECT id, username, password, real_name, role, status, doctor_id, create_time, update_time 
FROM sys_user WHERE username = ? AND status = 1;

-- 查询全部管理员
SELECT ... FROM sys_user WHERE role = 'admin' ORDER BY id;

-- 修改密码
UPDATE sys_user SET password = ? WHERE id = ?;

-- 统计管理员数量
SELECT COUNT(*) FROM sys_user WHERE role = 'admin' AND status = 1;
```

---

## 七、SQL 编写规范 checklist

- [ ] 所有 SQL 定义为常量，禁止字符串拼接
- [ ] SELECT 必须列出具体字段，禁止 `SELECT *`
- [ ] 需要显示关联表名称时，使用 `LEFT JOIN` + `AS` 别名
- [ ] 别名使用驼峰命名（如 `patientName`、`doctorName`），与实体类属性对应
- [ ] 事务中的 SQL 使用传入 `Connection` 的重载方法
- [ ] `INT UNSIGNED` 字段用 `Number` 接收后转换，避免 `ClassCastException`
- [ ] 模糊查询参数使用 `"%" + keyword + "%"` 拼接
