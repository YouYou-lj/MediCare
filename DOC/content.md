# MediCare 智慧医疗门诊管理系统 - 项目知识文档

> 基线版本: v1.0.0-baseline | 最后更新: 2026-06-16

---

## 一、项目概述

MediCare 是一款基于 JavaFX 的社区医院门诊管理桌面应用，面向基层医疗场景，提供患者挂号、医生问诊、处方管理、药品库存等全流程门诊业务支持。强调本地化部署、离线可用性与操作简洁性。

---

## 二、技术栈

| 类别 | 技术选型 | 版本 |
|------|----------|------|
| 语言 | Java | 17 |
| GUI 框架 | JavaFX | 21.0.5 |
| 数据库 | MySQL | 8.0.33 |
| 连接池 | HikariCP | 4.0.3 |
| JDBC 工具 | Apache Commons DbUtils | 1.8.1 |
| 参数校验 | Hibernate Validator | 6.2.5.Final |
| 日志 | SLF4J + Logback | 1.2.12 |
| CSV 处理 | OpenCSV | 5.7.1 |
| 构建工具 | Maven | - |
| 打包插件 | maven-shade-plugin | 3.4.1 |

---

## 三、项目目录结构

```
MediCare/
├── DOC/                          # 项目文档与资源
│   ├── assets/drawio/            # 架构图/流程图源文件
│   ├── fxml-templates/           # FXML 布局模板
│   └── tasks/                    # 任务说明文档
├── sql/
│   ├── schema_baseline.sql       # 数据库 DDL + 初始数据
│   └── seed_data.sql             # 完整种子数据
├── src/main/
│   ├── java/com/medicare/
│   │   ├── Launcher.java         # JAR 启动入口（代理 Main）
│   │   ├── Main.java             # JavaFX Application 入口
│   │   ├── controller/           # 控制层（16个类）
│   │   ├── dao/                  # 数据访问层（12个类）
│   │   ├── model/                # 实体模型层（11个类）
│   │   ├── service/              # 业务逻辑层（9个类）
│   │   └── util/                 # 工具类
│   └── resources/
│       ├── css/                  # 样式表
│       ├── fxml/                 # FXML 布局文件（17个）
│       └── logback.xml           # 日志配置
├── scaffold/                     # 脚手架/初始代码参考
└── pom.xml                       # Maven 项目配置
```

---

## 四、四层架构

```
┌──────────────────────────────────────────────────┐
│  Controller 层 (JavaFX Controller)               │
│  处理 UI 事件、加载数据到 TableView/控件           │
│  异步模式: javafx.concurrent.Task + new Thread()  │
├──────────────────────────────────────────────────┤
│  Service 层 (业务逻辑)                            │
│  事务控制、业务校验、跨 DAO 编排                    │
│  getConnection() → 操作 → commit/rollback/close   │
├──────────────────────────────────────────────────┤
│  DAO 层 (数据访问)                                │
│  继承 BaseDAO<T>，SQL 常量定义在类顶部             │
│  JOIN 查询用 AS 别名映射关联字段                   │
├──────────────────────────────────────────────────┤
│  Model 层 (POJO)                                 │
│  纯 Java Bean，含关联字段（非 DB 列）              │
│  状态常量定义在类中                                │
└──────────────────────────────────────────────────┘
```

---

## 五、核心类详解

### 5.1 入口与配置

| 类 | 路径 | 职责 |
|----|------|------|
| `Launcher` | `com.medicare.Launcher` | Fat JAR 启动入口，代理调用 `Main.main()` |
| `Main` | `com.medicare.Main` | JavaFX Application，加载 LoginView，`stop()` 关闭连接池 |
| `ConnectionConfig` | `com.medicare.util.ConnectionConfig` | HikariCP 单例连接池，最大20连接，最小空闲5 |

**数据库连接配置**:
- URL: `jdbc:mysql://localhost:3306/medicare?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true`
- 用户名: `root`，密码: `mysql`

### 5.2 BaseDAO 泛型基类

`com.medicare.dao.BaseDAO<T>` — 所有 DAO 的父类

