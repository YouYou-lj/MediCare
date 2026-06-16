# MediCare 学生脚手架

## 脚手架说明

本目录包含 MediCare 智慧医疗门诊管理系统的**基础框架代码**，是老师分发给学生进行模块开发的起点。

**学生任务**：在本脚手架基础上，按照小组分工完成各自负责的功能模块。

---

## 脚手架包含的内容

| 目录/文件 | 说明 | 是否可修改 |
|-----------|------|-----------|
| `pom.xml` | Maven 构建配置（Java 17 + JavaFX 21 + 依赖） | ❌ 禁止 |
| `src/main/java/com/medicare/Main.java` | 应用入口，加载登录界面 | ❌ 禁止 |
| `src/main/java/com/medicare/Launcher.java` | Fat JAR 启动入口 | ❌ 禁止 |
| `src/main/java/com/medicare/model/` | 11 个实体 POJO | ❌ 禁止 |
| `src/main/java/com/medicare/dao/BaseDAO.java` | 泛型 DAO 基类 | ❌ 禁止 |
| `src/main/java/com/medicare/dao/JavaTimeBeanProcessor.java` | 时间类型处理器 | ❌ 禁止 |
| `src/main/java/com/medicare/util/ConnectionConfig.java` | HikariCP 连接池 | ❌ 禁止 |
| `src/main/java/com/medicare/controller/MainController.java` | 主界面导航框架 | ⚠️ 仅可添加注释，不可改路由 |
| `src/main/resources/css/` | 全局样式文件 | ⚠️ 可扩展，不可删除 |
| `src/main/resources/fxml/MainView.fxml` | 主界面布局 | ❌ 禁止 |
| `src/main/resources/logback.xml` | 日志配置 | ❌ 禁止 |
| `sql/schema_baseline.sql` | 数据库建表 + 初始数据 | ❌ 禁止 |

---

## 学生需要创建的内容

### A 组：患者管理
- `PatientDAO.java`
- `PatientService.java`
- `PatientController.java`
- `PatientDialogController.java`
- `PatientView.fxml`
- `PatientDialog.fxml`

### B 组：挂号预约
- `ScheduleDAO.java`（若老师未演示）
- `RegistrationDAO.java`
- `RegistrationService.java`
- `RegistrationController.java`
- `PatientSelectDialogController.java`
- `RegistrationView.fxml`
- `PatientSelectDialog.fxml`

### C 组：医生工作站 + 病历管理
- `MedicalRecordDAO.java`
- `MedicalRecordService.java`
- `DoctorWorkstationController.java`
- `MedicalRecordController.java`
- `MedicalRecordDetailDialogController.java`
- `DoctorWorkstationView.fxml`
- `MedicalRecordView.fxml`
- `MedicalRecordDetailDialog.fxml`

### D 组：药品库存 + 处方管理
- `MedicineDAO.java`（若老师未演示）
- `InventoryLogDAO.java`
- `PrescriptionDAO.java`
- `PrescriptionItemDAO.java`
- `MedicineService.java`
- `PrescriptionService.java`
- `PharmacyController.java`
- `MedicineDialogController.java`
- `StockDialogController.java`
- `PrescriptionController.java`
- `PharmacyView.fxml`
- `MedicineDialog.fxml`
- `StockDialog.fxml`
- `PrescriptionView.fxml`

---

## 快速开始

1. **初始化数据库**：
   ```bash
   mysql -u root -p < sql/schema_baseline.sql
   ```

2. **编译运行**：
   ```bash
   mvn clean javafx:run
   # 或打包后运行
   mvn clean package
   java -jar target/mediCare-1.0.0-SNAPSHOT.jar
   ```

3. **开发模块**：
   - 按小组分工创建对应的 Java 类和 FXML 文件
   - 遵循 DAO → Service → Controller → FXML 的开发顺序
   - 所有数据库操作必须在后台线程（Task）中执行

---

## 开发规范

1. **分层约束**：Controller 只能调用 Service，禁止直接调用 DAO
2. **事务控制**：事务必须在 Service 层，使用 `BaseDAO` 提供的事务方法
3. **UI 线程安全**：耗时操作使用 `Task`，UI 更新使用 `Platform.runLater()`
4. **SQL 规范**：SQL 定义为 `private static final String` 常量，禁止拼接
5. **Git 规范**：
   - 分支命名：`feature/模块名`（如 `feature/patient-mgmt`）
   - 提交信息：`feat: 新增患者管理模块` / `fix: 修复性别显示错误`

---

## 数据库连接配置

默认配置（如需修改请联系老师）：
- URL: `jdbc:mysql://localhost:3306/medicare`
- 用户名: `root`
- 密码: `mysql`
