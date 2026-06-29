# 医生工作站与病历管理模块

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 医生工作站与病历
> **前端页面**: `/workstation` (WorkstationView.vue), `/medical-records` (RecordList.vue)
> **后端 Controller**: `DoctorController`, `MedicalRecordController`
> **角色权限**: admin, doctor

---

## 1. 模块概述

医生工作站是门诊业务的**核心诊疗模块**，支持医生查看候诊队列、叫号接诊、书写病历。病历管理模块则是病历的独立查询与管理入口，支持全量搜索和详情查看。

---

## 2. 功能清单

### 2.1 医生工作站

| 功能 | 说明 | 角色 |
|------|------|------|
| 医生选择 | 顶部选择当前医生 | admin, doctor |
| 候诊队列 | 显示当前医生今日候诊患者 | admin, doctor |
| 叫号 | 选中候诊患者，开始接诊 | admin, doctor |
| 病历书写 | 填写主诉、现病史、既往史、体格检查、诊断、医嘱 | admin, doctor |
| 保存病历 | 保存病历 + 自动完成挂号 | admin, doctor |
| 历史病历 | 查看当前患者的历史就诊记录 | admin, doctor |

### 2.2 病历管理

| 功能 | 说明 | 角色 |
|------|------|------|
| 病历列表 | 全量表格展示所有病历 | admin, doctor |
| 病历搜索 | 按患者姓名/医生姓名/诊断/主诉搜索 | admin, doctor |
| 病历详情 | 只读弹窗查看完整病历 | admin, doctor |
| 删除病历 | 删除病历记录 | admin, doctor |

---

## 3. 医生工作站页面 (WorkstationView.vue)

### 3.1 页面布局

采用 SplitPane 左右分割布局：
- **左侧**: 候诊队列 + 历史病历
- **右侧**: 病历书写表单

### 3.2 候诊队列

显示当前医生今日的候诊患者：

| 字段 | 说明 |
|------|------|
| 序号 | 当日挂号序号 |
| 患者姓名 | patient.name |
| 挂号时间 | registration.reg_time |
| 状态 | StatusTag（候诊/就诊中） |
| 操作 | 【叫号】按钮 |

### 3.3 叫号流程

1. 医生选择当前医生（ComboBox）
2. 左侧加载该医生今日候诊队列
3. 选中候诊患者，点击【叫号】
4. 系统：
   - 更新挂号状态为"就诊中" (1)
   - 患者信息展示在右侧就诊区
5. 医生填写病历表单

### 3.4 病历书写表单

表单字段：
- 主诉（VARCHAR 500）
- 现病史（TEXT）
- 既往史（VARCHAR 1000）
- 体格检查（VARCHAR 1000）
- 诊断（VARCHAR 500）
- 医嘱（VARCHAR 1000）

### 3.5 保存病历流程

1. 点击【保存病历】
2. 系统：
   - 校验必填字段（主诉、诊断至少不为空）
   - 插入病历记录（`medical_record` 表）
   - 调用 `RegistrationService.completeRegistration(regId)` 完成挂号
3. 刷新候诊队列和历史病历

---

## 4. 病历管理页面 (RecordList.vue)

### 4.1 页面布局

- 顶部: `DataToolbar`（搜索框 + 刷新）
- 中部: `el-table` 病历列表
- 操作列: 查看详情 + 删除

### 4.2 表格字段

| 字段 | 说明 |
|------|------|
| ID | 病历ID |
| 患者姓名 | patient.name |
| 医生姓名 | doctor.name |
| 主诉 | chief_complaint（截断显示） |
| 诊断 | diagnosis（截断显示） |
| 创建时间 | create_time |
| 操作 | 查看 / 删除 |

### 4.3 病历详情弹窗

只读弹窗，显示完整病历信息：
- 患者基本信息
- 主诉、现病史、既往史、体格检查、诊断、医嘱
- 创建/更新时间

---

## 5. 后端实现

### 5.1 DoctorController（候诊队列）

```java
@RestController
@RequestMapping("/api/doctors")
@Tag(name = "医生管理", description = "医生管理与候诊队列")
public class DoctorController {

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<DoctorVO>> list()     // 查询所有医生

    @GetMapping("/{id}/waiting-queue")
    @RequireRole({"admin", "doctor"})
    public Result<List<RegistrationVO>> waitingQueue(@PathVariable Long id)
        // 查询某医生今日候诊/就诊中队列

    @GetMapping("/{id}/today-patients")
    @RequireRole({"admin", "doctor"})
    public Result<List<Patient>> todayPatients(@PathVariable Long id)
        // 查询某医生今日接诊患者
}
```

