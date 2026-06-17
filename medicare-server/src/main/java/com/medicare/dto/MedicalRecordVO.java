package com.medicare.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 病历视图对象 — 关联患者/医生名称
 */
@Data
public class MedicalRecordVO {

    private Long id;
    private Long registrationId;
    private Long patientId;
    private Long doctorId;
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private String physicalExam;
    private String diagnosis;
    private String advice;
    private LocalDateTime createTime;

    // 关联字段
    private String patientName;
    private String doctorName;
}
