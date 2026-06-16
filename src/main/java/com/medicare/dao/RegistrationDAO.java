package com.medicare.dao;

import com.medicare.model.Registration;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * 挂号数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class RegistrationDAO extends BaseDAO<Registration> {

    private static final String SQL_INSERT =
            "INSERT INTO registration (patient_id, schedule_id, reg_time, status, seq_no, fee) " +
            "VALUES (?, ?, NOW(), ?, ?, ?)";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE registration SET status = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM registration WHERE id = ?";

    private static final String SQL_SELECT_BY_ID =
            "SELECT r.id, r.patient_id, r.schedule_id, r.reg_time, r.status, r.seq_no, r.fee, " +
            "r.create_time, r.update_time, " +
            "p.name AS patientName, d.name AS doctorName, dep.name AS departmentName, s.time_slot AS timeSlot " +
            "FROM registration r " +
            "LEFT JOIN patient p ON r.patient_id = p.id " +
            "LEFT JOIN schedule s ON r.schedule_id = s.id " +
            "LEFT JOIN doctor d ON s.doctor_id = d.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE r.id = ?";

    private static final String SQL_SELECT_TODAY =
            "SELECT r.id, r.patient_id, r.schedule_id, r.reg_time, r.status, r.seq_no, r.fee, " +
            "r.create_time, r.update_time, " +
            "p.name AS patientName, d.name AS doctorName, dep.name AS departmentName, s.time_slot AS timeSlot " +
            "FROM registration r " +
            "LEFT JOIN patient p ON r.patient_id = p.id " +
            "LEFT JOIN schedule s ON r.schedule_id = s.id " +
            "LEFT JOIN doctor d ON s.doctor_id = d.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE DATE(r.reg_time) = ? ORDER BY r.reg_time DESC";

    private static final String SQL_SELECT_BY_PATIENT =
            "SELECT r.id, r.patient_id, r.schedule_id, r.reg_time, r.status, r.seq_no, r.fee, " +
            "r.create_time, r.update_time, " +
            "p.name AS patientName, d.name AS doctorName, dep.name AS departmentName, s.time_slot AS timeSlot " +
            "FROM registration r " +
            "LEFT JOIN patient p ON r.patient_id = p.id " +
            "LEFT JOIN schedule s ON r.schedule_id = s.id " +
            "LEFT JOIN doctor d ON s.doctor_id = d.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE r.patient_id = ? ORDER BY r.reg_time DESC";

    private static final String SQL_SELECT_BY_STATUS =
            "SELECT r.id, r.patient_id, r.schedule_id, r.reg_time, r.status, r.seq_no, r.fee, " +
            "r.create_time, r.update_time, " +
            "p.name AS patientName, d.name AS doctorName, dep.name AS departmentName, s.time_slot AS timeSlot " +
            "FROM registration r " +
            "LEFT JOIN patient p ON r.patient_id = p.id " +
            "LEFT JOIN schedule s ON r.schedule_id = s.id " +
            "LEFT JOIN doctor d ON s.doctor_id = d.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE r.status = ? ORDER BY r.reg_time";

    private static final String SQL_SELECT_WAITING_BY_DOCTOR =
            "SELECT r.id, r.patient_id, r.schedule_id, r.reg_time, r.status, r.seq_no, r.fee, " +
            "r.create_time, r.update_time, " +
            "p.name AS patientName, d.name AS doctorName, dep.name AS departmentName, s.time_slot AS timeSlot " +
            "FROM registration r " +
            "LEFT JOIN patient p ON r.patient_id = p.id " +
            "LEFT JOIN schedule s ON r.schedule_id = s.id " +
            "LEFT JOIN doctor d ON s.doctor_id = d.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE s.doctor_id = ? AND DATE(s.work_date) = ? AND r.status IN (0, 1) " +
            "ORDER BY r.seq_no, r.reg_time";

    private static final String SQL_MAX_SEQ_NO =
            "SELECT MAX(seq_no) FROM registration WHERE DATE(reg_time) = ?";

    // ============================================================
    // 事务方法（需要传入 Connection）
    // ============================================================

    public Long insert(Connection conn, Registration reg) throws SQLException {
        return executeInsert(conn, SQL_INSERT,
                reg.getPatientId(), reg.getScheduleId(),
                reg.getStatus(), reg.getSeqNo(), reg.getFee());
    }

    public int updateStatus(Connection conn, Long id, Integer status) throws SQLException {
        return executeUpdate(conn, SQL_UPDATE_STATUS, status, id);
    }

    public int delete(Connection conn, Long id) throws SQLException {
        return executeUpdate(conn, SQL_DELETE, id);
    }

    // ============================================================
    // 非事务更新
    // ============================================================

    public int updateStatus(Long id, Integer status) throws SQLException {
        return executeUpdate(SQL_UPDATE_STATUS, status, id);
    }

    public int delete(Long id) throws SQLException {
        return executeUpdate(SQL_DELETE, id);
    }

    // ============================================================
    // 非事务查询
    // ============================================================

    public Registration findById(Long id) throws SQLException {
        return querySingle(SQL_SELECT_BY_ID, id);
    }

    public List<Registration> findToday(LocalDate date) throws SQLException {
        return queryList(SQL_SELECT_TODAY, date);
    }

    public List<Registration> findByPatient(Long patientId) throws SQLException {
        return queryList(SQL_SELECT_BY_PATIENT, patientId);
    }

    public List<Registration> findByStatus(Integer status) throws SQLException {
        return queryList(SQL_SELECT_BY_STATUS, status);
    }

    public List<Registration> findWaitingByDoctor(Long doctorId, LocalDate date) throws SQLException {
        return queryList(SQL_SELECT_WAITING_BY_DOCTOR, doctorId, date);
    }

    public Integer getMaxSeqNo(LocalDate date) throws SQLException {
        Number max = queryScalar(SQL_MAX_SEQ_NO, date);
        return max != null ? max.intValue() : 0;
    }
}
