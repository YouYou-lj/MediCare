# 任务书 B：挂号预约模块

> **负责小组**：B 组（3-5 人）  
> **依赖模块**：A 组（患者管理）+ 老师演示（基础数据）  
> **技术要点**：事务控制、弹窗联动、PatientSelectDialog、号源状态管理

---

## 一、功能需求

### 1.1 号源查询
- [ ] 选择日期 + 科室，查询可用号源列表
- [ ] 号源列表显示：医生、科室、时段、总号源、剩余号源
- [ ] 剩余号源为 0 的行灰色显示或不可选

### 1.2 挂号
- [ ] 选中号源，点击【挂号】按钮
- [ ] 弹出 `PatientSelectDialog`（患者选择弹窗）
- [ ] 弹窗内可搜索患者、选择患者、或【新增患者】（调用 A 组的 PatientDialog）
- [ ] 选中患者后点击【确定】完成挂号
- [ ] 挂号成功后：
  - 号源剩余数减 1
  - 生成挂号记录（状态=候诊，分配序号）
  - 挂号费固定 10.00 元
  - 刷新号源列表和挂号记录列表

### 1.3 今日挂号记录
- [ ] 右侧显示今日挂号记录列表
- [ ] 记录显示：序号、患者姓名、医生、科室、时段、状态、挂号时间
- [ ] 状态列带颜色：候诊(黄色)、就诊中(蓝色加粗)、已完成(绿色)、已取消(灰色)
- [ ] 状态筛选按钮：全部/候诊/就诊中/已完成

### 1.4 叫号
- [ ] 选中候诊记录，点击【叫号】
- [ ] 状态变为"就诊中"

### 1.5 完成就诊
- [ ] 选中就诊中记录，点击【完成】
- [ ] 状态变为"已完成"

### 1.6 取消挂号
- [ ] 选中记录，点击【取消】
- [ ] 确认后：释放号源（剩余+1）+ 删除挂号记录
- [ ] **必须在同一事务中执行**

---

## 二、数据库表

### 2.1 挂号表 (registration)

```sql
CREATE TABLE registration (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id  BIGINT UNSIGNED NOT NULL,
    schedule_id BIGINT UNSIGNED NOT NULL,
    reg_time    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    status      TINYINT DEFAULT 0 COMMENT '0-候诊 1-就诊中 2-已完成 3-已取消',
    seq_no      INT UNSIGNED COMMENT '序号',
    fee         DECIMAL(10,2) DEFAULT 0.00,
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_reg_patient  FOREIGN KEY (patient_id)  REFERENCES patient(id),
    CONSTRAINT fk_reg_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id)
);
```

### 2.2 排班表 (schedule)

> 由老师演示模块提供，B 组直接使用。关键字段：`remain_slots`（剩余号源）。

---

## 三、分层设计任务

### 3.1 DAO 层

#### ScheduleDAO（如果老师未演示完，需补充）
- `findByDate(LocalDate)`：按日期查询排班（含 JOIN 医生/科室）
- `decrementRemain(Long scheduleId)`：扣减号源，返回影响行数（0=号源不足）
- `incrementRemain(Long scheduleId)`：释放号源

#### RegistrationDAO（重点）

创建 `RegistrationDAO.java`，继承 `BaseDAO<Registration>`。

**注意：insert/updateStatus/delete 需要事务版本（传入 Connection）**

| 方法 | 事务 | 说明 |
|------|------|------|
| `Long insert(Connection, Registration)` | ✅ | 事务中插入挂号记录 |
| `int updateStatus(Connection, Long, Integer)` | ✅ | 事务中更新状态 |
| `int delete(Connection, Long)` | ✅ | 事务中删除记录 |
| `int updateStatus(Long, Integer)` | ❌ | 非事务版本（叫号/完成用） |
| `int delete(Long)` | ❌ | 非事务版本 |
| `Registration findById(Long)` | — | 按 ID 查询（多表 JOIN） |
| `List<Registration> findToday(LocalDate)` | — | 今日挂号记录（多表 JOIN） |
| `List<Registration> findByStatus(Integer)` | — | 按状态查询 |
| `List<Registration> findWaitingByDoctor(Long, LocalDate)` | — | 某医生今日候诊/就诊中 |
| `Integer getMaxSeqNo(LocalDate)` | — | 今日最大序号 |

**⚠️ 类型安全警告**：`getMaxSeqNo` 使用 `ScalarHandler`，MySQL `INT UNSIGNED` 返回 `Long`。请用 `Number` 接收后 `.intValue()`：
```java
public Integer getMaxSeqNo(LocalDate date) throws SQLException {
    Number max = queryScalar(SQL_MAX_SEQ_NO, date);
    return max != null ? max.intValue() : 0;
}
```

### 3.2 Service 层（核心难点：事务控制）

创建 `RegistrationService.java`。

