# PR-001：MediCare 智慧医疗门诊管理系统 — 企业级桌面应用开发方案

> **提交人**：南京万和信息技术有限公司 研发部  
> **项目代号**：MediCare  
> **项目类型**：JavaFX 桌面客户端 + MySQL 后端  
> **目标用户**：社区医院、专科门诊、乡镇卫生院（日均门诊量 200–500 人次）  
> **状态**：待评审（Ready for Review）

---

## 1. 项目概述

### 1.1 背景
当前中小型医疗机构信息化程度低，核心痛点包括：
- **挂号效率低**：高峰期窗口拥堵，现场排队耗时长；
- **病历管理混乱**：纸质病历易丢失、难检索；
- **药品库存不准**：凭经验补货，常出现短缺或过期积压；
- **数据孤岛**：挂号、病历、药房各系统独立，数据不互通。

### 1.2 目标
开发一套基于 Java 的桌面级智慧医疗门诊管理系统（MediCare），覆盖患者管理、挂号预约、医生工作站、病历管理、药品库存、处方管理六大核心模块，具备完整的 JavaFX 图形界面，支持单机或局域网部署。

### 1.3 交付物
- 可运行的桌面客户端（`mediCare.jar` 或安装包）；
- 完整数据库脚本（DDL + 初始数据）；
- 数据库物理模型设计文档（ER 图 + 物理模型）；
- 技术文档（架构说明、接口设计、部署手册）；
- Git 仓库（含完整提交历史与分支管理）。

---

## 2. 功能需求

| 模块 | 功能点 | 业务规则 |
|------|--------|----------|
| **患者管理** | 患者登记、档案查询、搜索、编辑、删除 | 身份证唯一性校验；手机号格式校验；弹窗模式新增/编辑患者；表格操作列（编辑/删除） |
| **挂号预约** | 科室选择、医生排班查看、现场挂号 | 号源按医生+日期+时段管理；挂号后实时扣减号源；支持当日剩余号源分配；挂号后叫号/完成/取消全流程 |
| **医生工作站** | 患者叫号、病历书写、完成就诊 | 登录后展示今日候诊列表；选中患者进入病历书写界面；结构化字段（主诉/现病史/既往史/体格检查/诊断/建议） |
| **病历管理** | 历史病历检索、电子病历存储、病历详情查看 | 独立管理界面；全量病历列表；按患者/医生/诊断/主诉模糊搜索；结构化字段；只读详情弹窗；支持删除 |
| **药品库存** | 药品入库、出库、库存查询、有效期预警 | 入库记录批次号和有效期（同步更新药品批号/有效期）；库存低于安全阈值或有效期前 30 天自动预警；操作列（编辑/删除/出入库） |
| **处方管理** | 处方开立、库存校验 | 选择已完成就诊患者开立处方；自动检索药品；处方主表+明细表设计；保存时事务级库存扣减；**前端添加药品时暂未即时校验库存（待优化）** |
| **基础数据** | 科室管理、医生管理、排班管理 | Tab 页切换；增删改查完整；排班与号源联动 |
| **系统设置** | 管理员管理、密码修改 | 弹窗新增/编辑管理员；密码修改校验 |

---

## 3. 技术架构

### 3.1 分层架构
采用经典四层分层架构，职责边界清晰：

```
┌─────────────────────────────────────────┐
│  Presentation Layer（表示层）            │
│  JavaFX FXML + Controller + CSS           │
│  界面渲染、事件响应、表单校验、数据绑定     │
├─────────────────────────────────────────┤
│  Service Layer（业务逻辑层）                │
│  PatientService / RegistrationService     │
│  / DoctorService / PrescriptionService    │
│  / PharmacyService                        │
│  业务流程编排、事务控制、业务规则校验         │
├─────────────────────────────────────────┤
│  DAO Layer（数据访问层）                     │
│  各实体 DAO（基于 DbUtils 模板）             │
│  SQL 封装、QueryRunner 执行、结果映射        │
├─────────────────────────────────────────┤
│  Model Layer（实体模型层）                   │
│  Patient / Doctor / Department /          │
│  Registration / MedicalRecord /           │
│  Prescription / Medicine 等               │
└─────────────────────────────────────────┘
```

### 3.2 数据流
用户操作 JavaFX 界面 → Controller 接收事件 → 调用 Service 层方法 → Service 调用 DAO 层 DbUtils 模板执行 SQL → DAO 返回实体对象/集合 → Service 处理业务逻辑 → Controller 更新界面显示。

**关键约束**：所有数据库操作通过 DAO 层统一封装；**事务控制统一在 Service 层**，禁止 DAO 层自行管理事务。

---

