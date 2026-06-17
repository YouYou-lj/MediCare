package com.medicare.repository;

import com.medicare.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByRecordId(Long recordId);

    List<Prescription> findByPatientIdOrderByCreateTimeDesc(Long patientId);

    List<Prescription> findByStatus(Integer status);

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
