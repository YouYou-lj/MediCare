# 挂号预约与排班模块

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 挂号预约
> **前端页面**: `/registration` (RegistrationView.vue)
> **后端 Controller**: `RegistrationController`, `ScheduleController`
> **角色权限**: admin

---

## 1. 模块概述

挂号预约模块是门诊业务的**入口模块**，负责管理号源、办理患者挂号、跟踪挂号状态流转。模块核心依赖患者管理模块（选择患者）和基础数据模块（科室/医生/排班）。

---

## 2. 功能清单

| 功能 | 说明 | 角色 |
|------|------|------|
| 号源查询 | 按日期 + 科室筛选可用号源 | admin |
| 挂号 | 选择号源 → 选择患者 → 确认挂号 | admin |
| 今日挂号记录 | 右侧显示今日所有挂号记录 | admin |
| 状态筛选 | 全部/候诊/就诊中/已完成 | admin |
| 叫号 | 选中候诊记录，状态变为"就诊中" | admin |
| 完成就诊 | 选中就诊中记录，状态变为"已完成" | admin |
| 取消挂号 | 释放号源 + 删除记录（事务） | admin |
| 排班管理 | 基础数据页面维护医生排班 | admin/doctor |

---

## 3. 挂号状态流转

```
┌─────────┐    叫号     ┌─────────┐    完成就诊   ┌─────────┐
│  候诊   │ ──────────► │ 就诊中  │ ───────────► │ 已完成  │
│  (0)   │              │  (1)   │              │  (2)   │
└─────────┘              └─────────┘              └─────────┘
     │
     │ 取消挂号
     ▼
┌─────────┐
│ 已取消  │
│  (3)   │
└─────────┘
```

- **候诊 (0)**: 刚挂号成功，等待叫号
- **就诊中 (1)**: 叫号后，患者进入诊室
- **已完成 (2)**: 医生完成病历书写后自动完成
- **已取消 (3)**: 取消挂号，释放号源

---

## 4. 前端实现 (RegistrationView.vue)

### 4.1 页面布局

采用 SplitPane 左右分割布局：
- **左侧**: 号源查询 + 号源列表
- **右侧**: 今日挂号记录列表 + 操作按钮

### 4.2 号源查询

筛选条件:
- 日期选择器（默认今日）
- 科室下拉框（来自 Department 表）

号源列表字段:
| 字段 | 说明 |
|------|------|
| 医生 | 医生姓名 |
| 科室 | 科室名称 |
| 时段 | 上午/下午 |
| 总号源 | total_slots |
| 剩余号源 | remain_slots（0 时灰色/不可选） |

### 4.3 挂号流程

1. 左侧选择号源（`remain_slots > 0`）
2. 点击【挂号】按钮
3. 弹出 `PatientSelectDialog`（患者选择弹窗）
   - 支持搜索患者
   - 支持选择已有患者
   - 支持【新增患者】（调用 PatientDialog 弹窗）
4. 选中患者后点击【确定】
5. 系统:
   - 扣减号源剩余数（`remain_slots - 1`）
   - 生成挂号记录（status=0, 分配排班内序号 `max(seq_no) + 1`）
   - 自动生成固定挂号编号 `REG-000001`（基于自增 id）
   - 挂号费固定 10.00 元
6. 刷新号源列表和挂号记录列表

> 序号规则：按排班维度从 1 开始递增；取消挂号后序号不回收，新挂号继续从当前最大序号往后编排，保证与导入/历史数据衔接。

### 4.4 挂号记录列表

| 字段 | 说明 |
|------|------|
| 序号 | seq_no（按排班从 1 递增，固定不变） |
| 患者姓名 | patient.name |
| 医生 | doctor.name |
| 科室 | department.name |
| 时段 | time_slot |
| 状态 | StatusTag 组件（带颜色） |
| 挂号时间 | reg_time |

状态颜色:
- 候诊: 黄色
- 就诊中: 蓝色加粗
- 已完成: 绿色
- 已取消: 灰色

### 4.5 操作按钮

- 【叫号】: 选中候诊记录 → 状态变为"就诊中"
- 【完成】: 选中就诊中记录 → 状态变为"已完成"
- 【取消】: 选中记录 → 确认 → 释放号源 + 删除记录

---

## 5. 后端实现

### 5.1 RegistrationController

```java
@RestController
@RequestMapping("/api/registrations")
@Tag(name = "挂号预约", description = "挂号预约相关接口")
public class RegistrationController {

    @GetMapping
    @RequireRole("admin")
    public Result<List<RegistrationVO>> list()          // 查询今日挂号列表

    @GetMapping("/waiting")
    @RequireRole({"admin", "doctor"})
    public Result<List<RegistrationVO>> waitingList()  // 查询候诊/就诊中列表

    @PostMapping
    @RequireRole("admin")
    public Result<RegistrationVO> register(@RequestBody RegistrationVO vo)  // 挂号

    @PostMapping("/{id}/call")
    @RequireRole({"admin", "doctor"})
    public Result<Void> callPatient(@PathVariable Long id)     // 叫号

    @PostMapping("/{id}/complete")
    @RequireRole({"admin", "doctor"})
    public Result<Void> complete(@PathVariable Long id)          // 完成就诊

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> cancel(@PathVariable Long id)           // 取消挂号
}
```

### 5.2 核心 Service 逻辑 (RegistrationService)

#### 挂号事务（核心）

