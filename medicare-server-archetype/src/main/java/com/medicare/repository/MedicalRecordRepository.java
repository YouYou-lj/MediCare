package com.medicare.repository;

import com.medicare.dto.MedicalRecordVO;
import com.medicare.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Optional<MedicalRecord> findByRegistrationId(Long registrationId);

    List<MedicalRecord> findByPatientIdOrderByCreateTimeDesc(Long patientId);

    /**
     * 查询病历列表（带患者+医生名称）
     */
    @Query(value = "SELECT mr.id, mr.registration_id AS registrationId, mr.patient_id AS patientId, "
            + "mr.doctor_id AS doctorId, mr.chief_complaint AS chiefComplaint, mr.present_illness AS presentIllness, "
            + "mr.past_history AS pastHistory, mr.physical_exam AS physicalExam, mr.diagnosis, mr.advice, "
            + "mr.create_time AS createTime, "
            + "p.name AS patientName, d.name AS doctorName "
            + "FROM medical_record mr "
            + "LEFT JOIN patient p ON mr.patient_id = p.id "
            + "LEFT JOIN doctor d ON mr.doctor_id = d.id "
            + "WHERE (:patientId IS NULL OR mr.patient_id = :patientId) "
            + "AND (:registrationId IS NULL OR mr.registration_id = :registrationId) "
            + "ORDER BY mr.create_time DESC",
            nativeQuery = true)
    List<MedicalRecordVO> findRecordVOList(@Param("patientId") Long patientId, @Param("registrationId") Long registrationId);

    /**
     * 按挂号ID查询病历详情
     */
    @Query(value = "SELECT mr.id, mr.registration_id AS registrationId, mr.patient_id AS patientId, "
            + "mr.doctor_id AS doctorId, mr.chief_complaint AS chiefComplaint, mr.present_illness AS presentIllness, "
            + "mr.past_history AS pastHistory, mr.physical_exam AS physicalExam, mr.diagnosis, mr.advice, "
            + "mr.create_time AS createTime, "
            + "p.name AS patientName, d.name AS doctorName "
            + "FROM medical_record mr "
            + "LEFT JOIN patient p ON mr.patient_id = p.id "
            + "LEFT JOIN doctor d ON mr.doctor_id = d.id "
            + "WHERE mr.registration_id = :registrationId",
            nativeQuery = true)
    MedicalRecordVO findRecordVOByRegistrationId(@Param("registrationId") Long registrationId);

    /**
     * 按ID查询病历详情（带患者+医生名称）
     */
    @Query(value = "SELECT mr.id, mr.registration_id AS registrationId, mr.patient_id AS patientId, "
            + "mr.doctor_id AS doctorId, mr.chief_complaint AS chiefComplaint, mr.present_illness AS presentIllness, "
            + "mr.past_history AS pastHistory, mr.physical_exam AS physicalExam, mr.diagnosis, mr.advice, "
            + "mr.create_time AS createTime, "
            + "p.name AS patientName, d.name AS doctorName "
            + "FROM medical_record mr "
            + "LEFT JOIN patient p ON mr.patient_id = p.id "
            + "LEFT JOIN doctor d ON mr.doctor_id = d.id "
            + "WHERE mr.id = :id",
            nativeQuery = true)
    MedicalRecordVO findRecordVOById(@Param("id") Long id);
}
