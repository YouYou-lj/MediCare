# 任务书 C：医生工作站 + 病历管理模块

> **负责小组**：C 组（3-5 人）  
> **依赖模块**：B 组（挂号预约）+ 老师演示（基础数据）  
> **技术要点**：状态机、表单填写、历史记录查看、病历详情弹窗

---

## 一、功能需求

### 1.1 医生工作站

#### 医生选择
- [ ] 登录后或进入模块时选择当前医生（ComboBox）
- [ ] 医生列表来自老师演示模块

#### 候诊队列
- [ ] 显示当前医生今日的候诊患者列表
- [ ] 列表显示：序号、患者姓名、挂号时间
- [ ] 点击【叫号】按钮：状态变为"就诊中"，患者进入就诊区

#### 病历书写
- [ ] 就诊区显示当前就诊患者信息
- [ ] 表单字段：主诉、现病史、既往史、体格检查、诊断、医嘱
- [ ] 点击【保存病历】：创建病历记录
- [ ] 保存后自动完成挂号（状态变为"已完成"）

#### 历史病历
- [ ] 显示当前患者的历史病历列表
- [ ] 点击可查看详情

### 1.2 病历管理（独立模块）

#### 病历列表
- [ ] 全宽表格显示所有病历（按创建时间倒序）
- [ ] 列：ID、患者姓名、医生姓名、主诉、诊断、创建时间
- [ ] 搜索：按患者姓名/医生姓名/诊断/主诉模糊搜索

#### 病历详情
- [ ] 点击行内【查看】按钮打开详情弹窗（只读）
- [ ] 显示完整病历信息

#### 删除病历
- [ ] 点击【删除】按钮，确认后删除

---

## 二、数据库表

### 2.1 病历表 (medical_record)

```sql
CREATE TABLE medical_record (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    registration_id BIGINT UNSIGNED NOT NULL,
    patient_id      BIGINT UNSIGNED NOT NULL,
    doctor_id       BIGINT UNSIGNED NOT NULL,
    chief_complaint VARCHAR(500) COMMENT '主诉',
    present_illness TEXT COMMENT '现病史',
    past_history    VARCHAR(1000) COMMENT '既往史',
    physical_exam   VARCHAR(1000) COMMENT '体格检查',
    diagnosis       VARCHAR(500) COMMENT '诊断',
    advice          VARCHAR(1000) COMMENT '医嘱',
    create_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_mr_registration FOREIGN KEY (registration_id) REFERENCES registration(id),
    CONSTRAINT fk_mr_patient      FOREIGN KEY (patient_id)      REFERENCES patient(id),
    CONSTRAINT fk_mr_doctor       FOREIGN KEY (doctor_id)       REFERENCES doctor(id)
);
```

### 2.2 依赖表

- `registration`：获取候诊患者（B 组提供接口）
- `patient`：患者信息（A 组提供）
- `doctor`：医生信息（老师演示模块）

---

## 三、分层设计任务

### 3.1 DAO 层

#### MedicalRecordDAO.java

创建 `MedicalRecordDAO.java`，继承 `BaseDAO<MedicalRecord>`。

**必须实现的方法**：

| 方法 | 说明 |
|------|------|
| `Long insert(MedicalRecord)` | 新增病历 |
| `int update(MedicalRecord)` | 更新病历（纠错用） |
| `int delete(Long id)` | 删除病历 |
| `MedicalRecord findById(Long id)` | 按 ID 查询（JOIN 患者+医生） |
| `MedicalRecord findByRegistration(Long regId)` | 按挂号 ID 查询 |
| `List<MedicalRecord> findByPatient(Long patientId)` | 按患者查询历史 |
| `List<MedicalRecord> findByDoctorToday(Long doctorId)` | 某医生今日病历 |
| `List<MedicalRecord> findAll()` | 全量查询 |
| `List<MedicalRecord> search(String keyword)` | 四字段模糊搜索 |

**所有 SELECT 必须 JOIN 患者表和医生表获取名称**：
```sql
SELECT mr.id, mr.registration_id, mr.patient_id, mr.doctor_id, 
       mr.chief_complaint, mr.present_illness, mr.past_history,
       mr.physical_exam, mr.diagnosis, mr.advice,
       mr.create_time, mr.update_time,
       p.name AS patientName, d.name AS doctorName
FROM medical_record mr
LEFT JOIN patient p ON mr.patient_id = p.id
LEFT JOIN doctor d ON mr.doctor_id = d.id
WHERE ...
```

