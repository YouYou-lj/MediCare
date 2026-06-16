package com.medicare.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 处方明细实体类
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class PrescriptionItem {

    private Long id;
    private Long prescriptionId;
    private Long medicineId;
    private Integer quantity;
    private String dosage;
    private String usageDesc;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private LocalDateTime createTime;

    // 关联字段
    private String medicineName;
    private String medicineSpec;
    private String medicineUnit;

    public PrescriptionItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getUsageDesc() {
        return usageDesc;
    }

    public void setUsageDesc(String usageDesc) {
        this.usageDesc = usageDesc;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getMedicineSpec() {
        return medicineSpec;
    }

    public void setMedicineSpec(String medicineSpec) {
        this.medicineSpec = medicineSpec;
    }

    public String getMedicineUnit() {
        return medicineUnit;
    }

    public void setMedicineUnit(String medicineUnit) {
        this.medicineUnit = medicineUnit;
    }
}
