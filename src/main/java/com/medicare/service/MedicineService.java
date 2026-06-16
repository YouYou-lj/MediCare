package com.medicare.service;

import com.medicare.dao.InventoryLogDAO;
import com.medicare.dao.MedicineDAO;
import com.medicare.model.InventoryLog;
import com.medicare.model.Medicine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * 药品库存管理服务
 * 入库/出库操作使用事务控制
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class MedicineService {

    private static final Logger logger = LoggerFactory.getLogger(MedicineService.class);

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final InventoryLogDAO inventoryLogDAO = new InventoryLogDAO();

    // ============================================================
    // 药品档案管理
    // ============================================================

    public Long addMedicine(Medicine m) throws SQLException, IllegalArgumentException {
        validate(m);
        if (medicineDAO.existsByNameAndSpec(m.getName(), m.getSpec())) {
            throw new IllegalArgumentException("该药品规格已存在");
        }
        if (m.getStock() == null) m.setStock(0);
        if (m.getSafetyStock() == null) m.setSafetyStock(10);
        if (m.getStatus() == null) m.setStatus(1);
        Long id = medicineDAO.insert(m);
        logger.info("新增药品: id={}, name={}", id, m.getName());
        return id;
    }

    public void updateMedicine(Medicine m) throws SQLException, IllegalArgumentException {
        if (m.getId() == null) throw new IllegalArgumentException("药品 ID 不能为空");
        validate(m);
        if (medicineDAO.existsByNameAndSpec(m.getName(), m.getSpec(), m.getId())) {
            throw new IllegalArgumentException("该药品规格已被其他药品使用");
        }
        int rows = medicineDAO.update(m);
        if (rows == 0) throw new IllegalArgumentException("药品不存在");
        logger.info("更新药品: id={}, name={}", m.getId(), m.getName());
    }

    public void deleteMedicine(Long id) throws SQLException, IllegalArgumentException {
        Connection conn = null;
        try {
            conn = medicineDAO.getConnection();
            // 先删除关联库存日志（外键约束）
            inventoryLogDAO.deleteByMedicineId(conn, id);
            int rows = medicineDAO.delete(conn, id);
            if (rows == 0) throw new IllegalArgumentException("药品不存在");
            medicineDAO.commit(conn);
            logger.info("删除药品: id={}", id);
        } catch (SQLException e) {
            medicineDAO.rollback(conn);
            logger.error("删除药品失败，事务已回滚", e);
            throw e;
        } finally {
            medicineDAO.closeConnection(conn);
        }
    }

    public Medicine getById(Long id) throws SQLException {
        return medicineDAO.findById(id);
    }

    public List<Medicine> listAll() throws SQLException {
        return medicineDAO.findAll();
    }

    public List<Medicine> search(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) return listAll();
        String kw = keyword.trim();
        List<Medicine> byName = medicineDAO.findByName(kw);
        if (!byName.isEmpty()) return byName;
        return medicineDAO.findByPinyin(kw.toUpperCase());
    }

    // ============================================================
    // 入库（事务控制）
    // ============================================================

    public void stockIn(Long medicineId, Integer quantity, String batchNo, LocalDate expiryDate, String operator, String remark)
            throws SQLException, IllegalArgumentException {
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("入库数量必须大于 0");
        if (medicineId == null) throw new IllegalArgumentException("药品不能为空");

        Connection conn = null;
        try {
            conn = medicineDAO.getConnection();

            // 1. 增加库存，同时更新批号和有效期
            Medicine m = medicineDAO.findById(medicineId);
            String finalBatchNo = (batchNo != null && !batchNo.isEmpty()) ? batchNo : m.getBatchNo();
            LocalDate finalExpiry = (expiryDate != null) ? expiryDate : m.getExpiryDate();
            medicineDAO.updateStockAndInfo(conn, medicineId, quantity, finalBatchNo, finalExpiry);

            // 2. 记录日志
            InventoryLog log = new InventoryLog();
            log.setMedicineId(medicineId);
            log.setType(InventoryLog.TYPE_IN);
            log.setQuantity(quantity);
            log.setBatchNo(batchNo);
            log.setExpiryDate(expiryDate);
            log.setOperator(operator);
            log.setRemark(remark);
            inventoryLogDAO.insert(conn, log);

            medicineDAO.commit(conn);
            logger.info("入库成功: medicineId={}, quantity={}", medicineId, quantity);
        } catch (SQLException e) {
            medicineDAO.rollback(conn);
            logger.error("入库失败，事务已回滚", e);
            throw e;
        } finally {
            medicineDAO.closeConnection(conn);
        }
    }

    // ============================================================
    // 出库（事务控制）
    // ============================================================

    public void stockOut(Long medicineId, Integer quantity, String operator, String remark)
            throws SQLException, IllegalArgumentException {
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("出库数量必须大于 0");
        if (medicineId == null) throw new IllegalArgumentException("药品不能为空");

        Medicine m = medicineDAO.findById(medicineId);
        if (m == null) throw new IllegalArgumentException("药品不存在");
        if (m.getStock() == null || m.getStock() < quantity) {
            throw new IllegalArgumentException("库存不足，当前库存: " + (m.getStock() != null ? m.getStock() : 0));
        }

        Connection conn = null;
        try {
            conn = medicineDAO.getConnection();

            // 1. 减少库存
            medicineDAO.updateStock(conn, medicineId, -quantity);

            // 2. 记录日志
            InventoryLog log = new InventoryLog();
            log.setMedicineId(medicineId);
            log.setType(InventoryLog.TYPE_OUT);
            log.setQuantity(-quantity);
            log.setBatchNo(m.getBatchNo());
            log.setExpiryDate(m.getExpiryDate());
            log.setOperator(operator);
            log.setRemark(remark);
            inventoryLogDAO.insert(conn, log);

            medicineDAO.commit(conn);
            logger.info("出库成功: medicineId={}, quantity={}", medicineId, quantity);
        } catch (SQLException e) {
            medicineDAO.rollback(conn);
            logger.error("出库失败，事务已回滚", e);
            throw e;
        } finally {
            medicineDAO.closeConnection(conn);
        }
    }

    // ============================================================
    // 预警查询
    // ============================================================

    public List<Medicine> listLowStock() throws SQLException {
        return medicineDAO.findLowStock();
    }

    public List<Medicine> listNearExpiry() throws SQLException {
        return medicineDAO.findNearExpiry();
    }

    // ============================================================
    // 库存日志
    // ============================================================

    public List<InventoryLog> listLogs() throws SQLException {
        return inventoryLogDAO.findAll();
    }

    public List<InventoryLog> listLogsByMedicine(Long medicineId) throws SQLException {
        return inventoryLogDAO.findByMedicine(medicineId);
    }

    // ============================================================
    // 私有校验
    // ============================================================

    private void validate(Medicine m) {
        if (m == null) throw new IllegalArgumentException("药品信息不能为空");
        if (m.getName() == null || m.getName().trim().isEmpty()) throw new IllegalArgumentException("药品名称不能为空");
    }
}
