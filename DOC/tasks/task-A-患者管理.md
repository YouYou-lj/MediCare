# 任务书 A：患者管理模块

> **负责小组**：A 组（3-5 人）  
> **模块定位**：核心业务模块，无外部依赖，其他模块的基础数据来源  
> **技术要点**：TableView + Dialog 弹窗复用、性别格式化、后台线程、搜索

---

## 一、功能需求

### 1.1 患者列表
- [ ] 全宽表格展示所有患者（按建档时间倒序）
- [ ] 表格列：ID、身份证号、姓名、性别、出生日期、手机号、住址、建档时间
- [ ] 性别列显示"女性"/"男性"（0=女，1=男）
- [ ] 建档时间格式化为 `yyyy-MM-dd HH:mm`
- [ ] 操作列：编辑按钮 + 删除按钮
- [ ] 底部显示记录总数

### 1.2 新增患者
- [ ] 点击【新建档案】打开弹窗 Dialog
- [ ] 弹窗表单：身份证号*、姓名*、性别（ComboBox）、出生日期、电话、地址、过敏信息
- [ ] 身份证号必填，格式校验（18位）
- [ ] 姓名必填
- [ ] 性别默认选中"男性"
- [ ] 保存时检查身份证号唯一性
- [ ] 保存成功关闭弹窗并刷新列表

### 1.3 编辑患者
- [ ] 点击行内【编辑】按钮打开弹窗
- [ ] 弹窗预填充当前患者数据
- [ ] 身份证号唯一性校验（排除自身）
- [ ] 保存成功刷新列表

### 1.4 删除患者
- [ ] 点击行内【删除】按钮弹出确认框
- [ ] 确认后后台删除并刷新列表
- [ ] 删除失败时提示错误信息

### 1.5 搜索
- [ ] 顶部搜索框输入姓名/手机号/身份证号关键字
- [ ] 点击【查询】按姓名模糊查询，无结果则按手机号查询
- [ ] 点击【刷新】清空搜索条件并重新加载全部

---

## 二、数据库表

> 表已存在，直接使用。详见 `schema_baseline.sql`。

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

## 三、分层设计任务

### 3.1 DAO 层（1 人负责）

创建 `PatientDAO.java`，继承 `BaseDAO<Patient>`。

**必须实现的方法**：

| 方法 | 说明 |
|------|------|
| `Long insert(Patient)` | 新增患者，返回主键 |
| `int update(Patient)` | 更新患者 |
| `int delete(Long id)` | 删除患者 |
| `Patient findById(Long id)` | 按 ID 查询 |
| `Patient findByIdCard(String idCard)` | 按身份证号查询 |
| `List<Patient> findAll()` | 查询全部（倒序） |
| `List<Patient> findByName(String name)` | 按姓名模糊查询 |
| `List<Patient> findByPhone(String phone)` | 按手机号模糊查询 |
| `boolean existsByIdCard(String idCard)` | 检查身份证号是否存在 |
| `boolean existsByIdCard(String idCard, Long excludeId)` | 编辑时排除自身检查 |

**SQL 参考**：见 `DOC/SQL参考.md` → 二、患者管理模块

### 3.2 Service 层（1 人负责）

创建 `PatientService.java`。

**必须实现的方法**：

| 方法 | 说明 |
|------|------|
| `Long registerPatient(Patient)` | 建档：校验 → 唯一性 → 插入 |
| `void updatePatient(Patient)` | 更新：校验 → 唯一性（排除自身）→ 更新 |
| `void deletePatient(Long id)` | 删除患者 |
| `Patient getById(Long id)` | 按 ID 查询 |
| `List<Patient> listAll()` | 查询全部 |
| `List<Patient> search(String keyword)` | 搜索：先按姓名，无结果按手机号 |

**业务规则**：
- 身份证号必填，18位格式校验（正则）
- 姓名必填
- 性别必填
- 手机号如有输入，需校验格式（1开头11位）
- 身份证号全局唯一

