package com.medicare.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存变动日志实体类
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class InventoryLog {

    private Long id;
    private Long medicineId;
    private Integer type;
    private Integer quantity;
    private String batchNo;
    private LocalDate expiryDate;
    private String operator;
    private String remark;
    private LocalDateTime logTime;

    // 关联字段
    private String medicineName;

    public static final int TYPE_IN = 1;      // 入库
    public static final int TYPE_OUT = 2;     // 出库
    public static final int TYPE_SURPLUS = 3; // 盘盈
    public static final int TYPE_DEFICIT = 4; // 盘亏

    public InventoryLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getLogTime() {
        return logTime;
    }

    public void setLogTime(LocalDateTime logTime) {
        this.logTime = logTime;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getTypeText() {
        return switch (type) {
            case TYPE_IN -> "入库";
            case TYPE_OUT -> "出库";
            case TYPE_SURPLUS -> "盘盈";
            case TYPE_DEFICIT -> "盘亏";
            default -> "未知";
        };
    }
}
