package com.medicare.dao;

import com.medicare.model.InventoryLog;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 库存变动日志数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class InventoryLogDAO extends BaseDAO<InventoryLog> {

    private static final String SQL_INSERT =
            "INSERT INTO inventory_log (medicine_id, type, quantity, batch_no, expiry_date, operator, remark) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_ALL =
            "SELECT l.id, l.medicine_id, l.type, l.quantity, l.batch_no, l.expiry_date, l.operator, l.remark, l.log_time, " +
            "m.name AS medicineName " +
            "FROM inventory_log l LEFT JOIN medicine m ON l.medicine_id = m.id " +
            "ORDER BY l.log_time DESC";

    private static final String SQL_SELECT_BY_MEDICINE =
            "SELECT l.id, l.medicine_id, l.type, l.quantity, l.batch_no, l.expiry_date, l.operator, l.remark, l.log_time, " +
            "m.name AS medicineName " +
            "FROM inventory_log l LEFT JOIN medicine m ON l.medicine_id = m.id " +
            "WHERE l.medicine_id = ? ORDER BY l.log_time DESC";

    private static final String SQL_SELECT_BY_TYPE =
            "SELECT l.id, l.medicine_id, l.type, l.quantity, l.batch_no, l.expiry_date, l.operator, l.remark, l.log_time, " +
            "m.name AS medicineName " +
            "FROM inventory_log l LEFT JOIN medicine m ON l.medicine_id = m.id " +
            "WHERE l.type = ? ORDER BY l.log_time DESC LIMIT 200";

    // ============================================================
    // 事务方法
    // ============================================================

    public Long insert(Connection conn, InventoryLog log) throws SQLException {
        return executeInsert(conn, SQL_INSERT,
                log.getMedicineId(), log.getType(), log.getQuantity(),
                log.getBatchNo(), log.getExpiryDate(), log.getOperator(), log.getRemark());
    }

    private static final String SQL_DELETE_BY_MEDICINE =
            "DELETE FROM inventory_log WHERE medicine_id = ?";

    // ============================================================
    // 事务方法
    // ============================================================

    public int deleteByMedicineId(Connection conn, Long medicineId) throws SQLException {
        return executeUpdate(conn, SQL_DELETE_BY_MEDICINE, medicineId);
    }

    // ============================================================
    // 非事务查询
    // ============================================================

    public List<InventoryLog> findAll() throws SQLException {
        return queryList(SQL_SELECT_ALL);
    }

    public List<InventoryLog> findByMedicine(Long medicineId) throws SQLException {
        return queryList(SQL_SELECT_BY_MEDICINE, medicineId);
    }

    public List<InventoryLog> findByType(Integer type) throws SQLException {
        return queryList(SQL_SELECT_BY_TYPE, type);
    }
}