### 3.3 Controller 层（2 人负责）

#### PatientController.java
- 实现 `Initializable`
- 使用 `list-view-template.fxml` 布局
- 表格初始化：声明正确的泛型类型！
  ```java
  @FXML private TableColumn<Patient, Long> colId;
  @FXML private TableColumn<Patient, String> colIdCard, colName;
  @FXML private TableColumn<Patient, Integer> colGender;  // 注意泛型是 Integer
  @FXML private TableColumn<Patient, LocalDate> colBirthDate;
  @FXML private TableColumn<Patient, LocalDateTime> colCreateTime;
  @FXML private TableColumn<Patient, Void> colAction;  // 操作列
  ```
- 性别列自定义 CellFactory：`gender == 0 ? "女性" : "男性"`
- 操作列自定义 CellFactory：HBox 内嵌两个 Button（编辑/删除）
- 所有数据加载使用 `Task` + `Platform.runLater()`
- 编辑/删除按钮获取当前行：`getTableView().getItems().get(getIndex())`

#### PatientDialogController.java
- 弹窗 Controller，不需要实现 Initializable
- 提供 `setPatient(Patient)` 方法：编辑时预填充，新增时传 null
- 提供 `Patient getPatient()` 方法：
  - 读取表单数据，构建 Patient 对象
  - 校验失败返回 null，成功返回 Patient
  - 性别从 ComboBox 的 selectedIndex 获取（0=女，1=男）
- 弹窗关闭时如果返回 null，需在 OK 按钮事件拦截器中 `event.consume()`

### 3.4 FXML 设计（1 人负责，可与 Controller 配合）

| 文件 | 布局模板 | 说明 |
|------|----------|------|
| `PatientView.fxml` | `list-view-template.fxml` | 全宽表格 + 工具栏 |
| `PatientDialog.fxml` | `dialog-form-template.fxml` | 患者表单弹窗 |

---

## 四、弹窗复用设计

`PatientDialog` 需要被两个地方调用：
1. **患者管理模块**（A 组自己的模块）：新增/编辑患者
2. **挂号预约模块**（B 组）：挂号时新增患者

**接口约定**：
```java
// PatientDialogController 必须提供的接口
public void setPatient(Patient patient);  // null = 新增模式
public Patient getPatient();  // 返回 null 表示校验失败
```

B 组会依赖此接口，开发前请与 B 组确认。

---

## 五、Git 分支

```bash
# 从 main 创建特性分支
git checkout -b feature/patient-mgmt

# 开发完成后提交
git add .
git commit -m "feat: 完成患者管理模块（DAO + Service + Controller + FXML）"

# 推送到远程
git push origin feature/patient-mgmt

# 发起 Pull Request 请求合并到 main
```

---

## 六、验收标准

### 功能验收（必须全部通过）
- [ ] 能正确显示患者列表（含性别格式化、时间格式化）
- [ ] 操作列显示【编辑】和【删除】按钮
- [ ] 点击【新建档案】打开弹窗，能成功新增患者
- [ ] 新增时身份证号重复有明确错误提示
- [ ] 点击【编辑】打开弹窗，数据预填充正确
- [ ] 编辑后列表实时刷新
- [ ] 点击【删除】有确认框，确认后删除并刷新
- [ ] 搜索功能按姓名/手机号正确过滤
- [ ] 界面无卡死，所有操作响应流畅（使用 Task）

### 代码规范
- [ ] DAO 继承 BaseDAO，SQL 定义为常量
- [ ] Service 有业务校验和日志记录
- [ ] Controller 使用 Task 模式，不在 UI 线程执行 SQL
- [ ] TableColumn 泛型声明与属性类型一致
- [ ] FXML 中 fx:controller 指向正确的类

### 集成验收
- [ ] B 组能通过 `PatientService` 调用患者查询接口
- [ ] B 组能正确复用 `PatientDialog` 弹窗
