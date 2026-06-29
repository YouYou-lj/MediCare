# 药品库存与处方管理模块

> **所属系统**: MediCare 智慧医疗门诊管理系统
> **文档类型**: RAG 检索文档 — 药品库存与处方
> **前端页面**: `/pharmacy` (MedicineList.vue), `/prescriptions` (PrescriptionView.vue)
> **后端 Controller**: `MedicineController`, `PrescriptionController`
> **角色权限**: admin, doctor, pharmacist

---

## 1. 模块概述

药品库存与处方管理是门诊系统的**药房业务核心模块**，覆盖药品基础信息管理、库存出入库、库存预警、处方开立与处方管理全流程。处方开立与药品库存扣减必须保证事务一致性。

---

## 2. 功能清单

### 2.1 药品库存管理

| 功能 | 说明 | 角色 |
|------|------|------|
| 药品列表 | 全宽表格展示药品信息 | admin, doctor, pharmacist |
| 新增药品 | 弹窗录入药品信息 | admin, pharmacist |
| 编辑药品 | 修改药品属性 | admin, pharmacist |
| 删除药品 | 删除药品记录 | admin |
| 出入库 | 入库/出库/盘盈/盘亏操作 | admin, pharmacist |
| 库存预警 | 低库存（stock <= safety_stock）和临期药品标红 | admin, pharmacist |
| 库存日志 | 查看药品的库存变动历史 | admin, pharmacist |

### 2.2 处方管理

| 功能 | 说明 | 角色 |
|------|------|------|
| 处方列表 | 展示所有处方记录 | admin, doctor, pharmacist |
| 处方开立 | 选择已完成就诊的患者，添加药品明细 | admin, doctor |
| 处方作废 | 恢复库存 + 更新状态为"已作废" | admin, doctor |
| 处方搜索 | 按患者姓名搜索 | admin, doctor, pharmacist |

---

## 3. 药品库存页面 (MedicineList.vue)

### 3.1 页面布局

- 顶部: `DataToolbar`（搜索 + 刷新 + 新增）+ 库存预警统计
- 中部: `el-table` 药品表格
- 操作列: 编辑 / 删除 / 出入库

### 3.2 表格字段

| 字段 | 说明 | 预警 |
|------|------|------|
| 名称 | medicine.name | — |
| 规格 | medicine.spec | — |
| 单位 | medicine.unit | — |
| 库存 | medicine.stock | `<= safety_stock` 时标红 |
| 安全库存 | medicine.safety_stock | — |
| 有效期 | medicine.expiry_date | 临期标红 |
| 零售价 | medicine.price | — |
| 状态 | medicine.status | — |
| 操作 | 编辑/删除/出入库 | — |

### 3.3 新增/编辑药品弹窗

表单字段:
- 名称*（必填）
- 规格
- 单位
- 库存（默认0）
- 安全库存（默认10）
- 有效期
- 批号
- 拼音简码
- 零售价
- 生产厂家
- 状态（正常/停用）

**唯一约束**: 名称 + 规格组合唯一。

### 3.4 出入库弹窗

- 类型选择: 入库(1) / 出库(2) / 盘盈(3) / 盘亏(4)
- 数量输入（出库时系统校验库存是否充足）
- 批号（入库时）
- 有效期（入库时）
- 操作人
- 备注

**事务保证**: 更新药品库存 + 插入库存日志记录在同一事务中执行。

---

## 4. 处方管理页面 (PrescriptionView.vue)

### 4.1 页面布局

- 上方: 患者选择（已完成的就诊记录）
- 中间: 处方明细表格（可添加/删除药品）
- 下方: 总金额 + 保存按钮

### 4.2 处方开立流程

1. 选择已完成的就诊记录（患者）
2. 搜索药品 → 选择药品 → 输入数量/用法/剂量 → 添加到处方明细
3. 处方明细列表显示: 药品名、规格、数量、单价、金额、用法用量
4. 系统自动计算处方总金额
5. 点击【保存处方】:
   - 插入处方主表
   - 插入处方明细表
   - 扣减药品库存
   - **必须在同一事务中执行**

### 4.3 处方状态

| 状态 | 值 | 说明 |
|------|-----|------|
| 待缴费 | 0 | 刚开立，未缴费 |
| 已缴费 | 1 | 患者已缴费 |
| 已取药 | 2 | 药房已发药 |
| 已作废 | 3 | 处方作废 |

### 4.4 处方作废

1. 选中处方，点击【作废】
2. 系统：
   - 恢复药品库存（按明细数量逐一加回）
   - 删除处方明细
   - 更新处方状态为"已作废"(3)
   - **必须在同一事务中执行**

---

## 5. 后端实现

### 5.1 MedicineController

