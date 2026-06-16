package com.medicare.dao;

import com.medicare.model.Medicine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 药品数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class MedicineDAO extends BaseDAO<Medicine> {

    private static final String SQL_INSERT =
            "INSERT INTO medicine (name, spec, unit, stock, safety_stock, expiry_date, batch_no, pinyin_code, price, manufacturer, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE medicine SET name = ?, spec = ?, unit = ?, stock = ?, safety_stock = ?, " +
            "expiry_date = ?, batch_no = ?, pinyin_code = ?, price = ?, manufacturer = ?, status = ? WHERE id = ?";

    private static final String SQL_UPDATE_STOCK =
            "UPDATE medicine SET stock = stock + ? WHERE id = ?";

    private static final String SQL_UPDATE_STOCK_INFO =
            "UPDATE medicine SET stock = stock + ?, batch_no = ?, expiry_date = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM medicine WHERE id = ?";

    private static final String SQL_SELECT_BY_ID =
            "SELECT id, name, spec, unit, stock, safety_stock, expiry_date, batch_no, pinyin_code, price, manufacturer, status, " +
            "create_time, update_time FROM medicine WHERE id = ?";

    private static final String SQL_SELECT_ALL =
            "SELECT id, name, spec, unit, stock, safety_stock, expiry_date, batch_no, pinyin_code, price, manufacturer, status, " +
            "create_time, update_time FROM medicine ORDER BY name";

    private static final String SQL_SELECT_BY_NAME =
            "SELECT id, name, spec, unit, stock, safety_stock, expiry_date, batch_no, pinyin_code, price, manufacturer, status, " +
            "create_time, update_time FROM medicine WHERE name LIKE ? ORDER BY name";

    private static final String SQL_SELECT_BY_PINYIN =
            "SELECT id, name, spec, unit, stock, safety_stock, expiry_date, batch_no, pinyin_code, price, manufacturer, status, " +
            "create_time, update_time FROM medicine WHERE pinyin_code LIKE ? ORDER BY name";

    private static final String SQL_SELECT_LOW_STOCK =
            "SELECT id, name, spec, unit, stock, safety_stock, expiry_date, batch_no, pinyin_code, price, manufacturer, status, " +
            "create_time, update_time FROM medicine WHERE stock <= safety_stock AND status = 1 ORDER BY stock";

    private static final String SQL_SELECT_NEAR_EXPIRY =
            "SELECT id, name, spec, unit, stock, safety_stock, expiry_date, batch_no, pinyin_code, price, manufacturer, status, " +
            "create_time, update_time FROM medicine WHERE expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
            "AND expiry_date >= CURDATE() AND status = 1 ORDER BY expiry_date";

    private static final String SQL_COUNT_BY_NAME_SPEC =
            "SELECT COUNT(*) FROM medicine WHERE name = ? AND spec = ?";

    private static final String SQL_COUNT_BY_NAME_SPEC_EXCLUDE_ID =
            "SELECT COUNT(*) FROM medicine WHERE name = ? AND spec = ? AND id != ?";

    // ============================================================
    // CRUD
    // ============================================================

    public Long insert(Medicine m) throws SQLException {
        return executeInsert(SQL_INSERT,
                m.getName(), m.getSpec(), m.getUnit(), m.getStock(), m.getSafetyStock(),
                m.getExpiryDate(), m.getBatchNo(), m.getPinyinCode(),
                m.getPrice(), m.getManufacturer(), m.getStatus());
    }

    public int update(Medicine m) throws SQLException {
        return executeUpdate(SQL_UPDATE,
                m.getName(), m.getSpec(), m.getUnit(), m.getStock(), m.getSafetyStock(),
                m.getExpiryDate(), m.getBatchNo(), m.getPinyinCode(),
                m.getPrice(), m.getManufacturer(), m.getStatus(), m.getId());
    }

    public int updateStock(Connection conn, Long medicineId, Integer delta) throws SQLException {
        return executeUpdate(conn, SQL_UPDATE_STOCK, delta, medicineId);
    }

    public int updateStockAndInfo(Connection conn, Long medicineId, Integer delta, String batchNo, java.time.LocalDate expiryDate) throws SQLException {
        return executeUpdate(conn, SQL_UPDATE_STOCK_INFO, delta, batchNo, expiryDate, medicineId);
    }

    public int delete(Long id) throws SQLException {
        return executeUpdate(SQL_DELETE, id);
    }

    public int delete(Connection conn, Long id) throws SQLException {
        return executeUpdate(conn, SQL_DELETE, id);
    }

    public Medicine findById(Long id) throws SQLException {
        return querySingle(SQL_SELECT_BY_ID, id);
    }

    public List<Medicine> findAll() throws SQLException {
        return queryList(SQL_SELECT_ALL);
    }

    public List<Medicine> findByName(String name) throws SQLException {
        return queryList(SQL_SELECT_BY_NAME, "%" + name + "%");
    }

    public List<Medicine> findByPinyin(String pinyin) throws SQLException {
        return queryList(SQL_SELECT_BY_PINYIN, "%" + pinyin + "%");
    }

    public List<Medicine> findLowStock() throws SQLException {
        return queryList(SQL_SELECT_LOW_STOCK);
    }

    public List<Medicine> findNearExpiry() throws SQLException {
        return queryList(SQL_SELECT_NEAR_EXPIRY);
    }

    public boolean existsByNameAndSpec(String name, String spec) throws SQLException {
        Long count = queryScalar(SQL_COUNT_BY_NAME_SPEC, name, spec);
        return count != null && count > 0;
    }

    public boolean existsByNameAndSpec(String name, String spec, Long excludeId) throws SQLException {
        Long count = queryScalar(SQL_COUNT_BY_NAME_SPEC_EXCLUDE_ID, name, spec, excludeId);
        return count != null && count > 0;
    }
}
