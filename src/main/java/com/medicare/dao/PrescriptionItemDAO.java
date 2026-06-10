package com.medicare.dao;

import com.medicare.model.PrescriptionItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 处方明细数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class PrescriptionItemDAO extends BaseDAO<PrescriptionItem> {

    private static final String SQL_INSERT =
            "INSERT INTO prescription_item (prescription_id, medicine_id, quantity, dosage, usage_desc, unit_price, amount) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_DELETE_BY_PRESCRIPTION =
            "DELETE FROM prescription_item WHERE prescription_id = ?";

    private static final String SQL_SELECT_BY_PRESCRIPTION =
            "SELECT pi.id, pi.prescription_id, pi.medicine_id, pi.quantity, pi.dosage, pi.usage_desc, " +
            "pi.unit_price, pi.amount, pi.create_time, " +
            "m.name AS medicineName, m.spec AS medicineSpec, m.unit AS medicineUnit " +
            "FROM prescription_item pi " +
            "LEFT JOIN medicine m ON pi.medicine_id = m.id " +
            "WHERE pi.prescription_id = ?";

    // ============================================================
    // 事务方法
    // ============================================================

    public Long insert(Connection conn, PrescriptionItem item) throws SQLException {
        return executeInsert(conn, SQL_INSERT,
                item.getPrescriptionId(), item.getMedicineId(), item.getQuantity(),
                item.getDosage(), item.getUsageDesc(), item.getUnitPrice(), item.getAmount());
    }

    public int deleteByPrescription(Connection conn, Long prescriptionId) throws SQLException {
        return executeUpdate(conn, SQL_DELETE_BY_PRESCRIPTION, prescriptionId);
    }

    // ============================================================
    // 非事务查询
    // ============================================================

    public List<PrescriptionItem> findByPrescription(Long prescriptionId) throws SQLException {
        return queryList(SQL_SELECT_BY_PRESCRIPTION, prescriptionId);
    }
}