### 3.2 Service 层

创建 `MedicalRecordService.java`。

| 方法 | 说明 |
|------|------|
| `Long saveRecord(MedicalRecord)` | 保存病历（同时完成挂号状态变更） |
| `void updateRecord(MedicalRecord)` | 更新病历 |
| `void deleteRecord(Long id)` | 删除病历 |
| `MedicalRecord getById(Long id)` | 按 ID 查询 |
| `MedicalRecord getByRegistration(Long regId)` | 按挂号查询 |
| `List<MedicalRecord> listByPatient(Long patientId)` | 患者历史 |
| `List<MedicalRecord> listAll()` | 全量列表 |
| `List<MedicalRecord> search(String keyword)` | 搜索 |

**saveRecord 的业务逻辑**：
1. 校验必填字段（主诉、诊断至少不为空）
2. 插入病历记录
3. 调用 B 组接口完成挂号：`registrationService.completeRegistration(regId)`

### 3.3 Controller 层

#### DoctorWorkstationController.java
- 使用 `split-pane-template.fxml` 布局
- 左侧：候诊队列（Registration 列表）
- 右侧：病历书写表单 + 历史病历
- 顶部：医生选择 ComboBox
- 叫号按钮：调用 B 组 `RegistrationService.callPatient()`
- 保存病历按钮：
  1. 构建 MedicalRecord
  2. 调用 `MedicalRecordService.saveRecord()`
  3. 刷新候诊队列和历史病历

#### MedicalRecordController.java
- 使用 `list-view-template.fxml` 布局
- 表格列：patientName、doctorName、chiefComplaint、diagnosis、createTime
- 搜索框：四字段模糊搜索
- 操作列：查看详情 + 删除

#### MedicalRecordDetailDialogController.java
- 只读弹窗，显示完整病历信息
- 使用 `DialogPane` 布局

### 3.4 FXML 设计

| 文件 | 布局 | 说明 |
|------|------|------|
| `DoctorWorkstationView.fxml` | SplitPane | 左候诊队列，右病历表单 |
| `MedicalRecordView.fxml` | list-view-template | 全宽病历列表 |
| `MedicalRecordDetailDialog.fxml` | DialogPane | 只读详情弹窗 |

---

## 四、与其他模块的接口约定

### 4.1 依赖 B 组（挂号预约）

```java
// B 组必须提供的方法
RegistrationService.findWaitingByDoctor(Long doctorId, LocalDate date)
    // 返回 List<Registration>，含 patientName, doctorName, timeSlot 等 JOIN 字段
RegistrationService.callPatient(Long regId)
RegistrationService.completeRegistration(Long regId)
```

### 4.2 依赖老师演示（基础数据）

```java
DoctorService.findAll()  // 医生选择下拉框
```

### 4.3 提供给 D 组（处方管理）

```java
// C 组需要提供的方法
MedicalRecordService.getById(Long id)
    // D 组开立处方时需要关联病历
MedicalRecordService.listByPatient(Long patientId)
    // D 组查看患者历史病历
```

---

## 五、Git 分支

```bash
git checkout -b feature/doctor-workstation
git commit -m "feat: 完成医生工作站 + 病历管理模块"
git push origin feature/doctor-workstation
```

---

## 六、验收标准

### 功能验收
- [ ] 医生选择后正确加载候诊队列
- [ ] 叫号后患者状态变为"就诊中"
- [ ] 病历表单能正确填写和保存
- [ ] 保存病历后挂号状态自动变为"已完成"
- [ ] 历史病历列表正确显示
- [ ] 病历管理模块能全量显示和搜索
- [ ] 病历详情弹窗只读显示完整信息
- [ ] 删除病历后列表刷新

### 代码规范
- [ ] 所有病历查询 SQL 包含 JOIN 获取患者名和医生名
- [ ] 保存病历与完成挂号的状态变更正确联动
- [ ] 后台线程 + Platform.runLater 模式
