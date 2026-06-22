package com.medicare.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 挂号视图对象 — 4 表 LEFT JOIN 投影（接口投影）
 */
public interface RegistrationVO {
    Long getId();
    Long getPatientId();
    Long getScheduleId();
    Long getDoctorId();
    LocalDateTime getRegTime();
    Integer getStatus();
    Integer getSeqNo();
    BigDecimal getFee();
    LocalDateTime getCreateTime();
    String getPatientName();
    String getDoctorName();
    String getDepartmentName();
    String getTimeSlot();
}