package com.medicare.repository;

import com.medicare.dto.ScheduleVO;
import com.medicare.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 原子扣减号源 — WHERE remain_slots > 0 防止超卖
     * @return 影响行数，0 表示号源不足
     */
    @Modifying
    @Query("UPDATE Schedule s SET s.remainSlots = s.remainSlots - 1 WHERE s.id = :id AND s.remainSlots > 0")
    int decrementRemain(@Param("id") Long id);

    /**
     * 原子回增号源（取消挂号时）
     */
    @Modifying
    @Query("UPDATE Schedule s SET s.remainSlots = s.remainSlots + 1 WHERE s.id = :id")
    int incrementRemain(@Param("id") Long id);

    /**
     * 查询排班列表（带医生+科室名称）
     */
    @Query(value = "SELECT s.id, s.doctor_id AS doctorId, s.work_date AS workDate, s.time_slot AS timeSlot, "
            + "s.total_slots AS totalSlots, s.remain_slots AS remainSlots, s.create_time AS createTime, "
            + "d.name AS doctorName, dep.name AS departmentName "
            + "FROM schedule s LEFT JOIN doctor d ON s.doctor_id = d.id "
            + "LEFT JOIN department dep ON d.department_id = dep.id "
            + "WHERE (:date IS NULL OR s.work_date = :date) "
            + "AND (:deptId IS NULL OR d.department_id = :deptId) "
            + "ORDER BY s.work_date, s.time_slot",
            nativeQuery = true)
    List<ScheduleVO> findScheduleVOList(@Param("date") LocalDate date, @Param("deptId") Long deptId);

    /**
     * 查询可用号源（剩余 > 0）
     */
    @Query(value = "SELECT s.id, s.doctor_id AS doctorId, s.work_date AS workDate, s.time_slot AS timeSlot, "
            + "s.total_slots AS totalSlots, s.remain_slots AS remainSlots, s.create_time AS createTime, "
            + "d.name AS doctorName, dep.name AS departmentName "
            + "FROM schedule s LEFT JOIN doctor d ON s.doctor_id = d.id "
            + "LEFT JOIN department dep ON d.department_id = dep.id "
            + "WHERE s.remain_slots > 0 "
            + "AND (:date IS NULL OR s.work_date = :date) "
            + "AND (:deptId IS NULL OR d.department_id = :deptId) "
            + "ORDER BY s.work_date, s.time_slot",
            nativeQuery = true)
    List<ScheduleVO> findAvailableSchedules(@Param("date") LocalDate date, @Param("deptId") Long deptId);

    /**
     * 查询某天某医生已有几条排班（用于序号分配）
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.doctorId = :doctorId AND s.workDate = :date")
    long countByDoctorAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);
}
