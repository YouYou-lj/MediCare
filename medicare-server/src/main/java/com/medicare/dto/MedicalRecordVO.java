package com.medicare.dto;

import java.time.LocalDateTime;

/**
 * 病历视图对象 — 关联患者/医生名称（接口投影）
 */
public interface MedicalRecordVO {
    Long getId();
    Long getRegistrationId();
    Long getPatientId();
    Long getDoctorId();
    String getChiefComplaint();
    String getPresentIllness();
    String getPastHistory();
    String getPhysicalExam();
    String getDiagnosis();
    String getAdvice();
    LocalDateTime getCreateTime();
    String getPatientName();
    String getDoctorName();
}