package com.medicare.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 处方明细视图对象 — 关联药品名称/规格/单位
 */
@Data
public class PrescriptionItemVO {

    private Long id;
    private Long prescriptionId;
    private Long medicineId;
    private Integer quantity;
    private String dosage;
    private String usageDesc;
    private BigDecimal unitPrice;
    private BigDecimal amount;

    // 关联字段
    private String medicineName;
    private String medicineSpec;
    private String medicineUnit;
}