**非事务方法**（自动获取/关闭连接）:
- `querySingle(sql, params)` → 单条记录
- `queryList(sql, params)` → 列表记录
- `queryScalar(sql, params)` → 标量值（COUNT/SUM）
- `executeUpdate(sql, params)` → 增删改
- `executeInsert(sql, params)` → 插入并返回自增主键

**事务方法**（需传 Connection）:
- `getConnection()` → 获取连接并关闭自动提交
- `executeUpdate(conn, sql, params)` / `executeInsert(conn, sql, params)`
- `querySingle(conn, sql, params)` / `queryList(conn, sql, params)`
- `commit(conn)` / `rollback(conn)` / `closeConnection(conn)`

**JavaTimeBeanProcessor**: 扩展 GenerousBeanProcessor，支持 `LocalDate`/`LocalDateTime`/`LocalTime` 类型映射

### 5.3 Model 层（11个实体类）

| 实体类 | 数据库表 | 关键字段 | 状态/类型常量 |
|--------|----------|----------|---------------|
| `Department` | department | name, location, phone | - |
| `Doctor` | doctor | name, departmentId, title, status | status: 0-停用 1-在职 |
| `Patient` | patient | idCard, name, gender, birthDate, phone, allergyInfo | gender: 0-女 1-男 2-其他 |
| `Schedule` | schedule | doctorId, workDate, timeSlot, totalSlots, remainSlots | - |
| `Registration` | registration | patientId, scheduleId, regTime, status, seqNo, fee | status: 0-候诊 1-就诊中 2-已完成 3-已取消 |
| `MedicalRecord` | medical_record | registrationId, patientId, doctorId, chiefComplaint, diagnosis | - |
| `Medicine` | medicine | name, spec, unit, stock, safetyStock, price, pinyinCode | status: 0-停用 1-启用 |
| `Prescription` | prescription | recordId, patientId, doctorId, totalAmount, status | status: 0-待缴费 1-已缴费 2-已取药 3-已作废 |
| `PrescriptionItem` | prescription_item | prescriptionId, medicineId, quantity, dosage, unitPrice, amount | - |
| `InventoryLog` | inventory_log | medicineId, type, quantity, batchNo, operator | type: 1-入库 2-出库 3-盘盈 4-盘亏 |
| `SysUser` | sys_user | username, password, realName, role, doctorId | role: admin/doctor/pharmacist |

**关联字段说明**: Model 中部分字段非数据库列，由 DAO 的 JOIN 查询 AS 别名映射:
- `Doctor.departmentName` ← `dep.name AS departmentName`
- `Registration.patientName/doctorName/departmentName/timeSlot` ← JOIN 查询
- `Schedule.doctorName/departmentName` ← JOIN 查询

### 5.4 DAO 层（12个类）

| DAO 类 | 关键方法 | 事务方法 |
|--------|----------|----------|
| `DepartmentDAO` | findAll, findById, insert, update, delete | - |
| `DoctorDAO` | findAll, findById, findByDepartment, insert, update | - |
| `PatientDAO` | findAll, findById, findByIdCard, insert, update, delete | - |
| `ScheduleDAO` | findByDoctorAndDate, findById, insert, update, delete | decrementRemain, incrementRemain |
| `RegistrationDAO` | findToday, findByPatient, findByStatus, findWaitingByDoctor, insert, updateStatus, delete | insert(conn), delete(conn) |
| `MedicalRecordDAO` | findByRegistration, findByPatient, insert, update | insert(conn) |
| `MedicineDAO` | findAll, findById, findByName, insert, update, delete | updateStock(conn) |
| `PrescriptionDAO` | findById, findByRecord, findByPatient, findToday, insert, updateStatus | insert(conn), updateStatus(conn) |
| `PrescriptionItemDAO` | findByPrescription, insert | insert(conn) |
| `InventoryLogDAO` | findByMedicine, findByDateRange, insert | insert(conn) |
| `SysUserDAO` | findByUsername, findAll, insert, update, delete | - |
| `ScheduleDAO` | findByDoctorAndDate, decrementRemain, incrementRemain | - |

### 5.5 Service 层（9个类）