## 4. 技术选型与开发环境

### 4.1 核心技术栈

| 技术点 | 选用方案 | 版本/说明                                     |
|--------|----------|-------------------------------------------|
| 开发语言 | Java | 17 LTS（兼容 JavaFX 21）                      |
| 图形界面 | JavaFX + FXML | 官方桌面应用标准技术，FXML 分离界面与逻辑                   |
| 界面设计器 | Gluon Scene Builder | 独立可视化 FXML 设计工具，与 IDEA 协同                 |
| 数据库 | MySQL | 8.0 Community Server                      |
| 数据库操作 | Apache Commons DbUtils + HikariCP | DbUtils 1.8.1 + HikariCP 4.0.3            |
| 构建工具 | Maven | 3.8+                                      |
| 版本管理 | Git | 2.35+                                     |
| 日志 | SLF4J + Logback | slf4j-api 1.7.36 / logback-classic 1.2.12 |
| 数据校验 | Hibernate Validator | 6.2.5.Final                               |
| 数据导出 | OpenCSV | 5.7.1                                     |

### 4.2 开发环境配置

| 工具 | 版本/配置 | 用途 |
|------|-----------|------|
| **IntelliJ IDEA** | Community Edition 或 Ultimate | 主 IDE，负责代码开发、Maven 构建、Git 管理、SQL 调试 |
| **Gluon Scene Builder** | 最新稳定版 | FXML 可视化设计，生成 `.fxml` 布局文件，由 IDEA 引用绑定 |
| **MySQL Workbench** | 8.0+ | 数据库物理模型设计、ER 图绘制、DDL 生成 |
| **Git** | 2.35+ | 版本控制 |
| **Maven** | 3.8+ | 依赖管理与项目构建 |

**IDEA 与 Scene Builder 协作流程**：
1. 在 Scene Builder 中拖拽设计界面，生成/修改 `.fxml` 文件；
2. IDEA 中打开 `.fxml` 文件，编写对应的 `Controller` 类，绑定 `@FXML` 注解与事件方法；
3. 通过 IDEA 的 Maven 面板一键编译运行，Scene Builder 与 IDEA 分屏或双屏协作。

---

## 5. 数据库物理模型设计

### 5.1 设计流程
数据库设计遵循 **先模型后代码** 原则，流程如下：

```
概念模型（ER 图） → 逻辑模型（关系模式） → 物理模型（MySQL 表结构） → DDL 脚本 → 基线锁定
```

### 5.2 核心实体关系（ER 图）

| 实体 | 关联关系 |  cardinality |
|------|----------|-------------|
| 患者（Patient） | 1 : N | 挂号（Registration） |
| 医生（Doctor） | 1 : N | 排班/号源（Schedule） |
| 科室（Department） | 1 : N | 医生（Doctor） |
| 挂号（Registration） | 1 : 1 | 病历（MedicalRecord） |
| 病历（MedicalRecord） | 1 : N | 处方（Prescription） |
| 处方（Prescription） | 1 : N | 处方明细（PrescriptionItem） |
| 药品（Medicine） | 1 : N | 处方明细（PrescriptionItem）、库存记录（Inventory） |

### 5.3 物理模型设计要点

| 表名 | 关键字段 | 约束与索引 |
|------|----------|-----------|
| `patient` | `id`, `id_card`, `phone`, `name`, `gender`, `birth_date`, `address`, `allergy_info`, `create_time`, `update_time` | PK: `id`; UK: `id_card`; IDX: `phone` |
| `doctor` | `id`, `name`, `department_id`, `title`, `status` | PK: `id`; FK: `department_id` → `department(id)` |
| `department` | `id`, `name`, `location`, `phone` | PK: `id`; UK: `name` |
| `schedule` | `id`, `doctor_id`, `work_date`, `time_slot`, `total_slots`, `remain_slots` | PK: `id`; UK: `doctor_id + work_date + time_slot`; FK: `doctor_id` |
| `registration` | `id`, `patient_id`, `schedule_id`, `reg_time`, `status`, `seq_no` | PK: `id`; FK: `patient_id`, `schedule_id` |
| `medical_record` | `id`, `registration_id`, `patient_id`, `doctor_id`, `chief_complaint`, `present_illness`, `past_history`, `physical_exam`, `diagnosis`, `advice`, `create_time`, `update_time` | PK: `id`; FK: `registration_id`, `patient_id`, `doctor_id`; IDX: `patient_id + create_time` |
| `prescription` | `id`, `record_id`, `patient_id`, `doctor_id`, `total_amount`, `status`, `create_time` | PK: `id`; FK: `record_id`, `patient_id`, `doctor_id` |
| `prescription_item` | `id`, `prescription_id`, `medicine_id`, `quantity`, `dosage`, `usage`, `unit_price`, `amount` | PK: `id`; FK: `prescription_id`, `medicine_id` |
| `medicine` | `id`, `name`, `spec`, `unit`, `stock`, `safety_stock`, `expiry_date`, `batch_no`, `pinyin_code` | PK: `id`; UK: `name + spec`; IDX: `pinyin_code`, `expiry_date` |
| `inventory_log` | `id`, `medicine_id`, `type`, `quantity`, `batch_no`, `expiry_date`, `operator`, `log_time` | PK: `id`; FK: `medicine_id`; IDX: `medicine_id + log_time` |

