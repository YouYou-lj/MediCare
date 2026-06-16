# 任务书：基础数据管理（老师演示模块）

> **目标**：老师带领学生完成一个完整的 CRUD 模块演示，涵盖 DAO → Service → Controller → FXML 的全链路。  
> **模块**：科室管理 + 医生管理 + 排班管理  
> **课时**：课时 5-6（共 90 分钟）

---

## 一、功能需求

### 1.1 科室管理
- [ ] 科室列表展示（TableView）
- [ ] 新增科室（右侧表单）
- [ ] 编辑科室（选中左侧行，右侧表单填充）
- [ ] 删除科室（删除前检查是否有医生）
- [ ] 按名称搜索

### 1.2 医生管理
- [ ] 医生列表展示（TableView，显示科室名称）
- [ ] 新增/编辑/删除医生
- [ ] 按姓名搜索、按科室筛选
- [ ] 职称下拉选择（主任医师/副主任医师/主治医师/医师）

### 1.3 排班管理
- [ ] 排班列表展示（TableView，显示医生名和科室名）
- [ ] 新增/编辑/删除排班
- [ ] 按日期和科室筛选
- [ ] 时段下拉选择（上午/下午/晚上）
- [ ] 总号源数用 Spinner 控件

---

## 二、数据库表

> 已在 `schema_baseline.sql` 中建表并初始化数据，学生不需要手动建表。

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `department` | 科室表 | id, name, location, phone |
| `doctor` | 医生表 | id, name, department_id, title, status |
| `schedule` | 排班表 | id, doctor_id, work_date, time_slot, total_slots, remain_slots |

---

## 三、分层设计任务

### 3.1 DAO 层（课时 5 上半节，20min）

老师先讲解 `BaseDAO` 的使用，然后带领学生手写 `DepartmentDAO`：

```java
public class DepartmentDAO extends BaseDAO<Department> {
    private static final String SQL_INSERT = "INSERT INTO department (...) VALUES (...)";
    // ... 其他 SQL 常量
    
    public Long insert(Department dept) throws SQLException {
        return executeInsert(SQL_INSERT, dept.getName(), ...);
    }
    // ... CRUD 方法
}
```

**学生跟着完成**：
- `DoctorDAO.java`（含 JOIN 查询获取 departmentName）
- `ScheduleDAO.java`（含三表 JOIN 获取 doctorName + departmentName）

### 3.2 Service 层（课时 5 下半节，15min）

老师讲解 Service 职责，带领学生完成 `DepartmentService`：
- 参数校验（名称非空）
- 唯一性校验（名称不能重复）
- 调用 DAO 完成业务

**学生跟着完成**：
- `DoctorService.java`
- `ScheduleService.java`

### 3.3 Controller + FXML（课时 6，45min）

老师演示：
1. 用 SceneBuilder 打开 `tab-pane-template.fxml`，修改为三 Tab 布局
2. 讲解 `BasicDataController.java` 的结构：
   - 每个 Tab 对应一组 TableView + 表单
   - 选中行事件填充表单
   - 保存/删除按钮事件
3. 运行调试

**学生跟着完成**：三个 Tab 的完整交互逻辑。

---

## 四、界面布局选型

| 功能 | 布局 | 模板参考 |
|------|------|----------|
| 基础数据整体 | TabPane | `tab-pane-template.fxml` |
| 每个 Tab 内部 | SplitPane（左表格右表单） | 内置在 TabPane 模板中 |
| 科室/医生/排班弹窗 | 无（使用 SplitPane 右侧表单直接编辑） | — |

---

## 五、与其他模块的接口约定

| 提供者 | 接口 | 消费者 |
|--------|------|--------|
| 本模块 | `DepartmentService.findAll()` | B组（挂号预约科室下拉） |
| 本模块 | `DoctorService.findByDepartment(deptId)` | B组（挂号预约医生筛选） |
| 本模块 | `ScheduleService.findByDate(date)` | B组（挂号预约号源列表） |
| 本模块 | `DoctorService.findAll()` | C组（医生工作站医生选择） |

---

## 六、验收标准

### 功能验收
- [ ] 能正确显示科室/医生/排班列表
- [ ] 能新增、编辑、删除科室
- [ ] 删除科室前检查是否有医生关联
- [ ] 医生列表显示科室名称（JOIN 查询）
- [ ] 排班列表显示医生名和科室名（三表 JOIN）
- [ ] 搜索和筛选功能正常

### 代码规范
- [ ] DAO 继承 `BaseDAO<Department>` 等
- [ ] SQL 定义为常量
- [ ] Service 层有参数校验
- [ ] Controller 使用 `Task` 进行后台操作

---

## 七、老师参考代码位置

完整实现位于项目源码：
- `src/main/java/com/medicare/dao/DepartmentDAO.java`
- `src/main/java/com/medicare/dao/DoctorDAO.java`
- `src/main/java/com/medicare/dao/ScheduleDAO.java`
- `src/main/java/com/medicare/service/DepartmentService.java`
- `src/main/java/com/medicare/service/DoctorService.java`
- `src/main/java/com/medicare/service/ScheduleService.java`
- `src/main/java/com/medicare/controller/BasicDataController.java`
- `src/main/resources/fxml/BasicDataView.fxml`
