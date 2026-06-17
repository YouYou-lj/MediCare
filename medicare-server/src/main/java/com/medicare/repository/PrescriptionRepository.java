package com.medicare.repository;

import com.medicare.dto.PrescriptionVO;
import com.medicare.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByRecordId(Long recordId);

    List<Prescription> findByPatientIdOrderByCreateTimeDesc(Long patientId);

    List<Prescription> findByStatus(Integer status);

    /**
     * 处方列表 — 3 表 LEFT JOIN 投影
     */
    @Query(value = "SELECT p.id, p.record_id AS recordId, p.patient_id AS patientId, p.doctor_id AS doctorId, "
            + "p.total_amount AS totalAmount, p.status, p.create_time AS createTime, "
            + "pat.name AS patientName, doc.name AS doctorName "
            + "FROM prescription p "
            + "LEFT JOIN patient pat ON p.patient_id = pat.id "
            + "LEFT JOIN doctor doc ON p.doctor_id = doc.id "
            + "WHERE (:patientId IS NULL OR p.patient_id = :patientId) "
            + "AND (:today IS NULL OR DATE(p.create_time) = :today) "
            + "ORDER BY p.create_time DESC",
            nativeQuery = true)
    List<PrescriptionVO> findPrescriptionVOList(@Param("patientId") Long patientId, @Param("today") LocalDate today);

    /**
     * 取药 — 状态改为已取药(2)
     */
    @Modifying
    @Query("UPDATE Prescription p SET p.status = 2 WHERE p.id = :id AND p.status IN (0, 1)")
    int dispense(@Param("id") Long id);

    /**
     * 作废 — 状态改为已作废(3)
     */
    @Modifying
    @Query("UPDATE Prescription p SET p.status = 3 WHERE p.id = :id AND p.status IN (0, 1)")
    int cancel(@Param("id") Long id);

    /**
     * 按医生查询处方
     */
    List<Prescription> findByDoctorIdOrderByCreateTimeDesc(Long doctorId);
}