| Service 类 | 核心业务逻辑 | 事务场景 |
|------------|-------------|----------|
| `DepartmentService` | 科室 CRUD | - |
| `DoctorService` | 医生 CRUD + 关联查询 | - |
| `PatientService` | 患者 CRUD + 身份证唯一校验 | - |
| `ScheduleService` | 排班 CRUD + 号源管理 | - |
| `RegistrationService` | 挂号/取消挂号/叫号/完成就诊 | 挂号: 扣减号源 + 生成记录；取消: 释放号源 + 删除记录 |
| `MedicalRecordService` | 病历 CRUD + 按患者/挂号查询 | 病历创建(事务) |
| `MedicineService` | 药品 CRUD + 入库/出库 | 入库/出库: 更新库存 + 记录日志 |
| `PrescriptionService` | 开立处方/取药/作废处方 | 开立: 保存处方 + 扣减库存 + 记录日志；作废: 回滚库存 + 更新状态 |
| `SysUserService` | 用户登录/CRUD/密码修改 | - |

**事务控制模式**（Service 层统一管理）:
```java
Connection conn = null;
try {
    conn = xxxDAO.getConnection();      // 获取连接，关闭自动提交
    // ... 多个 DAO 操作 ...
    xxxDAO.commit(conn);                // 提交
} catch (Exception e) {
    xxxDAO.rollback(conn);              // 回滚
    throw e;
} finally {
    xxxDAO.closeConnection(conn);       // 归还连接
}
```

### 5.6 Controller 层（16个类）

| Controller | FXML | 功能模块 |
|------------|------|----------|
| `LoginController` | LoginView.fxml | 登录验证，静态 `currentUser` 供全局访问 |
| `MainController` | MainView.fxml | 左侧导航 + StackPane 动态加载子视图，Dashboard |
| `BasicDataController` | BasicDataView.fxml | 科室/医生/排班三合一 Tab 页 |
| `PatientController` | PatientView.fxml | 患者列表 + 搜索 |
| `PatientDialogController` | PatientDialog.fxml | 患者新增/编辑弹窗 |
| `RegistrationController` | RegistrationView.fxml | 挂号预约 + 号源展示 |
| `PatientSelectDialogController` | PatientSelectDialog.fxml | 挂号时选择患者弹窗 |
| `DoctorWorkstationController` | DoctorWorkstationView.fxml | 叫号/病历书写/历史病历 |
| `MedicalRecordController` | MedicalRecordView.fxml | 病历列表管理 |
| `MedicalRecordDetailDialogController` | MedicalRecordDetailDialog.fxml | 病历详情弹窗 |
| `PharmacyController` | PharmacyView.fxml | 药品库存 + 入库/出库/预警 |
| `MedicineDialogController` | MedicineDialog.fxml | 药品新增/编辑弹窗 |
| `StockDialogController` | StockDialog.fxml | 库存变动弹窗（入库/出库/盘点） |
| `PrescriptionController` | PrescriptionView.fxml | 处方开立（选患者→添加药品→保存） |
| `SettingsController` | SettingsView.fxml | 系统设置 + 用户管理 |
| `AdminDialogController` | AdminDialog.fxml | 管理员 CRUD + 密码修改弹窗 |

**UI 异步模式**:
```java
javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
    @Override
    protected Void call() throws Exception {
        // 数据库操作（后台线程）
        return null;
    }
    @Override
    protected void succeeded() {
        // UI 更新（FX Application Thread）
    }
};
new Thread(task).start();
```

**弹窗模式**:
```java
Dialog<ButtonType> dialog = new Dialog<>();
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/XXXDialog.fxml"));
dialog.setDialogPane(loader.load());
// 拦截 OK 按钮，实现异步保存
dialog.getDialogPane().addEventFilter(ButtonType.OK, event -> {
    event.consume();  // 阻止默认关闭
    // 异步保存逻辑...
});
```

---

## 六、数据库设计

### 6.1 ER 关系图

```
department ──1:N── doctor ──1:N── schedule
                           │
                           └──(via schedule)── registration ──1:1── medical_record
                                │                              │
                                │                              └──1:N── prescription
                                │                                          │
                           patient ──────────────────────────────────────1:N── prescription_item
                                │                                                    │
                                │                                          medicine ──1:N── inventory_log
                                └────────────────────────────────────────────────────────┘

sys_user (独立，doctor_id 可关联 doctor)
```

