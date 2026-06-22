package com.medicare.dto;

import java.time.LocalDateTime;

/**
 * 医生视图对象 — 关联科室名称（接口投影）
 */
public interface DoctorVO {
    Long getId();
    String getName();
    Long getDepartmentId();
    String getTitle();
    Integer getStatus();
    LocalDateTime getCreateTime();
    String getDepartmentName();
}