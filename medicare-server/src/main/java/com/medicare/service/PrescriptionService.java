package com.medicare.service;

import com.medicare.common.RedisLock;
import com.medicare.dto.InventoryLogVO;
import com.medicare.dto.PrescriptionItemVO;
import com.medicare.dto.PrescriptionListVO;
import com.medicare.dto.PrescriptionVO;
import com.medicare.entity.*;
import com.medicare.exception.BusinessException;
import com.medicare.repository.*;
import com.medicare.util.CodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 处方服务 — 开处方 / 取药 / 作废处方 / 库存日志。
 * <p>
 * 开立处方和作废处方时对涉及的药品批量加分布式锁，再结合数据库乐观锁（WHERE stock >= qty）
 * 保证多实例并发下库存扣减安全。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private static final String LOCK_PREFIX = "lock:medicine:";
    private static final Duration LOCK_EXPIRE = Duration.ofSeconds(15);
    private static final long LOCK_WAIT_MS = 5000;
    private static final long LOCK_RETRY_MS = 100;

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final RedisLock redisLock;
    private final DashboardService dashboardService;

    /**
     * 开立处方 — 事务操作：保存处方 + 批量分布式锁 + 逐条扣减库存 + 记录日志 + 保存明细。
     * 修复：库存扣减使用 safeDecrementStock 原子操作。
     */
    @Transactional
    public Prescription createPrescription(Prescription prescription, List<PrescriptionItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("处方明细不能为空");
        }

        // 按药品 ID 升序排序并加锁，避免多线程死锁
        List<Long> medicineIds = items.stream()
                .map(PrescriptionItem::getMedicineId)
                .distinct()
                .sorted()
                .toList();
        List<RedisLock.LockContext> locks = acquireLocks(medicineIds);

        try {
            // 保存处方主表
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (PrescriptionItem item : items) {
                item.setAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                totalAmount = totalAmount.add(item.getAmount());
            }
            prescription.setTotalAmount(totalAmount);
            prescription.setStatus(Prescription.STATUS_PENDING);
            prescription = prescriptionRepository.save(prescription);
            prescription.setCode(CodeUtils.generateCode("PRE", prescription.getId()));
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
                item = prescriptionItemRepository.save(item);
                item.setCode(CodeUtils.generateCode("PIT", item.getId()));
                prescriptionItemRepository.save(item);

                // 记录库存日志
                InventoryLog log = buildInventoryLog(item.getMedicineId(), InventoryLog.TYPE_STOCK_OUT,
                        item.getQuantity(), "处方出库 - 处方ID:" + prescription.getId());
                inventoryLogRepository.save(log);
            }

            // 清理仪表盘缓存
            dashboardService.clearStatsCache();
            return prescription;
        } finally {
            locks.forEach(RedisLock.LockContext::close);
        }
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
     * 作废处方 — 事务操作：批量分布式锁 + 逐条回滚库存 + 更新处方状态
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
        List<Long> medicineIds = items.stream()
                .map(PrescriptionItem::getMedicineId)
                .distinct()
                .sorted()
                .toList();
        List<RedisLock.LockContext> locks = acquireLocks(medicineIds);

        try {
            for (PrescriptionItem item : items) {
                medicineRepository.incrementStock(item.getMedicineId(), item.getQuantity());
                // 记录库存日志
                InventoryLog log = buildInventoryLog(item.getMedicineId(), InventoryLog.TYPE_STOCK_IN,
                        item.getQuantity(), "处方作废回滚 - 处方ID:" + id);
                inventoryLogRepository.save(log);
            }

            // 更新处方状态
            prescriptionRepository.cancel(id);

            // 清理仪表盘缓存
            dashboardService.clearStatsCache();
        } finally {
            locks.forEach(RedisLock.LockContext::close);
        }
    }

    private InventoryLog buildInventoryLog(Long medicineId, Integer type, Integer quantity, String remark) {
        InventoryLog log = new InventoryLog();
        log.setMedicineId(medicineId);
        log.setType(type);
        log.setQuantity(quantity);
        log.setOperator("system");
        log.setRemark(remark);
        log = inventoryLogRepository.save(log);
        log.setCode(CodeUtils.generateCode("INV", log.getId()));
        return inventoryLogRepository.save(log);
    }

    private List<RedisLock.LockContext> acquireLocks(List<Long> medicineIds) {
        List<RedisLock.LockContext> locks = new ArrayList<>();
        try {
            for (Long medicineId : medicineIds) {
                RedisLock.LockContext context = redisLock.tryLock(LOCK_PREFIX + medicineId, LOCK_EXPIRE,
                        Duration.ofMillis(LOCK_WAIT_MS), Duration.ofMillis(LOCK_RETRY_MS));
                if (context == null) {
                    throw new BusinessException("药品操作并发量过大，请稍后重试");
                }
                locks.add(context);
            }
            return locks;
        } catch (InterruptedException e) {
            locks.forEach(RedisLock.LockContext::close);
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请稍后重试");
        } catch (Exception e) {
            locks.forEach(RedisLock.LockContext::close);
            throw e;
        }
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
        vo.setCode(prescription.getCode());
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
            itemVO.setCode(item.getCode());
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
                itemVO.setMedicineCode(med.getCode());
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
