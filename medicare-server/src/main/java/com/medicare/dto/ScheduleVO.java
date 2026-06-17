package com.medicare.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班视图对象 — 关联医生+科室名称（接口投影）
 */
public interface ScheduleVO {
    Long getId();
    Long getDoctorId();
    LocalDate getWorkDate();
    String getTimeSlot();
    Integer getTotalSlots();
    Integer getRemainSlots();
    LocalDateTime getCreateTime();
    String getDoctorName();
    String getDepartmentName();
}