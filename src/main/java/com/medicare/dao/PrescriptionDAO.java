package com.medicare.dao;

import com.medicare.model.Prescription;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 处方数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class PrescriptionDAO extends BaseDAO<Prescription> {

    private static final String SQL_INSERT =
            "INSERT INTO prescription (record_id, patient_id, doctor_id, total_amount, status) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE prescription SET status = ? WHERE id = ?";

    private static final String SQL_SELECT_BY_ID =
            "SELECT p.id, p.record_id, p.patient_id, p.doctor_id, p.total_amount, p.status, " +
            "p.create_time, p.update_time, " +
            "pt.name AS patientName, d.name AS doctorName " +
            "FROM prescription p " +
            "LEFT JOIN patient pt ON p.patient_id = pt.id " +
            "LEFT JOIN doctor d ON p.doctor_id = d.id " +
            "WHERE p.id = ?";

    private static final String SQL_SELECT_BY_PATIENT =
            "SELECT p.id, p.record_id, p.patient_id, p.doctor_id, p.total_amount, p.status, " +
            "p.create_time, p.update_time, " +
            "pt.name AS patientName, d.name AS doctorName " +
            "FROM prescription p " +
            "LEFT JOIN patient pt ON p.patient_id = pt.id " +
            "LEFT JOIN doctor d ON p.doctor_id = d.id " +
            "WHERE p.patient_id = ? ORDER BY p.create_time DESC";

    private static final String SQL_SELECT_BY_RECORD =
            "SELECT p.id, p.record_id, p.patient_id, p.doctor_id, p.total_amount, p.status, " +
            "p.create_time, p.update_time, " +
            "pt.name AS patientName, d.name AS doctorName " +
            "FROM prescription p " +
            "LEFT JOIN patient pt ON p.patient_id = pt.id " +
            "LEFT JOIN doctor d ON p.doctor_id = d.id " +
            "WHERE p.record_id = ?";

    private static final String SQL_SELECT_TODAY =
            "SELECT p.id, p.record_id, p.patient_id, p.doctor_id, p.total_amount, p.status, " +
            "p.create_time, p.update_time, " +
            "pt.name AS patientName, d.name AS doctorName " +
            "FROM prescription p " +
            "LEFT JOIN patient pt ON p.patient_id = pt.id " +
            "LEFT JOIN doctor d ON p.doctor_id = d.id " +
            "WHERE DATE(p.create_time) = CURDATE() ORDER BY p.create_time DESC";

    // ============================================================
    // 事务方法
    // ============================================================

    public Long insert(Connection conn, Prescription p) throws SQLException {
        return executeInsert(conn, SQL_INSERT,
                p.getRecordId(), p.getPatientId(), p.getDoctorId(),
                p.getTotalAmount(), p.getStatus());
    }

    public int updateStatus(Connection conn, Long id, Integer status) throws SQLException {
        return executeUpdate(conn, SQL_UPDATE_STATUS, status, id);
    }

    // ============================================================
    // 非事务查询
    // ============================================================

    public Prescription findById(Long id) throws SQLException {
        return querySingle(SQL_SELECT_BY_ID, id);
    }

    public List<Prescription> findByPatient(Long patientId) throws SQLException {
        return queryList(SQL_SELECT_BY_PATIENT, patientId);
    }

    public Prescription findByRecord(Long recordId) throws SQLException {
        return querySingle(SQL_SELECT_BY_RECORD, recordId);
    }

    public List<Prescription> findToday() throws SQLException {
        return queryList(SQL_SELECT_TODAY);
    }
}
