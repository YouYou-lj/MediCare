package com.medicare.repository;

import com.medicare.dto.DoctorVO;
import com.medicare.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByDepartmentIdAndStatus(Long departmentId, Integer status);

    List<Doctor> findByStatus(Integer status);

    /**
     * 查询医生列表（带科室名称）
     */
    @Query(value = "SELECT d.id, d.name, d.department_id AS departmentId, d.title, d.status, d.create_time AS createTime, "
            + "dep.name AS departmentName "
            + "FROM doctor d LEFT JOIN department dep ON d.department_id = dep.id "
            + "WHERE (:deptId IS NULL OR d.department_id = :deptId) "
            + "ORDER BY d.id",
            nativeQuery = true)
    List<DoctorVO> findDoctorVOList(@Param("deptId") Long deptId);
}
