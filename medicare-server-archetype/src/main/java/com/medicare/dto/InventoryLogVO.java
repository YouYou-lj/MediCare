package com.medicare.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存日志视图对象 — 关联药品名称（接口投影）
 */
public interface InventoryLogVO {
    Long getId();
    Long getMedicineId();
    Integer getType();
    Integer getQuantity();
    String getBatchNo();
    LocalDate getExpiryDate();
    String getOperator();
    String getRemark();
    LocalDateTime getLogTime();
    String getMedicineName();
}