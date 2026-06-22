package com.medicare.repository;

import com.medicare.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByIdCard(String idCard);
    boolean existsByIdCardAndIdNot(String idCard, Long id);

    Optional<Patient> findByIdCard(String idCard);

    /**
     * 按关键字搜索患者（姓名/身份证/手机号）
     */
    @Query("SELECT p FROM Patient p WHERE p.name LIKE %:keyword% OR p.idCard LIKE %:keyword% OR p.phone LIKE %:keyword%")
    List<Patient> search(@Param("keyword") String keyword);
}
