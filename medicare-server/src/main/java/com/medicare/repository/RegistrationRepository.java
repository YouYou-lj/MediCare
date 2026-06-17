package com.medicare.repository;

import com.medicare.dto.RegistrationVO;
import com.medicare.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    /**
     * 查询今日挂号列表 — 4 表 LEFT JOIN 投影
     */
    @Query(value = "SELECT r.id, r.patient_id AS patientId, r.schedule_id AS scheduleId, r.doctor_id AS doctorId, "
            + "r.reg_time AS regTime, r.status, r.seq_no AS seqNo, r.fee, r.create_time AS createTime, "
            + "p.name AS patientName, d.name AS doctorName, dep.name AS departmentName, s.time_slot AS timeSlot "
            + "FROM registration r "
            + "LEFT JOIN patient p ON r.patient_id = p.id "
            + "LEFT JOIN schedule s ON r.schedule_id = s.id "
            + "LEFT JOIN doctor d ON s.doctor_id = d.id "
            + "LEFT JOIN department dep ON d.department_id = dep.id "
            + "WHERE DATE(r.reg_time) = :date "
            + "AND (:status IS NULL OR r.status = :status) "
            + "ORDER BY r.reg_time DESC",
            nativeQuery = true)
    List<RegistrationVO> findTodayList(@Param("date") LocalDate date, @Param("status") Integer status);

    /**
     * 按医生查询候诊/就诊中列表
     */
    @Query(value = "SELECT r.id, r.patient_id AS patientId, r.schedule_id AS scheduleId, r.doctor_id AS doctorId, "
            + "r.reg_time AS regTime, r.status, r.seq_no AS seqNo, r.fee, r.create_time AS createTime, "
            + "p.name AS patientName, d.name AS doctorName, dep.name AS departmentName, s.time_slot AS timeSlot "
            + "FROM registration r "
            + "LEFT JOIN patient p ON r.patient_id = p.id "
            + "LEFT JOIN schedule s ON r.schedule_id = s.id "
            + "LEFT JOIN doctor d ON s.doctor_id = d.id "
            + "LEFT JOIN department dep ON d.department_id = dep.id "
            + "WHERE r.doctor_id = :doctorId AND r.status IN :statuses "
            + "ORDER BY r.seq_no",
            nativeQuery = true)
    List<RegistrationVO> findByDoctorAndStatusIn(@Param("doctorId") Long doctorId, @Param("statuses") List<Integer> statuses);

    /**
     * 叫号 — 状态 0→1
     */
    @Modifying
    @Query("UPDATE Registration r SET r.status = 1 WHERE r.id = :id AND r.status = 0")
    int callPatient(@Param("id") Long id);

    /**
     * 完成就诊 — 状态 1→2
     */
    @Modifying
    @Query("UPDATE Registration r SET r.status = 2 WHERE r.id = :id AND r.status = 1")
    int completeRegistration(@Param("id") Long id);

    /**
     * 按排班统计挂号数（用于序号分配）
     */
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.scheduleId = :scheduleId AND r.status <> 3")
    long countByScheduleAndNotCancelled(@Param("scheduleId") Long scheduleId);

    /**
     * 按患者查询挂号记录
     */
    @Query(value = "SELECT r.id, r.patient_id AS patientId, r.schedule_id AS scheduleId, r.doctor_id AS doctorId, "
            + "r.reg_time AS regTime, r.status, r.seq_no AS seqNo, r.fee, r.create_time AS createTime, "
            + "p.name AS patientName, d.name AS doctorName, dep.name AS departmentName, s.time_slot AS timeSlot "
            + "FROM registration r "
            + "LEFT JOIN patient p ON r.patient_id = p.id "
            + "LEFT JOIN schedule s ON r.schedule_id = s.id "
            + "LEFT JOIN doctor d ON s.doctor_id = d.id "
            + "LEFT JOIN department dep ON d.department_id = dep.id "
            + "WHERE r.patient_id = :patientId "
            + "ORDER BY r.reg_time DESC",
            nativeQuery = true)
    List<RegistrationVO> findByPatientId(@Param("patientId") Long patientId);
}
