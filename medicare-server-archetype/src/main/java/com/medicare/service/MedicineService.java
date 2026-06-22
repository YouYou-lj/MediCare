package com.medicare.service;

import com.medicare.dto.InventoryLogVO;
import com.medicare.entity.InventoryLog;
import com.medicare.entity.Medicine;
import com.medicare.exception.BusinessException;
import com.medicare.repository.InventoryLogRepository;
import com.medicare.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 药品服务 — 药品 CRUD + 库存管理（入库/出库）+ 库存预警
 * <p>
 * 入库：增加库存 + 记录日志；出库：安全扣减库存（防超卖）+ 记录日志
 */
@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public List<Medicine> findAll(String keyword) {
        return null;
    }

    public Medicine findById(Long id) {
        return null;
    }

    /** 查询库存低于安全存量的药品（库存预警） */
    public List<Medicine> findLowStock() {
        return null;
    }

    public Medicine create(Medicine medicine) {
        return null;
    }

    public Medicine update(Long id, Medicine medicine) {
        return null;
    }

    /** 删除药品 — 禁止删除库存大于0的药品（需先出库清零） */
    public void delete(Long id) {
    }

    /**
     * 入库 — 增加库存 + 记录日志（含批次号、有效期）
     */
    @Transactional
    public void stockIn(Long id, int quantity, String batchNo, LocalDateTime expireDate) {
    }

    /**
     * 出库 — 安全扣减库存（WHERE stock >= qty 防超卖）+ 记录日志
     */
    @Transactional
    public void stockOut(Long id, int quantity, String batchNo) {
    }
}