### 6.2 数据库表清单（11张表）

| 表名 | 中文名 | 主键 | 核心字段 | 唯一键/索引 |
|------|--------|------|----------|-------------|
| department | 科室表 | id(BIGINT UNSIGNED) | name, location, phone | uk_department_name |
| doctor | 医生表 | id | name, department_id, title, status | idx_doctor_dept |
| patient | 患者表 | id | id_card, name, gender, birth_date, phone, allergy_info | uk_patient_id_card |
| schedule | 排班号源表 | id | doctor_id, work_date, time_slot, total_slots, remain_slots | uk_schedule_doctor_date_slot |
| registration | 挂号表 | id | patient_id, schedule_id, reg_time, status, seq_no, fee | idx_reg_patient, idx_reg_schedule, idx_reg_status |
| medical_record | 病历表 | id | registration_id, patient_id, doctor_id, chief_complaint, diagnosis | idx_mr_patient_time, idx_mr_reg |
| prescription | 处方表 | id | record_id, patient_id, doctor_id, total_amount, status | idx_presc_record, idx_presc_patient |
| medicine | 药品表 | id | name, spec, unit, stock, safety_stock, price, pinyin_code | uk_medicine_name_spec, idx_medicine_pinyin |
| prescription_item | 处方明细表 | id | prescription_id, medicine_id, quantity, dosage, unit_price, amount | idx_pi_prescription |
| inventory_log | 库存日志表 | id | medicine_id, type, quantity, batch_no, operator, remark | idx_invlog_medicine_time |
| sys_user | 系统用户表 | id | username, password, real_name, role, doctor_id | uk_sys_user_username |

### 6.3 外键约束

| 外键 | 引用 | 删除策略 |
|------|------|----------|
| doctor.department_id → department.id | 科室 | RESTRICT |
| schedule.doctor_id → doctor.id | 医生 | RESTRICT |
| registration.patient_id → patient.id | 患者 | RESTRICT |
| registration.schedule_id → schedule.id | 排班 | RESTRICT |
| medical_record.registration_id → registration.id | 挂号 | RESTRICT |
| medical_record.patient_id → patient.id | 患者 | RESTRICT |
| medical_record.doctor_id → doctor.id | 医生 | RESTRICT |
| prescription.record_id → medical_record.id | 病历 | RESTRICT |
| prescription.patient_id → patient.id | 患者 | RESTRICT |
| prescription.doctor_id → doctor.id | 医生 | RESTRICT |
| prescription_item.prescription_id → prescription.id | 处方 | RESTRICT |
| prescription_item.medicine_id → medicine.id | 药品 | RESTRICT |
| inventory_log.medicine_id → medicine.id | 药品 | RESTRICT |

---

## 七、核心业务流程

### 7.1 挂号流程

```
患者选择排班号源 → 校验号源余量 → 事务开始
  ├── 扣减号源(remain_slots - 1)
  ├── 分配序号(seq_no + 1)
  └── 生成挂号记录(status=0 候诊)
→ 事务提交 → 叫号(status→1 就诊中) → 完成就诊(status→2)
```

### 7.2 医生问诊流程

```
叫号(Registration.status→1) → 填写病历(MedicalRecord)
  → 开立处方(Prescription + PrescriptionItem)
    → 事务开始
      ├── 保存处方主表
      └── 逐条处理明细
          ├── 校验库存
          ├── 扣减库存(medicine.stock)
          ├── 记录库存日志(InventoryLog, type=2 出库)
          └── 保存处方明细
    → 事务提交
→ 完成就诊(Registration.status→2)
```

### 7.3 药品库存流程

```
入库: 事务开始 → 更新库存(+N) → 记录日志(type=1 入库) → 事务提交
出库: 事务开始 → 校验库存 → 更新库存(-N) → 记录日志(type=2 出库) → 事务提交
盘点: 盘盈(type=3)/盘亏(type=4) → 更新库存 → 记录日志
```

