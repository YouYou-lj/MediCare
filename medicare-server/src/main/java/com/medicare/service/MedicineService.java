package com.medicare.service;

import com.medicare.common.CacheKey;
import com.medicare.common.RedisLock;
import com.medicare.entity.InventoryLog;
import com.medicare.entity.Medicine;
import com.medicare.exception.BusinessException;
import com.medicare.repository.InventoryLogRepository;
import com.medicare.repository.MedicineRepository;
import com.medicare.util.CodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 药品服务 — 药品 CRUD + 库存出入库。
 * <p>
 * 查询结果使用 Redis 缓存，库存变更通过分布式锁 + 数据库乐观锁双重保护，
 * 保证多实例并发下库存不会扣成负数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "medicines")
public class MedicineService {

    private static final String LOCK_PREFIX = "lock:medicine:";
    private static final java.time.Duration LOCK_EXPIRE = java.time.Duration.ofSeconds(10);
    private static final long LOCK_WAIT_MS = 3000;
    private static final long LOCK_RETRY_MS = 100;

    private final MedicineRepository medicineRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final RedisLock redisLock;
    private final DashboardService dashboardService;

    @Cacheable(key = "'all'", unless = "#result == null")
    public List<Medicine> findAll() {
        return medicineRepository.findByStatus(1);
    }

    @Cacheable(key = "'search:' + #keyword", unless = "#result == null")
    public List<Medicine> search(String keyword) {
        return medicineRepository.searchByKeyword(keyword);
    }

    @Cacheable(key = "'low-stock'", unless = "#result == null")
    public List<Medicine> findLowStock() {
        return medicineRepository.findLowStockMedicines();
    }

    @Cacheable(key = "#id", unless = "#result == null")
    public Medicine findById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException("药品不存在"));
    }

    @CacheEvict(allEntries = true)
    public Medicine create(Medicine medicine) {
        if (medicineRepository.existsByNameAndSpec(medicine.getName(), medicine.getSpec())) {
            throw new BusinessException("该药品规格已存在");
        }
        medicine = medicineRepository.save(medicine);
        medicine.setCode(CodeUtils.generateCode("MED", medicine.getId()));
        return medicineRepository.save(medicine);
    }

    @CacheEvict(allEntries = true)
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

    @CacheEvict(allEntries = true)
    public void delete(Long id) {
        if (!medicineRepository.existsById(id)) {
            throw new BusinessException("药品不存在");
        }
        medicineRepository.deleteById(id);
    }

    /**
     * 入库 — 事务操作：增加库存 + 记录日志。
     * 对同一药品加分布式锁，避免并发导致库存不一致。
     */
    @CacheEvict(allEntries = true)
    @Transactional
    public void stockIn(Long medicineId, int quantity, String batchNo, String expiryDate, String operator, String remark) {
        String lockKey = LOCK_PREFIX + medicineId;
        try (RedisLock.LockContext ignored = acquireLock(lockKey)) {
            Medicine medicine = findById(medicineId);
            medicine.setStock(medicine.getStock() + quantity);
            medicineRepository.save(medicine);

            InventoryLog log = buildInventoryLog(medicineId, InventoryLog.TYPE_STOCK_IN, quantity,
                    batchNo, expiryDate, operator, remark);
            inventoryLogRepository.save(log);

            // 清理仪表盘缓存
            dashboardService.clearStatsCache();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请稍后重试");
        }
    }

    /**
     * 出库 — 事务操作：安全扣减库存 + 记录日志。
     * 对同一药品加分布式锁，再使用数据库乐观锁（WHERE stock >= qty）扣减。
     */
    @CacheEvict(allEntries = true)
    @Transactional
    public void stockOut(Long medicineId, int quantity, String batchNo, String expiryDate, String operator, String remark) {
        String lockKey = LOCK_PREFIX + medicineId;
        try (RedisLock.LockContext ignored = acquireLock(lockKey)) {
            int affected = medicineRepository.safeDecrementStock(medicineId, quantity);
            if (affected == 0) {
                throw new BusinessException("库存不足");
            }

            InventoryLog log = buildInventoryLog(medicineId, InventoryLog.TYPE_STOCK_OUT, quantity,
                    batchNo, expiryDate, operator, remark);
            inventoryLogRepository.save(log);

            // 清理仪表盘缓存
            dashboardService.clearStatsCache();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请稍后重试");
        }
    }

    private InventoryLog buildInventoryLog(Long medicineId, Integer type, Integer quantity,
                                           String batchNo, String expiryDate, String operator, String remark) {
        InventoryLog log = new InventoryLog();
        log.setMedicineId(medicineId);
        log.setType(type);
        log.setQuantity(quantity);
        log.setBatchNo(batchNo);
        if (expiryDate != null && !expiryDate.isBlank()) {
            log.setExpiryDate(LocalDate.parse(expiryDate));
        }
        log.setOperator(operator);
        log.setRemark(remark);
        log = inventoryLogRepository.save(log);
        log.setCode(CodeUtils.generateCode("INV", log.getId()));
        return inventoryLogRepository.save(log);
    }

    private RedisLock.LockContext acquireLock(String lockKey) throws InterruptedException {
        RedisLock.LockContext context = redisLock.tryLock(lockKey, LOCK_EXPIRE,
                java.time.Duration.ofMillis(LOCK_WAIT_MS), java.time.Duration.ofMillis(LOCK_RETRY_MS));
        if (context == null) {
            throw new BusinessException("操作过于频繁，请稍后重试");
        }
        return context;
    }
}
