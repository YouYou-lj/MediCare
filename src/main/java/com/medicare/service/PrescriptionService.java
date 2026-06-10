package com.medicare.service;

import com.medicare.dao.*;
import com.medicare.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * 处方管理服务
 * 核心：处方开立时事务级库存扣减
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class PrescriptionService {

    private static final Logger logger = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();
    private final PrescriptionItemDAO itemDAO = new PrescriptionItemDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final InventoryLogDAO inventoryLogDAO = new InventoryLogDAO();

    // ============================================================
    // 开立处方（事务控制）
    // ============================================================

    public Long createPrescription(Prescription prescription, List<PrescriptionItem> items)
            throws SQLException, IllegalArgumentException {
        validate(prescription, items);

        // 计算总金额
        BigDecimal total = items.stream()
                .map(PrescriptionItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        prescription.setTotalAmount(total);
        prescription.setStatus(Prescription.STATUS_PENDING);

        Connection conn = null;
        try {
            conn = prescriptionDAO.getConnection();

            // 1. 保存处方主表
            Long prescriptionId = prescriptionDAO.insert(conn, prescription);
            prescription.setId(prescriptionId);

            // 2. 逐条处理明细（校验库存 + 扣减库存 + 记录日志 + 保存明细）
            for (PrescriptionItem item : items) {
                Medicine medicine = medicineDAO.findById(item.getMedicineId());
                if (medicine == null) {
                    throw new IllegalArgumentException("药品不存在: " + item.getMedicineId());
                }
                if (medicine.getStock() == null || medicine.getStock() < item.getQuantity()) {
                    throw new IllegalArgumentException(
                            "药品【" + medicine.getName() + "】库存不足，当前库存: " + medicine.getStock());
                }

                // 扣减库存
                medicineDAO.updateStock(conn, medicine.getId(), -item.getQuantity());

                // 记录库存日志
                InventoryLog log = new InventoryLog();
                log.setMedicineId(medicine.getId());
                log.setType(InventoryLog.TYPE_OUT);
                log.setQuantity(-item.getQuantity());
                log.setBatchNo(medicine.getBatchNo());
                log.setExpiryDate(medicine.getExpiryDate());
                log.setOperator("处方开立");
                log.setRemark("处方号: " + prescriptionId);
                inventoryLogDAO.insert(conn, log);

                // 保存处方明细
                item.setPrescriptionId(prescriptionId);
                itemDAO.insert(conn, item);
            }

            prescriptionDAO.commit(conn);
            logger.info("处方开立成功: id={}, patientId={}, totalAmount={}, items={}",
                    prescriptionId, prescription.getPatientId(), total, items.size());
            return prescriptionId;

        } catch (SQLException e) {
            prescriptionDAO.rollback(conn);
            logger.error("处方开立失败，事务已回滚", e);
            throw e;
        } catch (IllegalArgumentException e) {
            prescriptionDAO.rollback(conn);
            logger.warn("处方开立失败（业务校验），事务已回滚: {}", e.getMessage());
            throw e;
        } finally {
            prescriptionDAO.closeConnection(conn);
        }
    }

    // ============================================================
    // 取药（状态变更）
    // ============================================================

    public void dispensePrescription(Long prescriptionId) throws SQLException, IllegalArgumentException {
        Prescription p = prescriptionDAO.findById(prescriptionId);
        if (p == null) throw new IllegalArgumentException("处方不存在");
        if (p.getStatus() != Prescription.STATUS_PAID) {
            throw new IllegalArgumentException("仅已缴费的处方可取药");
        }
        prescriptionDAO.updateStatus(null, prescriptionId, Prescription.STATUS_DISPENSED);
        logger.info("处方取药完成: id={}", prescriptionId);
    }

    // ============================================================
    // 作废处方（回滚库存）
    // ============================================================

    public void cancelPrescription(Long prescriptionId) throws SQLException, IllegalArgumentException {
        Prescription p = prescriptionDAO.findById(prescriptionId);
        if (p == null) throw new IllegalArgumentException("处方不存在");
        if (p.getStatus() == Prescription.STATUS_DISPENSED) {
            throw new IllegalArgumentException("已取药的处方不能作废");
        }

        List<PrescriptionItem> items = itemDAO.findByPrescription(prescriptionId);

        Connection conn = null;
        try {
            conn = prescriptionDAO.getConnection();

            // 1. 回滚库存
            for (PrescriptionItem item : items) {
                medicineDAO.updateStock(conn, item.getMedicineId(), item.getQuantity());

                InventoryLog log = new InventoryLog();
                log.setMedicineId(item.getMedicineId());
                log.setType(InventoryLog.TYPE_SURPLUS);
                log.setQuantity(item.getQuantity());
                log.setOperator("处方作废");
                log.setRemark("处方号: " + prescriptionId);
                inventoryLogDAO.insert(conn, log);
            }

            // 2. 更新处方状态为已作废
            prescriptionDAO.updateStatus(conn, prescriptionId, Prescription.STATUS_VOID);

            prescriptionDAO.commit(conn);
            logger.info("处方作废成功: id={}", prescriptionId);

        } catch (SQLException e) {
            prescriptionDAO.rollback(conn);
            logger.error("处方作废失败，事务已回滚", e);
            throw e;
        } finally {
            prescriptionDAO.closeConnection(conn);
        }
    }

    // ============================================================
    // 查询方法
    // ============================================================

    public Prescription getById(Long id) throws SQLException {
        Prescription p = prescriptionDAO.findById(id);
        if (p != null) {
            p.setItems(itemDAO.findByPrescription(id));
        }
        return p;
    }

    public Prescription getByRecord(Long recordId) throws SQLException {
        Prescription p = prescriptionDAO.findByRecord(recordId);
        if (p != null) {
            p.setItems(itemDAO.findByPrescription(p.getId()));
        }
        return p;
    }

    public List<Prescription> listByPatient(Long patientId) throws SQLException {
        return prescriptionDAO.findByPatient(patientId);
    }

    public List<Prescription> listToday() throws SQLException {
        return prescriptionDAO.findToday();
    }

    public List<PrescriptionItem> listItems(Long prescriptionId) throws SQLException {
        return itemDAO.findByPrescription(prescriptionId);
    }

    // ============================================================
    // 私有校验
    // ============================================================

    private void validate(Prescription p, List<PrescriptionItem> items) {
        if (p == null) throw new IllegalArgumentException("处方信息不能为空");
        if (p.getPatientId() == null) throw new IllegalArgumentException("患者不能为空");
        if (p.getDoctorId() == null) throw new IllegalArgumentException("医生不能为空");
        if (p.getRecordId() == null) throw new IllegalArgumentException("病历记录不能为空");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("处方明细不能为空");
        for (PrescriptionItem item : items) {
            if (item.getMedicineId() == null) throw new IllegalArgumentException("药品不能为空");
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("药品数量必须大于0");
            }
        }
    }
}