### 5.4 设计规范
- **字符集**：`utf8mb4`，排序规则 `utf8mb4_unicode_ci`；
- **主键**：全部使用自增 `BIGINT UNSIGNED`，禁止业务字段做主键；
- **外键**：物理层建立外键约束，确保引用完整性，删除策略统一为 `RESTRICT`（禁止级联删除，由业务层控制）；
- **索引策略**：查询条件字段、外键字段、时间范围字段必须建索引；联合索引遵循最左前缀原则；
- **字段命名**：全小写下划线分隔，如 `create_time`、`id_card`；
- **时间字段**：统一使用 `DATETIME(3)`，默认 `CURRENT_TIMESTAMP(3)`；
- **金额字段**：使用 `DECIMAL(10,2)`，禁止 `FLOAT`/`DOUBLE`。

---

## 6. 开发计划（Milestone）

| 里程碑 | 周期 | 核心任务 | 交付物 |
|--------|------|----------|--------|
| **M1：需求与模型** | Week 1 | 需求评审定稿；数据库概念模型 → 逻辑模型 → **物理模型**设计；MySQL Workbench 生成 ER 图与物理模型图；DDL 脚本评审与基线锁定 | 需求规格说明书、ER 图、物理模型文档、DDL 基线脚本 |
| **M2：基础设施** | Week 1 | IDEA 工程搭建（Maven 多模块或单模块）；HikariCP 连接池配置；DbUtils 工具类封装；通用 **BaseDAO**（泛型+反射）实现；Gluon Scene Builder 设计主界面框架 FXML；Git 分支策略建立 | 可编译运行的基础工程、BaseDAO 与 DbUtils 工具类、主界面框架 FXML、数据库连接验证通过 |
| **M3：核心模块开发** | Week 2–3 | 按模块迭代开发：患者管理 → 挂号预约 → 医生工作站 → 病历管理 → 药品库存 → 处方管理。每模块包含 DAO → Service → Controller → FXML 界面 | 六大模块单体可运行，代码审查完成 |
| **M4：系统集成** | Week 4 | 全链路联调（登记→挂号→病历→处方→药品）；编译警告清零；代码审查（DbUtils 规范、事务控制、编码规范）；性能调优（SQL 索引、连接池参数） | 完整可运行系统、Bug 清单清零 |
| **M5：交付与部署** | Week 4 | 编写部署手册与用户手册；打包发布（JAR/安装程序）；Git 打 Tag；项目复盘 | 安装包、技术文档、用户手册、源码仓库归档 |

**开发模式**：M3 阶段采用 **Code Review 强制门禁**，重点审查：
- DbUtils 模板使用是否规范（是否继承 BaseDAO、是否拼接 SQL）；
- Service 层事务边界是否清晰（`setAutoCommit`/`commit`/`rollback`）；
- JavaFX 数据绑定与后台线程分离（禁止在 UI 线程执行耗时 SQL）。

---

## 7. 工程规范

### 7.1 编码规范（Java）
- **命名**：类名大驼峰（`PatientService`），方法/变量小驼峰（`findById`），常量全大写下划线（`MAX_POOL_SIZE`）；
- **包名**：全小写，如 `com.medicare.dao`、`com.medicare.service`；
- **格式**：4 空格缩进（禁用 Tab），行宽 ≤ 120 字符，K&R 大括号风格；
- **注释**：类注释（作者/日期/功能）、方法注释（参数/返回值/异常）、复杂逻辑行内注释。

### 7.2 DbUtils 使用规范
- 所有 DAO **必须继承 BaseDAO**，禁止直接 `new QueryRunner`；
- SQL 语句使用 **常量定义**（或集中管理在 XML/枚举），禁止在业务方法中拼接 SQL；
- 查询使用 `BaseDAO.querySingle` / `queryList`，更新使用 `executeUpdate`；
- **事务控制统一在 Service 层**，禁止 DAO 层自行管理事务。

