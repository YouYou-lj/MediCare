# 任务书 D：药品库存 + 处方管理模块

> **负责小组**：D 组（3-5 人）  
> **依赖模块**：C 组（病历管理）  
> **技术要点**：操作列、库存事务（入库/出库）、处方明细表、复杂事务

---

## 一、功能需求

### 1.1 药品库存管理

#### 药品列表
- [ ] 全宽表格展示药品：名称、规格、单位、库存、安全库存、零售价、状态
- [ ] 低库存药品行标红（stock <= safetyStock）
- [ ] 操作列：编辑、删除、出入库

#### 新增/编辑药品
- [ ] 弹窗表单：名称*、规格、单位、库存、安全库存、有效期、批号、拼音简码、零售价、厂家、状态
- [ ] 名称+规格组合唯一性校验

#### 出入库
- [ ] 点击【出入库】按钮打开弹窗
- [ ] 类型选择：入库/出库/盘盈/盘亏
- [ ] 输入数量、批号、有效期（入库时）
- [ ] 保存时：更新药品库存 + 插入库存日志记录
- [ ] **必须在同一事务中执行**

#### 库存预警
- [ ] 顶部显示低库存和临期药品数量统计
- [ ] 支持筛选查看低库存/临期药品

### 1.2 处方管理

#### 处方开立
- [ ] 选择已完成的就诊记录（患者）
- [ ] 添加药品：搜索药品 → 输入数量 → 添加到处方明细列表
- [ ] 处方明细显示：药品名、规格、数量、单价、金额、用法用量
- [ ] 自动计算处方总金额
- [ ] 点击【保存处方】：
  - 插入处方主表
  - 插入处方明细表
  - 扣减药品库存
  - **必须在同一事务中执行**

#### 处方作废
- [ ] 选中处方，点击【作废】
- [ ] 恢复药品库存 + 删除明细 + 更新处方状态为"已作废"
- [ ] **必须在同一事务中执行**

#### 处方列表
- [ ] 显示所有处方：患者、医生、总金额、状态、创建时间
- [ ] 支持按患者搜索

---

## 二、数据库表

### 2.1 药品表 (medicine)

```sql
CREATE TABLE medicine (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    spec         VARCHAR(100),
    unit         VARCHAR(20),
    stock        INT UNSIGNED DEFAULT 0,
    safety_stock INT UNSIGNED DEFAULT 10,
    expiry_date  DATE,
    batch_no     VARCHAR(50),
    pinyin_code  VARCHAR(50),
    price        DECIMAL(10,2) DEFAULT 0.00,
    manufacturer VARCHAR(200),
    status       TINYINT DEFAULT 1,
    UNIQUE KEY uk_medicine_name_spec (name, spec)
);
```

### 2.2 库存日志表 (inventory_log)

```sql
CREATE TABLE inventory_log (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    medicine_id BIGINT UNSIGNED NOT NULL,
    type        TINYINT NOT NULL COMMENT '1-入库 2-出库 3-盘盈 4-盘亏',
    quantity    INT NOT NULL DEFAULT 0,
    batch_no    VARCHAR(50),
    expiry_date DATE,
    operator    VARCHAR(50),
    remark      VARCHAR(500),
    log_time    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
);
```

### 2.3 处方表 (prescription)

```sql
CREATE TABLE prescription (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    record_id    BIGINT UNSIGNED NOT NULL,
    patient_id   BIGINT UNSIGNED NOT NULL,
    doctor_id    BIGINT UNSIGNED NOT NULL,
    total_amount DECIMAL(10,2) DEFAULT 0.00,
    status       TINYINT DEFAULT 0 COMMENT '0-待缴费 1-已缴费 2-已取药 3-已作废'
);
```

### 2.4 处方明细表 (prescription_item)

```sql
CREATE TABLE prescription_item (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT UNSIGNED NOT NULL,
    medicine_id     BIGINT UNSIGNED NOT NULL,
    quantity        INT UNSIGNED NOT NULL DEFAULT 1,
    dosage          VARCHAR(200),
    usage_desc      VARCHAR(200),
    unit_price      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    amount          DECIMAL(10,2) NOT NULL DEFAULT 0.00
);
```

---

## 三、分层设计任务

### 3.1 DAO 层

#### MedicineDAO.java
- 标准 CRUD
- `updateStock(Connection, medicineId, delta)`：事务中更新库存
- `findLowStock()`：低库存查询
- `findNearExpiry()`：临期查询

#### InventoryLogDAO.java
- `insert(Connection, InventoryLog)`：事务中插入日志
- `findAll()` / `findByMedicine()` / `findByType()`：查询日志

#### PrescriptionDAO.java
- `insert(Connection, Prescription)`：事务中插入处方
- `updateStatus(Connection, id, status)`：事务中更新状态
- 多表 JOIN 查询（含 patientName、doctorName）

#### PrescriptionItemDAO.java
- `insert(Connection, PrescriptionItem)`：事务中插入明细
- `deleteByPrescription(Connection, prescriptionId)`：事务中删除明细
- `findByPrescription(prescriptionId)`：JOIN medicine 表查询明细

### 3.2 Service 层（核心：复杂事务）

#### MedicineService.java

