package com.medicare.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 处方列表视图对象 — 接口投影（不含明细）
 */
public interface PrescriptionListVO {
    Long getId();
    Long getRecordId();
    Long getPatientId();
    Long getDoctorId();
    BigDecimal getTotalAmount();
    Integer getStatus();
    LocalDateTime getCreateTime();
    String getPatientName();
    String getDoctorName();
}