```java
@Transactional
public Registration register(RegistrationVO vo) {
    // 1. 校验号源
    Schedule schedule = scheduleRepository.findById(vo.getScheduleId())
        .orElseThrow(() -> new BusinessException("号源不存在"));
    if (schedule.getRemainSlots() <= 0) {
        throw new BusinessException("号源不足");
    }

    // 2. 扣减号源（乐观锁）
    int affected = scheduleRepository.decrementRemain(schedule.getId());
    if (affected == 0) {
        throw new BusinessException("号源不足，请刷新后重试");
    }

    // 3. 分配序号：取该排班当前最大序号 + 1，保证连续不重复，并与历史/导入数据衔接
    int seqNo = registrationRepository.findMaxSeqNoByScheduleId(scheduleId) + 1;

    // 4. 创建挂号记录（先 insert 拿自增 id，再回填 REG-000001 编号）
    Registration reg = new Registration();
    reg.setPatientId(vo.getPatientId());
    reg.setScheduleId(vo.getScheduleId());
    reg.setDoctorId(schedule.getDoctorId());
    reg.setStatus(Registration.STATUS_WAITING);
    reg.setSeqNo(seqNo);
    reg.setFee(new BigDecimal("10.00"));
    reg = registrationRepository.save(reg);
    reg.setCode(CodeUtils.generateCode("REG", reg.getId()));
    return registrationRepository.save(reg);
}
```

#### 取消挂号事务

```java
@Transactional
public void cancel(Long id) {
    Registration reg = registrationRepository.findById(id)
        .orElseThrow(() -> new BusinessException("挂号记录不存在"));

    // 1. 释放号源
    scheduleRepository.incrementRemain(reg.getScheduleId());

    // 2. 标记挂号为已取消（不删除记录，序号保留不回收）
    reg.setStatus(Registration.STATUS_CANCELLED);
    registrationRepository.save(reg);
}
```

---

## 6. 排班管理 (Schedule)

### 6.1 数据模型

```java
@Entity
@Table(name = "schedule")
public class Schedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;           // 医生ID

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;      // 工作日期

    @Column(name = "time_slot", nullable = false, length = 20)
    private String timeSlot;         // 时段（如"上午"、"下午"）

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots = 0;  // 总号源

    @Column(name = "remain_slots", nullable = false)
    private Integer remainSlots = 0; // 剩余号源
}
```

### 6.2 ScheduleController

```java
@RestController
@RequestMapping("/api/schedules")
@Tag(name = "排班管理", description = "医生排班号源管理")
public class ScheduleController {

    @GetMapping("/available")
    @RequireRole({"admin", "doctor"})
    public Result<List<ScheduleVO>> available(
        @RequestParam LocalDate date,
        @RequestParam(required = false) Long departmentId
    )   // 查询指定日期+科室的可用号源

    @PostMapping
    @RequireRole("admin")
    public Result<ScheduleVO> create(@RequestBody ScheduleVO vo)   // 新增排班

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<ScheduleVO> update(@PathVariable Long id, @RequestBody ScheduleVO vo)  // 更新排班

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id)   // 删除排班
}
```

---

## 7. 数据库表结构

### 7.1 registration (挂号表)

```sql
CREATE TABLE registration (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(32) COMMENT '挂号编号（固定，格式 REG-000001）',
    patient_id  BIGINT UNSIGNED NOT NULL,
    schedule_id BIGINT UNSIGNED NOT NULL,
    doctor_id   BIGINT UNSIGNED,          -- V2 新增
    reg_time    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    status      TINYINT DEFAULT 0 COMMENT '0-候诊 1-就诊中 2-已完成 3-已取消',
    seq_no      INT UNSIGNED NOT NULL COMMENT '序号（按排班从 1 递增）',
    fee         DECIMAL(10,2) DEFAULT 0.00,
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_reg_code (code),
    UNIQUE KEY uk_reg_schedule_seq (schedule_id, seq_no),
    CONSTRAINT fk_reg_patient  FOREIGN KEY (patient_id)  REFERENCES patient(id),
    CONSTRAINT fk_reg_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id),
    CONSTRAINT fk_reg_doctor   FOREIGN KEY (doctor_id)   REFERENCES doctor(id)
);
```

### 7.2 schedule (排班表)

```sql
CREATE TABLE schedule (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    doctor_id    BIGINT UNSIGNED NOT NULL,
    work_date    DATE NOT NULL,
    time_slot    VARCHAR(20) NOT NULL,
    total_slots  INT UNSIGNED DEFAULT 0,
    remain_slots INT UNSIGNED DEFAULT 0,
    create_time  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);
```

---

## 8. 事务安全要点

### 8.1 挂号事务

- 扣减号源 + 插入挂号记录必须在**同一事务**中执行
- 使用乐观锁（`WHERE remain_slots > 0`）防止并发超卖
- 号源不足时抛出明确异常，事务回滚

### 8.2 取消挂号事务

- 释放号源（+1）+ 删除记录必须在**同一事务**中执行
- 确保号源数正确恢复

---

## 9. 与其他模块的接口

### 9.1 提供给医生工作站

```java
RegistrationService.findWaitingByDoctor(Long doctorId, LocalDate date)
    // 返回某医生今日候诊和就诊中的挂号记录
RegistrationService.callPatient(Long regId)
    // 叫号，状态变为"就诊中"
RegistrationService.completeRegistration(Long regId)
    // 完成就诊，状态变为"已完成"
```

### 9.2 依赖患者管理模块

```java
PatientService.findById(patientId)     // 挂号时选择患者
PatientService.search(keyword)          // 患者搜索弹窗
```

### 9.3 依赖基础数据模块

```java
DepartmentService.findAll()             // 科室下拉列表
DoctorService.findAll()                 // 医生列表
ScheduleService.findByDate(date)        // 按日期查询号源
```
