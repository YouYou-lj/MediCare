# 仪表盘与基础数据模块

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 仪表盘与基础数据
> **前端页面**: `/dashboard` (DashboardView.vue), `/basic-data` (BasicDataView.vue)
> **后端 Controller**: `DashboardController`, `DepartmentController`, `DoctorController`, `UserController`, `ScheduleController`
> **角色权限**: admin, doctor, pharmacist (仪表盘); admin, doctor (基础数据)

---

## 1. 仪表盘模块 (Dashboard)

### 1.1 概述

仪表盘是用户登录后的**首页**，根据用户角色动态展示不同的统计卡片和图表，提供业务数据的一目了然视图。

### 1.2 前端页面 (DashboardView.vue)

#### 布局结构

```
┌─────────────────────────────────────────┐
│  PageHeader (标题 + 欢迎语)            │
├─────────────────────────────────────────┤
│  [统计卡片行] — 根据角色动态显示          │
│  ┌────────┐ ┌────────┐ ┌────────┐      │
│  │ 今日挂号 │ │ 候诊人数 │ │ 库存预警 │      │
│  └────────┘ └────────┘ └────────┘      │
├─────────────────────────────────────────┤
│  [图表行 1]                              │
│  ┌─────────────┐ ┌─────────────┐         │
│  │ 近7日挂号趋势 │ │ 科室挂号分布 │         │
│  └─────────────┘ └─────────────┘         │
├─────────────────────────────────────────┤
│  [图表行 2] + 快捷操作                    │
│  ┌─────────────┐ ┌─────────────┐         │
│  │ 低库存药品   │ │  快捷操作    │         │
│  └─────────────┘ └─────────────┘         │
└─────────────────────────────────────────┘
```

#### 统计卡片（角色动态显示）

| 卡片 | 角色 | 数据来源 | 颜色 |
|------|------|----------|------|
| 今日挂号 | 全部 | 今日挂号总数 | #0F9F8F (绿) |
| 候诊人数 | 全部 | 状态=候诊的挂号数 | #F59E0B (黄) |
| 库存预警 | admin/pharmacist | stock <= safety_stock 的药品数 | #EF4444 (红) |
| 今日已接诊 | doctor | 状态=已完成的挂号数 | #3B82F6 (蓝) |
| 待配药 | pharmacist | 状态=待缴费的处方数 | #8B5CF6 (紫) |

#### 图表组件

1. **近7日挂号趋势** (ECharts 折线图)
   - X轴: 近7天日期
   - Y轴: 挂号数量
   - 平滑曲线 + 面积填充
   - 颜色: #0F9F8F

2. **科室挂号分布** (ECharts 饼图)
   - 按科室统计今日挂号分布
   - 环形图 (radius: ['40%', '70%'])
   - 自动分配7种颜色

3. **低库存药品 Top5** (ECharts 柱状图)
   - 横向柱状图对比当前库存与安全库存
   - 当前库存: 红色 (#EF4444)
   - 安全库存: 灰色 (#cbd5e1)

#### 快捷操作

根据用户角色动态显示可跳转的功能入口：
- 患者挂号 → `/registration`
- 医生工作站 → `/workstation`
- 病历管理 → `/medical-records`
- 药品库存 → `/pharmacy`
- 处方管理 → `/prescriptions`
- 系统设置 → `/settings`

### 1.3 后端实现 (DashboardController)

```java
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "仪表盘", description = "首页统计数据")
public class DashboardController {

    @GetMapping("/stats")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<DashboardStats> stats()   // 获取仪表盘统计数据
}
```

### 1.4 统计数据 (DashboardStats)

```java
public class DashboardStats {
    private Integer todayRegCount;        // 今日挂号数
    private Integer waitingCount;         // 候诊人数
    private Integer stockAlertCount;      // 库存预警数
    private Integer completedCount;         // 今日已接诊数
    private Integer pendingDispenseCount;   // 待配药数
    private List<RegTrend> regTrend;             // 近7日挂号趋势
    private List<DeptRegDistribution> deptRegDistribution;  // 科室分布
    private List<LowStockItem> lowStockTopN;    // 低库存Top5
}
```

---

## 2. 基础数据模块 (Basic Data)

### 2.1 概述

基础数据模块是系统的**数据字典管理中心**，维护科室、医生、系统用户、排班号源等基础信息。所有业务模块都依赖这些基础数据。

### 2.2 前端页面 (BasicDataView.vue)

页面采用标签页 (Tab) 组织，包含四个子模块：

#### Tab 1: 科室管理

- 表格: 科室列表（ID、名称、位置、电话、操作）
- 操作: 新增 / 编辑 / 删除
- 表单: 名称*、位置、电话

#### Tab 2: 医生管理

- 表格: 医生列表（ID、姓名、科室、职称、状态、操作）
- 操作: 新增 / 编辑 / 删除
- 表单: 姓名*、科室（下拉）、职称、状态

#### Tab 3: 用户管理

- 表格: 用户列表（ID、用户名、真实姓名、角色、状态、操作）
- 操作: 新增 / 编辑 / 删除 / 重置密码
- 表单: 用户名*、密码*、真实姓名、角色（admin/doctor/pharmacist）、关联医生

#### Tab 4: 排班管理

- 表格: 排班列表（医生、日期、时段、总号源、剩余号源、操作）
- 操作: 新增 / 编辑 / 删除
- 表单: 医生（下拉）、日期、时段、总号源

---

## 3. 后端实现

### 3.1 DepartmentController

```java
@RestController
@RequestMapping("/api/departments")
@Tag(name = "科室管理", description = "科室基础数据管理")
public class DepartmentController {

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<Department>> list()          // 查询全部科室

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<Department> detail(@PathVariable Long id)

    @PostMapping
    @RequireRole("admin")
    public Result<Department> create(@RequestBody Department dept)

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<Department> update(@PathVariable Long id, @RequestBody Department dept)

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id)
}
```

### 3.2 DoctorController

```java
@RestController
@RequestMapping("/api/doctors")
@Tag(name = "医生管理", description = "医生基础数据与候诊队列")
public class DoctorController {

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<DoctorVO>> list()             // 查询全部医生

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<DoctorVO> detail(@PathVariable Long id)

    @PostMapping
    @RequireRole("admin")
    public Result<DoctorVO> create(@RequestBody DoctorVO vo)

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<DoctorVO> update(@PathVariable Long id, @RequestBody DoctorVO vo)

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id)

    @GetMapping("/{id}/waiting-queue")
    @RequireRole({"admin", "doctor"})
    public Result<List<RegistrationVO>> waitingQueue(@PathVariable Long id)
}
```

### 3.3 UserController

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "系统用户账号管理")
public class UserController {

    @GetMapping
    @RequireRole("admin")
    public Result<List<SysUser>> list()              // 查询全部用户

    @PostMapping
    @RequireRole("admin")
    public Result<SysUser> create(@RequestBody SysUser user)   // 新增用户

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<SysUser> update(@PathVariable Long id, @RequestBody SysUser user)

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id)

    @PutMapping("/password")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request)
        // 修改当前登录用户密码
}
```

### 3.4 ScheduleController

```java
@RestController
@RequestMapping("/api/schedules")
@Tag(name = "排班管理", description = "医生排班号源管理")
public class ScheduleController {

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<ScheduleVO>> list()           // 查询排班

