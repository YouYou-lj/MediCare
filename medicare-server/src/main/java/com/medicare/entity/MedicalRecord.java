package com.medicare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 病历实体
 */
@Data
@Entity
@Table(name = "medical_record")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "chief_complaint", length = 500)
    private String chiefComplaint;

    @Column(name = "present_illness", columnDefinition = "TEXT")
    private String presentIllness;

    @Column(name = "past_history", length = 1000)
    private String pastHistory;

    @Column(name = "physical_exam", length = 1000)
    private String physicalExam;

    @Column(length = 500)
    private String diagnosis;

    @Column(length = 1000)
    private String advice;

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
