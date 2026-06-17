package com.medicare.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存日志视图对象 — 关联药品名称
 */
@Data
public class InventoryLogVO {

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
}