#### 挂号事务（重点）
```java
public Long register(Registration reg) throws SQLException {
    Connection conn = null;
    try {
        conn = registrationDAO.getConnection();
        conn.setAutoCommit(false);
        
        // 1. 扣减号源（乐观锁：WHERE remain_slots > 0）
        int affected = scheduleDAO.decrementRemain(reg.getScheduleId());
        if (affected == 0) {
            throw new SQLException("号源不足");
        }
        
        // 2. 分配序号
        Integer seqNo = registrationDAO.getMaxSeqNo(LocalDate.now()) + 1;
        reg.setSeqNo(seqNo);
        reg.setStatus(0); // 候诊
        reg.setFee(BigDecimal.valueOf(10.00));
        
        // 3. 插入挂号记录（事务连接）
        Long id = registrationDAO.insert(conn, reg);
        
        registrationDAO.commit(conn);
        return id;
    } catch (SQLException e) {
        registrationDAO.rollback(conn);
        throw e;
    } finally {
        registrationDAO.closeConnection(conn);
    }
}
```

#### 取消挂号事务（重点）
```java
public void cancelRegistration(Long regId) throws SQLException {
    // 先查询获取 schedule_id
    Registration reg = registrationDAO.findById(regId);
    Connection conn = null;
    try {
        conn = registrationDAO.getConnection();
        conn.setAutoCommit(false);
        
        // ⚠️ 注意：scheduleDAO.incrementRemain 也必须使用同一个 conn！
        // 但 scheduleDAO 默认方法使用自动获取的连接，不在事务中
        // 解决方案：在 Service 层直接用 conn 执行 UPDATE
        
        registrationDAO.delete(conn, regId);
        registrationDAO.commit(conn);
    } catch (SQLException e) {
        registrationDAO.rollback(conn);
        throw e;
    } finally {
        registrationDAO.closeConnection(conn);
    }
}
```

#### 叫号 / 完成
- `void callPatient(Long regId)`：更新 status = 1（就诊中）
- `void completeRegistration(Long regId)`：更新 status = 2（已完成）

### 3.3 Controller 层

#### RegistrationController.java
- 使用 `split-pane-template.fxml` 布局
- 左侧：号源列表（Schedule），右侧：挂号记录（Registration）
- 号源列表选中后，【挂号】按钮才可用
- 挂号按钮事件：打开 `PatientSelectDialog`

#### PatientSelectDialogController.java（重点）
- 弹窗功能：搜索患者、显示患者列表、选择患者、新增患者
- 【新增患者】按钮：打开 `PatientDialog`（复用 A 组的弹窗）
  ```java
  // 打开 PatientDialog
  FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PatientDialog.fxml"));
  DialogPane pane = loader.load();
  PatientDialogController controller = loader.getController();
  controller.setPatient(null); // 新增模式
  // ... 处理保存逻辑
  ```
- 提供 `Patient getSelectedPatient()` 方法供 RegistrationController 调用

### 3.4 FXML 设计

| 文件 | 布局模板 | 说明 |
|------|----------|------|
| `RegistrationView.fxml` | `split-pane-template.fxml` | 左右分割（号源+挂号记录） |
| `PatientSelectDialog.fxml` | 自定义 DialogPane | 患者搜索 + 表格 + 新增按钮 |

---

## 四、与其他模块的接口约定

### 4.1 依赖 A 组（患者管理）

```java
// A 组提供的接口
PatientService.listAll()           // 加载全部患者列表
PatientService.search(keyword)     // 搜索患者
PatientDialogController.setPatient(null)   // 新增患者弹窗
PatientDialogController.getPatient()       // 获取表单数据
```

### 4.2 提供给 C 组（医生工作站）

```java
// B 组需要提供的方法
RegistrationService.findWaitingByDoctor(Long doctorId, LocalDate date)
    // 返回某医生今日候诊和就诊中的挂号记录
RegistrationService.callPatient(Long regId)
    // 叫号，状态变为"就诊中"
RegistrationService.completeRegistration(Long regId)
    // 完成就诊，状态变为"已完成"
```

### 4.3 依赖老师演示（基础数据）

```java
// 老师模块提供的接口
ScheduleService.findByDate(LocalDate date)
    // 按日期查询号源
DepartmentService.findAll()
    // 科室下拉列表
```

---

## 五、Git 分支

```bash
git checkout -b feature/registration
git commit -m "feat: 完成挂号预约模块（含事务控制）"
git push origin feature/registration
```

---

## 六、验收标准

### 功能验收
- [ ] 能按日期+科室查询号源列表
- [ ] 号源剩余为 0 时挂号按钮禁用或提示
- [ ] 挂号流程：选号源 → 选患者 → 确认 → 成功
- [ ] 挂号成功后号源数减 1，记录出现在右侧列表
- [ ] 序号自动递增（当日从 1 开始）
- [ ] 叫号/完成/取消操作正常，状态正确变化
- [ ] 取消挂号后号源数恢复
- [ ] 状态列带颜色显示
- [ ] 状态筛选按钮正常工作
- [ ] PatientSelectDialog 能正确调用 PatientDialog 新增患者

### 事务验收（重点）
- [ ] 挂号时扣减号源和插入记录在同一事务
- [ ] 取消挂号时释放号源和删除记录在同一事务
- [ ] 并发挂号时不会超卖（WHERE remain_slots > 0）

### 代码规范
- [ ] DAO 事务方法传入 Connection 参数
- [ ] Service 层统一控制事务（commit/rollback/finally close）
- [ ] 所有数据库操作在 Task 中执行
