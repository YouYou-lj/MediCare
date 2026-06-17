package com.medicare.service;

import com.medicare.dto.InventoryLogVO;
import com.medicare.dto.PrescriptionItemVO;
import com.medicare.dto.PrescriptionListVO;
import com.medicare.dto.PrescriptionVO;
import com.medicare.entity.*;
import com.medicare.exception.BusinessException;
import com.medicare.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * 开立处方 — 事务操作：保存处方 + 逐条扣减库存 + 记录日志 + 保存明细
     * 修复：库存扣减使用 safeDecrementStock 原子操作
     */
    @Transactional
    public Prescription createPrescription(Prescription prescription, List<PrescriptionItem> items) {
        // 保存处方主表
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PrescriptionItem item : items) {
            item.setAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalAmount = totalAmount.add(item.getAmount());
        }
        prescription.setTotalAmount(totalAmount);
        prescription.setStatus(Prescription.STATUS_PENDING);
        prescription = prescriptionRepository.save(prescription);

        // 逐条扣减库存 + 记录日志 + 保存明细
        for (PrescriptionItem item : items) {
            // 安全扣减库存（WHERE stock >= :qty）
            int affected = medicineRepository.safeDecrementStock(item.getMedicineId(), item.getQuantity());
            if (affected == 0) {
                throw new BusinessException("药品库存不足：ID=" + item.getMedicineId());
            }

            // 保存明细
            item.setPrescriptionId(prescription.getId());
            prescriptionItemRepository.save(item);

            // 记录库存日志
            InventoryLog log = new InventoryLog();
            log.setMedicineId(item.getMedicineId());
            log.setType(InventoryLog.TYPE_STOCK_OUT);
            log.setQuantity(item.getQuantity());
            log.setOperator("system");
            log.setRemark("处方出库 - 处方ID:" + prescription.getId());
            inventoryLogRepository.save(log);
        }

        return prescription;
    }

    /**
     * 取药 — 状态改为已取药
     */
    @Transactional
    public void dispense(Long id) {
        int affected = prescriptionRepository.dispense(id);
        if (affected == 0) {
            throw new BusinessException("取药失败，处方可能不在待缴费/已缴费状态");
        }
    }

    /**
     * 作废处方 — 事务操作：逐条回滚库存 + 更新处方状态
     */
    @Transactional
    public void cancelPrescription(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("处方不存在"));
        if (prescription.getStatus() == Prescription.STATUS_CANCELLED) {
            throw new BusinessException("处方已作废");
        }
        if (prescription.getStatus() == Prescription.STATUS_DISPENSED) {
            throw new BusinessException("已取药处方不能作废");
        }

        // 逐条回滚库存
        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionId(id);
        for (PrescriptionItem item : items) {
            medicineRepository.incrementStock(item.getMedicineId(), item.getQuantity());
            // 记录库存日志
            InventoryLog log = new InventoryLog();
            log.setMedicineId(item.getMedicineId());
            log.setType(InventoryLog.TYPE_STOCK_IN);
            log.setQuantity(item.getQuantity());
            log.setOperator("system");
            log.setRemark("处方作废回滚 - 处方ID:" + id);
            inventoryLogRepository.save(log);
        }

        // 更新处方状态
        prescriptionRepository.cancel(id);
    }

    /**
     * 处方列表查询（带患者名、医生名）
     */
    public List<PrescriptionListVO> listPrescriptionVOs(Long patientId, Boolean today) {
        LocalDate todayDate = (today != null && today) ? LocalDate.now() : null;
        return prescriptionRepository.findPrescriptionVOList(patientId, todayDate);
    }

    /**
     * 查询处方详情（带明细和关联名称）
     * 优化：使用批量查询替代 N+1
     */
    public PrescriptionVO findPrescriptionVOById(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("处方不存在"));

        PrescriptionVO vo = new PrescriptionVO();
        vo.setId(prescription.getId());
        vo.setRecordId(prescription.getRecordId());
        vo.setPatientId(prescription.getPatientId());
        vo.setDoctorId(prescription.getDoctorId());
        vo.setTotalAmount(prescription.getTotalAmount());
        vo.setStatus(prescription.getStatus());
        vo.setCreateTime(prescription.getCreateTime());

        // 关联名称
        patientRepository.findById(prescription.getPatientId())
                .ifPresent(p -> vo.setPatientName(p.getName()));
        doctorRepository.findById(prescription.getDoctorId())
                .ifPresent(d -> vo.setDoctorName(d.getName()));

        // 明细列表 — 批量查询药品信息，避免 N+1
        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionId(id);
        List<Long> medicineIds = items.stream().map(PrescriptionItem::getMedicineId).distinct().toList();
        Map<Long, Medicine> medicineMap = medicineRepository.findAllById(medicineIds)
                .stream().collect(Collectors.toMap(Medicine::getId, Function.identity()));

        List<PrescriptionItemVO> itemVOs = new ArrayList<>();
        for (PrescriptionItem item : items) {
            PrescriptionItemVO itemVO = new PrescriptionItemVO();
            itemVO.setId(item.getId());
            itemVO.setPrescriptionId(item.getPrescriptionId());
            itemVO.setMedicineId(item.getMedicineId());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setDosage(item.getDosage());
            itemVO.setUsageDesc(item.getUsageDesc());
            itemVO.setUnitPrice(item.getUnitPrice());
            itemVO.setAmount(item.getAmount());
            Medicine med = medicineMap.get(item.getMedicineId());
            if (med != null) {
                itemVO.setMedicineName(med.getName());
                itemVO.setMedicineSpec(med.getSpec());
                itemVO.setMedicineUnit(med.getUnit());
            }
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);

        return vo;
    }

    /**
     * 按病历ID查询处方
     */
    public PrescriptionVO findByRecordId(Long recordId) {
        return prescriptionRepository.findByRecordId(recordId)
                .map(p -> findPrescriptionVOById(p.getId()))
                .orElse(null);
    }

    public List<InventoryLogVO> findInventoryLogVOList(Long medicineId) {
        return inventoryLogRepository.findLogVOList(medicineId);
    }
}