### 5.2 MedicalRecordController

```java
@RestController
@RequestMapping("/api/medical-records")
@Tag(name = "病历管理", description = "病历管理相关接口")
public class MedicalRecordController {

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<MedicalRecordVO>> list()          // 查询全部病历

    @GetMapping("/search")
    @RequireRole({"admin", "doctor"})
    public Result<List<MedicalRecordVO>> search(@RequestParam String keyword)
        // 按患者/医生/诊断/主诉搜索

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<MedicalRecordVO> detail(@PathVariable Long id)   // 查看详情

    @PostMapping
    @RequireRole({"admin", "doctor"})
    public Result<MedicalRecordVO> create(@RequestBody MedicalRecordVO vo)
        // 保存病历（同时完成挂号）

    @PutMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<MedicalRecordVO> update(@PathVariable Long id, @RequestBody MedicalRecordVO vo)
        // 更新病历（纠错用）

    @DeleteMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<Void> delete(@PathVariable Long id)     // 删除病历
}
```

### 5.3 核心 Service 逻辑

#### 保存病历（MedicalRecordService）

```java
@Transactional
public MedicalRecord saveRecord(MedicalRecordVO vo) {
    // 1. 校验必填字段
    if (StringUtils.isBlank(vo.getChiefComplaint())) {
        throw new BusinessException("主诉不能为空");
    }
    if (StringUtils.isBlank(vo.getDiagnosis())) {
        throw new BusinessException("诊断不能为空");
    }

    // 2. 构建病历实体
    MedicalRecord record = new MedicalRecord();
    record.setRegistrationId(vo.getRegistrationId());
    record.setPatientId(vo.getPatientId());
    record.setDoctorId(vo.getDoctorId());
    record.setChiefComplaint(vo.getChiefComplaint());
    record.setPresentIllness(vo.getPresentIllness());
    record.setPastHistory(vo.getPastHistory());
    record.setPhysicalExam(vo.getPhysicalExam());
    record.setDiagnosis(vo.getDiagnosis());
    record.setAdvice(vo.getAdvice());

    // 3. 保存病历
    medicalRecordRepository.save(record);

    // 4. 完成挂号（状态变为"已完成"）
    registrationService.completeRegistration(vo.getRegistrationId());

    return record;
}
```

#### 查询病历（含 JOIN）

所有病历查询必须 JOIN 患者表和医生表获取名称：

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

---

## 6. 数据模型 (MedicalRecord 实体)

```java
@Entity
@Table(name = "medical_record")
public class MedicalRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;     // 挂号ID

    @Column(name = "patient_id", nullable = false)
    private Long patientId;          // 患者ID

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;           // 医生ID

    @Column(name = "chief_complaint", length = 500)
    private String chiefComplaint;   // 主诉

    @Column(name = "present_illness", columnDefinition = "TEXT")
    private String presentIllness;   // 现病史

    @Column(name = "past_history", length = 1000)
    private String pastHistory;      // 既往史

    @Column(name = "physical_exam", length = 1000)
    private String physicalExam;     // 体格检查

    @Column(length = 500)
    private String diagnosis;        // 诊断

    @Column(length = 1000)
    private String advice;           // 医嘱

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
```

---

## 7. 数据库表结构

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

---

## 8. 与其他模块的接口

### 8.1 依赖挂号预约模块

```java
RegistrationService.findWaitingByDoctor(Long doctorId, LocalDate date)
    // 加载候诊队列
RegistrationService.callPatient(Long regId)
    // 叫号
RegistrationService.completeRegistration(Long regId)
    // 完成就诊（保存病历时自动调用）
```

### 8.2 提供给处方管理模块

```java
MedicalRecordService.getById(Long id)
    // 开立处方时关联病历
MedicalRecordService.listByPatient(Long patientId)
    // 查看患者历史病历
```

### 8.3 依赖患者管理模块

```java
PatientService.findById(patientId)
    // 显示患者信息
```

---

## 9. 常见问题

### Q: 保存病历时提示"挂号记录不存在"？

A: 请确认该挂号记录状态正确，且与当前医生匹配。只有已叫号（就诊中）的挂号才能保存病历。

### Q: 病历保存后挂号状态没有变化？

A: 保存病历操作会自动调用 `completeRegistration` 将挂号状态改为"已完成"。如果状态未变化，请检查事务是否正常提交。

### Q: 历史病历为空？

A: 该患者可能是首次就诊，或之前的病历记录已被删除。