**出入库事务**：
```java
public void stockIn(Long medicineId, Integer quantity, String batchNo, 
                    LocalDate expiryDate, String operator, String remark) throws SQLException {
    Connection conn = null;
    try {
        conn = medicineDAO.getConnection();
        conn.setAutoCommit(false);
        
        // 1. 更新药品库存
        medicineDAO.updateStock(conn, medicineId, quantity);
        
        // 2. 插入库存日志
        InventoryLog log = new InventoryLog();
        log.setMedicineId(medicineId);
        log.setType(1); // 入库
        log.setQuantity(quantity);
        log.setBatchNo(batchNo);
        log.setExpiryDate(expiryDate);
        log.setOperator(operator);
        log.setRemark(remark);
        inventoryLogDAO.insert(conn, log);
        
        medicineDAO.commit(conn);
    } catch (SQLException e) {
        medicineDAO.rollback(conn);
        throw e;
    } finally {
        medicineDAO.closeConnection(conn);
    }
}
```

#### PrescriptionService.java

**开立处方事务**（最复杂）：
```java
public Long createPrescription(Prescription prescription, 
                                List<PrescriptionItem> items) throws SQLException {
    Connection conn = null;
    try {
        conn = prescriptionDAO.getConnection();
        conn.setAutoCommit(false);
        
        // 1. 插入处方主表
        Long prescriptionId = prescriptionDAO.insert(conn, prescription);
        
        // 2. 插入明细 + 扣减库存
        BigDecimal total = BigDecimal.ZERO;
        for (PrescriptionItem item : items) {
            item.setPrescriptionId(prescriptionId);
            // 计算金额
            BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setAmount(amount);
            total = total.add(amount);
            
            prescriptionItemDAO.insert(conn, item);
            
            // 扣减库存（事务连接）
            medicineDAO.updateStock(conn, item.getMedicineId(), -item.getQuantity());
        }
        
        // 3. 更新处方总金额
        prescription.setTotalAmount(total);
        // ... 如有需要更新主表金额字段
        
        prescriptionDAO.commit(conn);
        return prescriptionId;
    } catch (SQLException e) {
        prescriptionDAO.rollback(conn);
        throw e;
    } finally {
        prescriptionDAO.closeConnection(conn);
    }
}
```

**作废处方事务**：
```java
public void cancelPrescription(Long prescriptionId) throws SQLException {
    Connection conn = null;
    try {
        conn = prescriptionDAO.getConnection();
        conn.setAutoCommit(false);
        
        // 1. 查询明细，恢复库存
        List<PrescriptionItem> items = prescriptionItemDAO.findByPrescription(prescriptionId);
        for (PrescriptionItem item : items) {
            medicineDAO.updateStock(conn, item.getMedicineId(), item.getQuantity());
        }
        
        // 2. 删除明细
        prescriptionItemDAO.deleteByPrescription(conn, prescriptionId);
        
        // 3. 更新处方状态为已作废
        prescriptionDAO.updateStatus(conn, prescriptionId, 3);
        
        prescriptionDAO.commit(conn);
    } catch (SQLException e) {
        prescriptionDAO.rollback(conn);
        throw e;
    } finally {
        prescriptionDAO.closeConnection(conn);
    }
}
```

### 3.3 Controller 层

#### PharmacyController.java
- 使用 `list-view-template.fxml`
- 操作列：三个按钮（编辑/删除/出入库）
- 低库存行红色提示：`setStyle("-fx-text-fill: #e74c3c;")`
- 出入库按钮打开 `StockDialog`

#### MedicineDialogController.java
- 新增/编辑药品弹窗
- 表单字段较多，注意布局

#### StockDialogController.java
- 出入库弹窗
- 类型 ComboBox（1=入库, 2=出库, 3=盘盈, 4=盘亏）
- 数量 Spinner（出库时为负数）

#### PrescriptionController.java
- 上方：患者选择（已完成就诊的患者列表）
- 中间：处方明细表格（可添加/删除药品）
- 下方：总金额 + 保存按钮
- 添加药品：搜索药品 → 选择 → 输入数量/用法 → 添加

### 3.4 FXML 设计

| 文件 | 布局 | 说明 |
|------|------|------|
| `PharmacyView.fxml` | list-view-template | 药品列表 + 操作列 |
| `MedicineDialog.fxml` | dialog-form-template | 药品表单弹窗 |
| `StockDialog.fxml` | dialog-form-template | 出入库弹窗 |
| `PrescriptionView.fxml` | 自定义 | 患者选择 + 明细表格 + 总金额 |

---

## 四、与其他模块的接口约定

### 4.1 依赖 C 组（病历管理）

```java
// C 组提供的方法
MedicalRecordService.listByPatient(Long patientId)
    // 查看患者历史病历
```

### 4.2 无外部依赖的独立模块

药品库存模块不依赖其他学生模块，可独立开发。

---

## 五、Git 分支

```bash
git checkout -b feature/pharmacy-prescription
git commit -m "feat: 完成药品库存 + 处方管理模块"
git push origin feature/pharmacy-prescription
```

---

## 六、验收标准

### 功能验收
- [ ] 药品列表正确显示，低库存标红
- [ ] 能新增、编辑、删除药品
- [ ] 出入库操作正确更新库存和日志
- [ ] 出入库在同一事务中执行
- [ ] 低库存/临期预警功能正常
- [ ] 处方开立：选患者 → 添加药品 → 保存 → 扣减库存
- [ ] 处方明细正确计算金额
- [ ] 处方作废：恢复库存 + 更新状态
- [ ] 处方操作在同一事务中执行
- [ ] 库存不足时处方保存失败并提示

### 事务验收（重点）
- [ ] 出入库：更新库存 + 插入日志在同一事务
- [ ] 开立处方：插入主表 + 插入明细 + 扣减库存在同一事务
- [ ] 作废处方：恢复库存 + 删除明细 + 更新状态在同一事务
- [ ] 并发情况下库存不会变成负数

### 代码规范
- [ ] 所有事务方法传入 Connection
- [ ] Service 层统一控制事务边界
- [ ] 操作列按钮样式统一
