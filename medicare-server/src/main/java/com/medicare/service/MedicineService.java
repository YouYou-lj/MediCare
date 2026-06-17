package com.medicare.service;

import com.medicare.entity.InventoryLog;
import com.medicare.entity.Medicine;
import com.medicare.exception.BusinessException;
import com.medicare.repository.InventoryLogRepository;
import com.medicare.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public List<Medicine> findAll() {
        return medicineRepository.findByStatus(1);
    }

    public List<Medicine> search(String keyword) {
        return medicineRepository.searchByKeyword(keyword);
    }

    public List<Medicine> findLowStock() {
        return medicineRepository.findLowStockMedicines();
    }

    public Medicine findById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException("药品不存在"));
    }

    public Medicine create(Medicine medicine) {
        if (medicineRepository.existsByNameAndSpec(medicine.getName(), medicine.getSpec())) {
            throw new BusinessException("该药品规格已存在");
        }
        return medicineRepository.save(medicine);
    }

    public Medicine update(Long id, Medicine medicine) {
        Medicine existing = findById(id);
        if (medicineRepository.existsByNameAndSpecAndIdNot(medicine.getName(), medicine.getSpec(), id)) {
            throw new BusinessException("该药品规格已存在");
        }
        existing.setName(medicine.getName());
        existing.setSpec(medicine.getSpec());
        existing.setUnit(medicine.getUnit());
        existing.setSafetyStock(medicine.getSafetyStock());
        existing.setPinyinCode(medicine.getPinyinCode());
        existing.setPrice(medicine.getPrice());
        existing.setManufacturer(medicine.getManufacturer());
        existing.setStatus(medicine.getStatus());
        return medicineRepository.save(existing);
    }

    public void delete(Long id) {
        if (!medicineRepository.existsById(id)) {
            throw new BusinessException("药品不存在");
        }
        medicineRepository.deleteById(id);
    }

    /**
     * 入库 — 事务操作：增加库存 + 记录日志
     */
    @Transactional
    public void stockIn(Long medicineId, int quantity, String batchNo, String expiryDate, String operator, String remark) {
        Medicine medicine = findById(medicineId);
        medicine.setStock(medicine.getStock() + quantity);
        medicineRepository.save(medicine);

        InventoryLog log = new InventoryLog();
        log.setMedicineId(medicineId);
        log.setType(InventoryLog.TYPE_STOCK_IN);
        log.setQuantity(quantity);
        log.setBatchNo(batchNo);
        log.setOperator(operator);
        log.setRemark(remark);
        inventoryLogRepository.save(log);
    }

    /**
     * 出库 — 事务操作：安全扣减库存 + 记录日志
     */
    @Transactional
    public void stockOut(Long medicineId, int quantity, String batchNo, String expiryDate, String operator, String remark) {
        int affected = medicineRepository.safeDecrementStock(medicineId, quantity);
        if (affected == 0) {
            throw new BusinessException("库存不足");
        }

        InventoryLog log = new InventoryLog();
        log.setMedicineId(medicineId);
        log.setType(InventoryLog.TYPE_STOCK_OUT);
        log.setQuantity(quantity);
        log.setBatchNo(batchNo);
        log.setOperator(operator);
        log.setRemark(remark);
        inventoryLogRepository.save(log);
    }

}