    @GetMapping("/available")
    @RequireRole({"admin", "doctor"})
    public Result<List<ScheduleVO>> available(
        @RequestParam LocalDate date,
        @RequestParam(required = false) Long departmentId
    )

    @PostMapping
    @RequireRole("admin")
    public Result<ScheduleVO> create(@RequestBody ScheduleVO vo)

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<ScheduleVO> update(@PathVariable Long id, @RequestBody ScheduleVO vo)

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id)
}
```

---

## 4. 数据模型

### 4.1 Department (科室)

```java
@Entity
@Table(name = "department")
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String name;               // 名称
    @Column(length = 100)
    private String location;           // 位置
    @Column(length = 20)
    private String phone;              // 电话
}
```

### 4.2 Doctor (医生)

```java
@Entity
@Table(name = "doctor")
public class Doctor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String name;               // 姓名
    @Column(name = "department_id", nullable = false)
    private Long departmentId;       // 科室ID
    @Column(length = 30)
    private String title;            // 职称
    @Column(nullable = false)
    private Integer status = 1;      // 1=在职, 0=离职
}
```

### 4.3 SysUser (系统用户)

```java
@Entity
@Table(name = "sys_user")
public class SysUser {
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_DOCTOR = "doctor";
    public static final String ROLE_PHARMACIST = "pharmacist";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50, unique = true)
    private String username;           // 用户名
    @Column(nullable = false, length = 100)
    private String password;           // 密码（BCrypt）
    @Column(name = "real_name", length = 50)
    private String realName;           // 真实姓名
    @Column(length = 20)
    private String role = ROLE_DOCTOR; // 角色
    @Column(nullable = false)
    private Integer status = 1;        // 1=正常, 0=禁用
    @Column(name = "doctor_id")
    private Long doctorId;             // 关联医生ID

    public boolean isAdmin() { return ROLE_ADMIN.equals(role); }
    public boolean isDoctor() { return ROLE_DOCTOR.equals(role); }
    public boolean isPharmacist() { return ROLE_PHARMACIST.equals(role); }
}
```

### 4.4 Schedule (排班)

```java
@Entity
@Table(name = "schedule")
public class Schedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;             // 医生ID
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;        // 工作日期
    @Column(name = "time_slot", nullable = false, length = 20)
    private String timeSlot;           // 时段
    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots = 0;  // 总号源
    @Column(name = "remain_slots", nullable = false)
    private Integer remainSlots = 0; // 剩余号源
}
```

---

## 5. 数据库表结构

```sql
CREATE TABLE department (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    location    VARCHAR(100),
    phone       VARCHAR(20),
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE TABLE doctor (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    title         VARCHAR(30),
    status        TINYINT DEFAULT 1,
    create_time   DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE TABLE sys_user (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    real_name   VARCHAR(50),
    role        VARCHAR(20) DEFAULT 'doctor',
    status      TINYINT DEFAULT 1,
    doctor_id   BIGINT UNSIGNED,
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

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

## 6. 修改密码功能

```
PUT /api/users/password
Content-Type: application/json

Request Body:
{
  "oldPassword": "当前密码",
  "newPassword": "新密码"
}

校验规则:
- 旧密码必须正确
- 新密码不能为空
- 新密码不能与旧密码相同
- 新密码自动使用 BCrypt 加密存储
```

---

## 7. 与其他模块的关系

| 基础数据 | 被依赖模块 | 用途 |
|----------|------------|------|
| 科室 | 医生管理、排班管理、挂号预约 | 科室归属 |
| 医生 | 排班管理、挂号预约、医生工作站、病历、处方 | 医生信息 |
| 用户 | 登录认证、权限控制 | 系统登录 |
| 排班 | 挂号预约 | 号源管理 |
