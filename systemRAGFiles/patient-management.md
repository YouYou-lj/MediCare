# 患者管理模块

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 患者管理
> **前端页面**: `/patients` (PatientList.vue)
> **后端 Controller**: `PatientController`
> **角色权限**: admin, doctor

---

## 1. 模块概述

患者管理是系统的**核心业务基础模块**，为挂号预约、医生工作站、病历管理、处方管理等下游模块提供患者数据。所有医疗行为都以患者档案为前提。

---

## 2. 功能清单

| 功能 | 说明 | 角色 |
|------|------|------|
| 患者列表 | 全宽表格展示所有患者，按建档时间倒序 | admin, doctor |
| 新增患者 | 弹窗表单录入患者档案 | admin, doctor |
| 编辑患者 | 行内编辑按钮，弹窗预填充数据 | admin, doctor |
| 删除患者 | 行内删除按钮，确认后删除 | admin |
| 删除含关联 | 清理患者关联业务数据（挂号/病历/处方）后删除 | admin |
| 搜索患者 | 按姓名/身份证号/手机号模糊搜索 | admin, doctor |
| 查看详情 | 按 ID 查询患者详细信息 | admin, doctor |

---

## 3. 前端实现 (PatientList.vue)

### 3.1 页面布局

- 顶部: `DataToolbar` 组件（搜索框 + 刷新按钮 + 新增按钮）
- 中部: `el-table` 患者表格
- 操作列: 编辑按钮 + 删除按钮
- 底部: 分页器（如数据量大）

### 3.2 表格字段

| 字段 | 对应属性 | 格式化 |
|------|----------|--------|
| ID | `id` | 原值 |
| 姓名 | `name` | 原值 |
| 性别 | `gender` | `0 → 女`, `1 → 男` |
| 身份证号 | `idCard` | 原值 |
| 手机号 | `phone` | 原值 |
| 住址 | `address` | 原值 |
| 建档时间 | `createTime` | `yyyy-MM-dd HH:mm` |
| 操作 | — | 编辑 / 删除 |

### 3.3 新增/编辑弹窗

表单字段:
- 身份证号*（必填，18位校验）
- 姓名*（必填）
- 性别（单选：女/男）
- 出生日期（日期选择器）
- 手机号（1开头11位校验）
- 地址（文本输入）
- 过敏信息（文本域）

### 3.4 前端 API 调用

```typescript
// api/patient.ts
import request from './index'

export const getPatientList = () => request.get('/patients')
export const searchPatients = (keyword: string) => request.get('/patients/search', { params: { keyword } })
export const getPatientById = (id: number) => request.get(`/patients/${id}`)
export const createPatient = (data: any) => request.post('/patients', data)
export const updatePatient = (id: number, data: any) => request.put(`/patients/${id}`, data)
export const deletePatient = (id: number) => request.delete(`/patients/${id}`)
export const deletePatientWithRelated = (id: number) => request.delete(`/patients/${id}/with-related`)
```

---

## 4. 后端实现 (PatientController)

### 4.1 API 端点

```java
@RestController
@RequestMapping("/api/patients")
@Tag(name = "患者管理", description = "患者管理相关接口")
public class PatientController {

    @GetMapping
    @RequireRole({"admin", "doctor"})
    @Operation(summary = "查询患者列表")
    public Result<List<Patient>> list()

    @GetMapping("/search")
    @RequireRole({"admin", "doctor"})
    @Operation(summary = "搜索患者")
    public Result<List<Patient>> search(@RequestParam String keyword)

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    @Operation(summary = "根据ID查询患者详情")
    public Result<Patient> detail(@PathVariable Long id)

    @PostMapping
    @RequireRole({"admin", "doctor"})
    @Operation(summary = "新增患者")
    public Result<Patient> create(@Valid @RequestBody Patient patient)

    @PutMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    @Operation(summary = "更新患者")
    public Result<Patient> update(@PathVariable Long id, @Valid @RequestBody Patient patient)

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    @Operation(summary = "删除患者")
    public Result<Void> delete(@PathVariable Long id)

    @DeleteMapping("/{id}/with-related")
    @RequireRole("admin")
    @Operation(summary = "清理关联业务数据并删除患者")
    public Result<Void> deleteWithRelated(@PathVariable Long id)
}
```

### 4.2 业务规则 (PatientService)

- **身份证号唯一性校验**: 新增时检查 `id_card` 是否已存在
- **编辑排除自身校验**: 编辑时检查身份证号是否被其他患者使用（排除当前 ID）
- **格式校验**:
  - 身份证号: 18 位（前端+后端双重校验）
  - 手机号: 1 开头 11 位（如有输入）
- **删除限制**: 删除患者前需确认是否有关联挂号记录；`deleteWithRelated` 会先清理关联数据

---

## 5. 数据模型 (Patient 实体)

```java
@Entity
@Table(name = "patient")
public class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_card", nullable = false, length = 18)
    private String idCard;           // 身份证号（唯一）

    @Column(nullable = false, length = 50)
    private String name;             // 姓名

    @Column(nullable = false)
    private Integer gender;          // 0=女, 1=男, 2=其他

    @Column(name = "birth_date")
    private LocalDate birthDate;     // 出生日期

    @Column(length = 20)
    private String phone;            // 手机号

    @Column(length = 200)
    private String address;          // 住址

    @Column(name = "allergy_info", length = 500)
    private String allergyInfo;      // 过敏信息

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;  // 建档时间

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;  // 更新时间
}
```

---

## 6. 数据库表结构

```sql
CREATE TABLE patient (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_card      VARCHAR(18) NOT NULL,
    name         VARCHAR(50) NOT NULL,
    gender       TINYINT NOT NULL COMMENT '0-女 1-男 2-其他',
    birth_date   DATE,
    phone        VARCHAR(20),
    address      VARCHAR(200),
    allergy_info VARCHAR(500),
    create_time  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_patient_id_card (id_card)
);
```

---

## 7. 与其他模块的接口

### 7.1 被挂号预约模块依赖

```java
// 挂号时需要选择患者
PatientService.findById(Long id)          // 按 ID 查询患者
PatientService.search(String keyword)     // 搜索患者（PatientSelectDialog）
PatientService.findAll()                  // 加载全部患者列表
```

### 7.2 被医生工作站模块依赖

```java
// 病历中显示患者信息
Patient patient = patientRepository.findById(patientId)
```

### 7.3 被处方模块依赖

```java
// 处方中关联患者
Prescription.setPatientId(patientId)
```

---

## 8. 常见问题

### Q: 新增患者时提示"身份证号已存在"？

A: 系统中身份证号全局唯一，请检查是否已录入该患者。如需修改，请使用编辑功能。

### Q: 删除患者失败？

A: 如果该患者有关联的挂号记录，直接删除会失败。admin 用户可使用"删除含关联"功能，系统会先清理关联业务数据再删除患者。

### Q: 搜索没有结果？

A: 搜索支持按姓名、身份证号、手机号模糊匹配。请检查输入是否正确，或尝试使用部分关键字。