```java
@RestController
@RequestMapping("/api/medicines")
@Tag(name = "药品管理", description = "药品库存与出入库管理")
public class MedicineController {

    @GetMapping
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<List<Medicine>> list()           // 查询全部药品

    @GetMapping("/search")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<List<Medicine>> search(@RequestParam String keyword)
        // 按名称/拼音简码搜索

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<Medicine> detail(@PathVariable Long id)   // 查看详情

    @PostMapping
    @RequireRole({"admin", "pharmacist"})
    public Result<Medicine> create(@RequestBody Medicine medicine)  // 新增药品

    @PutMapping("/{id}")
    @RequireRole({"admin", "pharmacist"})
    public Result<Medicine> update(@PathVariable Long id, @RequestBody Medicine medicine)
        // 更新药品

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id)    // 删除药品

    @PostMapping("/{id}/stock")
    @RequireRole({"admin", "pharmacist"})
    public Result<InventoryLogVO> stockOperation(@PathVariable Long id, @RequestBody StockRequest request)
        // 出入库操作

    @GetMapping("/low-stock")
    @RequireRole({"admin", "pharmacist"})
    public Result<List<Medicine>> lowStock()   // 低库存药品

    @GetMapping("/near-expiry")
    @RequireRole({"admin", "pharmacist"})
    public Result<List<Medicine>> nearExpiry()  // 临期药品
}
```

### 5.2 PrescriptionController

```java
@RestController
@RequestMapping("/api/prescriptions")
@Tag(name = "处方管理", description = "处方开立与处方管理")
public class PrescriptionController {

    @GetMapping
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<List<PrescriptionVO>> list()    // 查询全部处方

    @GetMapping("/search")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<List<PrescriptionVO>> search(@RequestParam String keyword)
        // 按患者搜索

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<PrescriptionVO> detail(@PathVariable Long id)   // 查看详情

    @PostMapping
    @RequireRole({"admin", "doctor"})
    public Result<PrescriptionVO> create(@RequestBody PrescriptionCreateRequest request)
        // 开立处方

    @PostMapping("/{id}/cancel")
    @RequireRole({"admin", "doctor"})
    public Result<Void> cancel(@PathVariable Long id)   // 作废处方

    @PostMapping("/{id}/dispense")
    @RequireRole({"admin", "pharmacist"})
    public Result<Void> dispense(@PathVariable Long id)   // 配药发药
}
```

### 5.3 核心 Service 逻辑

#### 出入库事务 (MedicineService)

```java
@Transactional
public void stockOperation(Long medicineId, StockRequest request) {
    Medicine medicine = medicineRepository.findById(medicineId)
        .orElseThrow(() -> new BusinessException("药品不存在"));

    // 1. 校验出库库存
    if (request.getType() == InventoryLog.TYPE_STOCK_OUT) {
        if (medicine.getStock() < request.getQuantity()) {
            throw new BusinessException("库存不足");
        }
    }

    // 2. 更新库存
    int delta = (request.getType() == InventoryLog.TYPE_STOCK_OUT || request.getType() == InventoryLog.TYPE_LOSS)
        ? -request.getQuantity() : request.getQuantity();
    medicine.setStock(medicine.getStock() + delta);
    medicineRepository.save(medicine);

    // 3. 记录日志
    InventoryLog log = new InventoryLog();
    log.setMedicineId(medicineId);
    log.setType(request.getType());
    log.setQuantity(request.getQuantity());
    log.setBatchNo(request.getBatchNo());
    log.setExpiryDate(request.getExpiryDate());
    log.setOperator(request.getOperator());
    log.setRemark(request.getRemark());
    inventoryLogRepository.save(log);
}
```

#### 开立处方事务 (PrescriptionService)

```java
@Transactional
public Prescription createPrescription(PrescriptionCreateRequest request) {
    // 1. 校验病历
    MedicalRecord record = medicalRecordRepository.findById(request.getRecordId())
        .orElseThrow(() -> new BusinessException("病历不存在"));

    // 2. 创建处方主表
    Prescription prescription = new Prescription();
    prescription.setRecordId(request.getRecordId());
    prescription.setPatientId(record.getPatientId());
    prescription.setDoctorId(record.getDoctorId());
    prescription.setStatus(Prescription.STATUS_PENDING);
    prescriptionRepository.save(prescription);

    // 3. 插入明细 + 扣减库存
    BigDecimal total = BigDecimal.ZERO;
    for (PrescriptionItemVO itemVO : request.getItems()) {
        Medicine medicine = medicineRepository.findById(itemVO.getMedicineId())
            .orElseThrow(() -> new BusinessException("药品不存在"));

        if (medicine.getStock() < itemVO.getQuantity()) {
            throw new BusinessException("药品【" + medicine.getName() + "】库存不足");
        }

        // 扣减库存
        medicine.setStock(medicine.getStock() - itemVO.getQuantity());
        medicineRepository.save(medicine);

        // 创建明细
        PrescriptionItem item = new PrescriptionItem();
        item.setPrescriptionId(prescription.getId());
        item.setMedicineId(itemVO.getMedicineId());
        item.setQuantity(itemVO.getQuantity());
        item.setDosage(itemVO.getDosage());
        item.setUsageDesc(itemVO.getUsageDesc());
        item.setUnitPrice(medicine.getPrice());
        item.setAmount(medicine.getPrice().multiply(BigDecimal.valueOf(itemVO.getQuantity())));
        prescriptionItemRepository.save(item);

        total = total.add(item.getAmount());
    }

    // 4. 更新总金额
    prescription.setTotalAmount(total);
    prescriptionRepository.save(prescription);

    return prescription;
}
```