### 7.3 Git 分支规范
- `main`：稳定分支，仅接受合并请求；
- `develop`：日常开发分支；
- `feature/<module>`：功能分支（如 `feature/patient-mgmt`）；
- `fix/<issue-id>`：修复分支。

提交信息格式：
```
<type>(<<scope>): <subject>

type: feat / fix / docs / refactor / test
```

### 7.4 Maven 关键依赖（`pom.xml`）
```xml
<!-- JavaFX -->
org.openjfx:javafx-controls:17
org.openjfx:javafx-fxml:17

<!-- Database -->
mysql:mysql-connector-java:8.0.33
commons-dbutils:commons-dbutils:1.8.1
com.zaxxer:HikariCP:4.0.3

<!-- Logging & Validation -->
org.slf4j:slf4j-api:1.7.36
ch.qos.logback:logback-classic:1.2.12
org.hibernate.validator:hibernate-validator:6.2.5.Final

<!-- Utils -->
com.opencsv:opencsv:5.7.1
```

---

## 8. 风险与依赖

| 风险项 | 影响 | 缓解措施 |
|--------|------|----------|
| **数据库物理模型变更** | M3 阶段返工 | M1 阶段必须完成模型评审与 DDL 基线锁定，变更需走评审流程 |
| **JavaFX 跨平台兼容性** | 不同 OS 渲染差异 | 统一使用 JavaFX 17 LTS，避免使用平台特定 API |
| **DbUtils 事务边界模糊** | 处方/挂号出现数据不一致 | 强制 Service 层事务模板，Code Review 重点检查 |
| **MySQL 8.0 驱动兼容性** | 连接失败或时区异常 | 统一使用 `mysql-connector-java:8.0.33`，连接串预置 `serverTimezone=Asia/Shanghai` |
| **Scene Builder 与 IDEA 协作** | 开发者不习惯独立工具 | 提供 FXML 设计规范文档，复杂布局优先手写 + CSS |
| **需求蔓延** | 客户要求增加住院/医保模块 | 严格锁定 MVP 范围，二期需求另立项 |

---

## 9. 验收标准

- [x] **功能完整性**：患者/挂号/医生工作站/病历/处方/药品核心功能正常，业务流程闭环；
- [x] **数据库模型**：物理模型文档完整，ER 图与物理模型图一致，DDL 脚本可重复执行；
- [x] **代码质量**：编译零警告，符合企业编码规范，DbUtils 模板使用规范，事务控制正确；
- [x] **界面体验**：JavaFX 界面美观、导航流畅、无 UI 阻塞，弹窗组件复用（PatientDialog），操作列风格统一；
- [x] **数据安全**：敏感操作（处方开立、库存扣减、挂号）在事务内完成，异常时回滚；
- [ ] **文档齐全**：架构文档、数据库物理模型设计文档、部署手册、用户手册齐备；
- [ ] **Git 规范**：分支管理清晰，提交历史完整，Tag 标记发布版本。

## 10. 代码已实现功能与 PR 文档差异说明

> 以下功能在 PR 文档初稿中提及，但实际代码中未实现或方案调整，以代码实现为准：

| PR 文档原需求 | 代码实际状态 | 说明 |
|---------------|--------------|------|
| 患者管理 — 就诊卡管理 | ❌ 未实现 | 无 `card_no` 字段及相关功能；患者通过身份证号唯一标识 |
| 医生工作站 — 处方开立 | ❌ 未集成 | 处方开立为独立模块（"处方管理"导航菜单），未嵌入医生工作站 |
| 病历管理 — 病历模板 | ❌ 未实现 | 无病历模板表及模板管理功能 |
| 处方管理 — 添加药品时即时库存校验 | ⚠️ 待优化 | 前端添加药品明细时未校验库存，仅在保存处方时事务层校验 |
| 挂号预约 — 取消挂号释放号源 | ⚠️ 事务边界 | `incrementRemain` 与 `delete` 不在同一连接事务内，存在号源与记录不一致风险 |

**二期规划（未在本次 MVP 范围）**：就诊卡管理、病历模板、医生工作站内嵌处方开立、前端即时库存校验。

---

**评审意见栏**

- [ ] 数据库物理模型（ER 图 + 物理模型）是否需提前安排专项评审会议？
- [ ] M1 模型设计周期（1 周）对 11 张核心表是否充足？
- [ ] 是否确认不引入 JUnit，仅通过 Code Review + 联调测试保障质量？
- [ ] M3 开发周期（2 周）对 6 个模块是否需增加缓冲时间？

> **下一步行动**：待评审通过后，基于 MySQL Workbench 物理模型生成 DDL 基线，创建 `develop` 分支，按 M1 计划启动项目。