### 7.4 处方生命周期

```
开立(STATUS_PENDING=0) → 缴费(STATUS_PAID=1) → 取药(STATUS_DISPENSED=2)
                                    ↓
                              作废(STATUS_VOID=3) → 回滚库存 + 记录日志(type=3 盘盈)
```

---

## 八、FXML 视图对照表

| FXML 文件 | 对应 Controller | 视图说明 |
|-----------|-----------------|----------|
| LoginView.fxml | LoginController | 登录界面（1000×700，不可调整大小） |
| MainView.fxml | MainController | 主界面（左导航 + 右StackPane） |
| BasicDataView.fxml | BasicDataController | 基础数据（TabPane: 科室/医生/排班） |
| PatientView.fxml | PatientController | 患者列表 |
| PatientDialog.fxml | PatientDialogController | 患者编辑弹窗 |
| RegistrationView.fxml | RegistrationController | 挂号预约 |
| PatientSelectDialog.fxml | PatientSelectDialogController | 选择患者弹窗 |
| DoctorWorkstationView.fxml | DoctorWorkstationController | 医生工作站 |
| MedicalRecordView.fxml | MedicalRecordController | 病历管理 |
| MedicalRecordDetailDialog.fxml | MedicalRecordDetailDialogController | 病历详情弹窗 |
| PharmacyView.fxml | PharmacyController | 药品库存 |
| MedicineDialog.fxml | MedicineDialogController | 药品编辑弹窗 |
| StockDialog.fxml | StockDialogController | 库存变动弹窗 |
| PrescriptionView.fxml | PrescriptionController | 处方开立 |
| SettingsView.fxml | SettingsController | 系统设置 |
| AdminDialog.fxml | AdminDialogController | 用户管理弹窗 |

---

## 九、构建与运行

### 9.1 Maven 命令

```bash
# 编译
mvn compile

# 运行（开发模式）
mvn javafx:run

# 打包 Fat JAR
mvn clean package

# 运行 JAR
java -jar target/mediCare-1.0.0-SNAPSHOT.jar
```

### 9.2 打包配置要点

- 主类: `com.medicare.Launcher`（非 `Main`，解决 JavaFX 模块系统问题）
- 使用 `maven-shade-plugin` 打包为可执行 Fat JAR
- JavaFX 依赖以 compile scope 引入，shade 进 JAR

### 9.3 数据库初始化

```bash
mysql -u root -p < sql/schema_baseline.sql    # 建库+建表+初始数据
mysql -u root -p < sql/seed_data.sql           # 完整测试数据（可选）
```

---

## 十、已知待改进项

| # | 问题 | 位置 | 优先级 |
|---|------|------|--------|
| 1 | 密码明文存储 | `SysUserService.login()` 直接比对明文 | 高 |
| 2 | Registration 缺少 doctorId 字段 | `PrescriptionController` 需通过 Schedule 间接获取 | 中 |
| 3 | Dashboard 未接入实时数据 | `MainController.refreshDashboard()` 硬编码 0 | 中 |
| 4 | 患者删除未校验关联挂号 | `PatientService.deletePatient()` 标注 TODO | 中 |
| 5 | 数据库连接密码硬编码 | `ConnectionConfig` 中密码为硬编码字符串 | 低 |

---

## 十一、代码规范摘要

1. **SQL 常量**: 所有 SQL 语句定义为 DAO 类顶部的 `private static final String`，禁止方法内拼接
2. **JOIN 别名映射**: 关联字段使用 `AS` 别名映射到 Model 的非 DB 列字段
3. **事务边界**: DAO 层禁止自行管理事务，由 Service 层统一控制
4. **异步 UI**: 所有数据库操作用 `javafx.concurrent.Task` 包裹，UI 更新通过 `Platform.runLater()`
5. **弹窗保存**: `addEventFilter(ButtonType.OK)` 拦截默认关闭，实现异步保存后手动关闭
6. **DAO 实例化**: Service 通过 `new XXXDAO()` 创建 DAO 实例（无依赖注入框架）
7. **日志规范**: 使用 SLF4J + Logback，`logger.info/warn/error` 分级输出