#### 作废处方事务

```java
@Transactional
public void cancelPrescription(Long id) {
    Prescription prescription = prescriptionRepository.findById(id)
        .orElseThrow(() -> new BusinessException("处方不存在"));

    if (prescription.getStatus() == Prescription.STATUS_CANCELLED) {
        throw new BusinessException("处方已作废");
    }

    // 1. 查询明细，恢复库存
    List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionId(id);
    for (PrescriptionItem item : items) {
        Medicine medicine = medicineRepository.findById(item.getMedicineId())
            .orElseThrow(() -> new BusinessException("药品不存在"));
        medicine.setStock(medicine.getStock() + item.getQuantity());
        medicineRepository.save(medicine);
    }

    // 2. 删除明细
    prescriptionItemRepository.deleteByPrescriptionId(id);

    // 3. 更新状态
    prescription.setStatus(Prescription.STATUS_CANCELLED);
    prescriptionRepository.save(prescription);
}
```

---

## 6. 数据模型

### 6.1 Medicine (药品)

```java
@Entity
@Table(name = "medicine")
public class Medicine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;               // 名称
    @Column(length = 100)
    private String spec;               // 规格
    @Column(length = 20)
    private String unit;               // 单位
    @Column(nullable = false)
    private Integer stock = 0;         // 库存
    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock = 10;  // 安全库存
    @Column(name = "expiry_date")
    private LocalDate expiryDate;      // 有效期
    @Column(name = "batch_no", length = 50)
    private String batchNo;            // 批号
    @Column(name = "pinyin_code", length = 50)
    private String pinyinCode;         // 拼音简码
    @Column(precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;  // 零售价
    @Column(length = 200)
    private String manufacturer;     // 生产厂家
    @Column(nullable = false)
    private Integer status = 1;        // 1=正常, 0=停用
}
```

### 6.2 Prescription (处方)

```java
@Entity
@Table(name = "prescription")
public class Prescription {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_DISPENSED = 2;
    public static final int STATUS_CANCELLED = 3;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "record_id", nullable = false)
    private Long recordId;             // 病历ID
    @Column(name = "patient_id", nullable = false)
    private Long patientId;            // 患者ID
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;             // 医生ID
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(nullable = false)
    private Integer status = 0;        // 0=待缴费, 1=已缴费, 2=已取药, 3=已作废
}
```

### 6.3 PrescriptionItem (处方明细)

```java
@Entity
@Table(name = "prescription_item")
public class PrescriptionItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;       // 处方ID
    @Column(name = "medicine_id", nullable = false)
    private Long medicineId;         // 药品ID
    @Column(nullable = false)
    private Integer quantity = 1;    // 数量
    @Column(length = 200)
    private String dosage;           // 剂量
    @Column(name = "usage_desc", length = 200)
    private String usageDesc;        // 用法
    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;  // 金额
}
```

---

## 7. 数据库表结构

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

CREATE TABLE prescription (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    record_id    BIGINT UNSIGNED NOT NULL,
    patient_id   BIGINT UNSIGNED NOT NULL,
    doctor_id    BIGINT UNSIGNED NOT NULL,
    total_amount DECIMAL(10,2) DEFAULT 0.00,
    status       TINYINT DEFAULT 0 COMMENT '0-待缴费 1-已缴费 2-已取药 3-已作废'
);

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

## 8. 事务安全要点

### 8.1 出入库事务
- 更新药品库存 + 插入库存日志必须在同一事务
- 出库时校验库存是否充足

### 8.2 开立处方事务
- 插入处方主表 + 插入明细 + 扣减库存必须在同一事务
- 任一药品库存不足时整体回滚
- 并发情况下通过数据库乐观锁保证库存不为负数

### 8.3 作废处方事务
- 恢复库存 + 删除明细 + 更新状态必须在同一事务
- 已作废处方不可再次作废
