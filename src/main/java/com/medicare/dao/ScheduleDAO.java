package com.medicare.dao;

import com.medicare.model.Schedule;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * 排班号源数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class ScheduleDAO extends BaseDAO<Schedule> {

    private static final String SQL_INSERT =
            "INSERT INTO schedule (doctor_id, work_date, time_slot, total_slots, remain_slots) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE schedule SET doctor_id = ?, work_date = ?, time_slot = ?, " +
            "total_slots = ?, remain_slots = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM schedule WHERE id = ?";

    private static final String SQL_SELECT_BY_ID =
            "SELECT s.id, s.doctor_id, s.work_date, s.time_slot, s.total_slots, s.remain_slots, " +
            "s.create_time, s.update_time, d.name AS doctorName, dep.name AS departmentName " +
            "FROM schedule s " +
            "LEFT JOIN doctor d ON s.doctor_id = d.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE s.id = ?";

    private static final String SQL_SELECT_ALL =
            "SELECT s.id, s.doctor_id, s.work_date, s.time_slot, s.total_slots, s.remain_slots, " +
            "s.create_time, s.update_time, d.name AS doctorName, dep.name AS departmentName " +
            "FROM schedule s " +
            "LEFT JOIN doctor d ON s.doctor_id = d.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "ORDER BY s.work_date DESC, s.doctor_id";

    private static final String SQL_SELECT_BY_DATE =
            "SELECT s.id, s.doctor_id, s.work_date, s.time_slot, s.total_slots, s.remain_slots, " +
            "s.create_time, s.update_time, d.name AS doctorName, dep.name AS departmentName " +
            "FROM schedule s " +
            "LEFT JOIN doctor d ON s.doctor_id = d.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE s.work_date = ? ORDER BY s.doctor_id";

    private static final String SQL_SELECT_BY_DOCTOR_DATE =
            "SELECT s.id, s.doctor_id, s.work_date, s.time_slot, s.total_slots, s.remain_slots, " +
            "s.create_time, s.update_time, d.name AS doctorName, dep.name AS departmentName " +
            "FROM schedule s " +
            "LEFT JOIN doctor d ON s.doctor_id = d.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE s.doctor_id = ? AND s.work_date = ? ORDER BY s.time_slot";

    private static final String SQL_SELECT_AVAILABLE =
            "SELECT s.id, s.doctor_id, s.work_date, s.time_slot, s.total_slots, s.remain_slots, " +
            "s.create_time, s.update_time, d.name AS doctorName, dep.name AS departmentName " +
            "FROM schedule s " +
            "LEFT JOIN doctor d ON s.doctor_id = d.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE s.work_date >= ? AND s.remain_slots > 0 ORDER BY s.work_date, s.doctor_id";

    private static final String SQL_DECREMENT_REMAIN =
            "UPDATE schedule SET remain_slots = remain_slots - 1 WHERE id = ? AND remain_slots > 0";

    private static final String SQL_INCREMENT_REMAIN =
            "UPDATE schedule SET remain_slots = remain_slots + 1 WHERE id = ?";

    private static final String SQL_CHECK_EXISTS =
            "SELECT COUNT(*) FROM schedule WHERE doctor_id = ? AND work_date = ? AND time_slot = ?";

    public Long insert(Schedule schedule) throws SQLException {
        return executeInsert(SQL_INSERT,
                schedule.getDoctorId(), schedule.getWorkDate(), schedule.getTimeSlot(),
                schedule.getTotalSlots(), schedule.getRemainSlots());
    }

    public int update(Schedule schedule) throws SQLException {
        return executeUpdate(SQL_UPDATE,
                schedule.getDoctorId(), schedule.getWorkDate(), schedule.getTimeSlot(),
                schedule.getTotalSlots(), schedule.getRemainSlots(), schedule.getId());
    }

    public int delete(Long id) throws SQLException {
        return executeUpdate(SQL_DELETE, id);
    }

    public Schedule findById(Long id) throws SQLException {
        return querySingle(SQL_SELECT_BY_ID, id);
    }

    public List<Schedule> findAll() throws SQLException {
        return queryList(SQL_SELECT_ALL);
    }

    public List<Schedule> findByDate(LocalDate date) throws SQLException {
        return queryList(SQL_SELECT_BY_DATE, date);
    }

    public List<Schedule> findByDoctorAndDate(Long doctorId, LocalDate date) throws SQLException {
        return queryList(SQL_SELECT_BY_DOCTOR_DATE, doctorId, date);
    }

    public List<Schedule> findAvailable(LocalDate fromDate) throws SQLException {
        return queryList(SQL_SELECT_AVAILABLE, fromDate);
    }

    /**
     * 扣减号源（挂号时使用）
     * @return 影响行数，0 表示号源不足
     */
    public int decrementRemain(Long scheduleId) throws SQLException {
        return executeUpdate(SQL_DECREMENT_REMAIN, scheduleId);
    }

    /**
     * 释放号源（取消挂号时使用）
     */
    public int incrementRemain(Long scheduleId) throws SQLException {
        return executeUpdate(SQL_INCREMENT_REMAIN, scheduleId);
    }

    public boolean exists(Long doctorId, LocalDate workDate, String timeSlot) throws SQLException {
        Long count = queryScalar(SQL_CHECK_EXISTS, doctorId, workDate, timeSlot);
        return count != null && count > 0;
    }
}
