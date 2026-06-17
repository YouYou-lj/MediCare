package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.*;
import com.medicare.entity.Medicine;
import com.medicare.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 药品控制器 — 药品 CRUD + 库存管理（入库/出库/库存预警）
 * <p>
 * 入库流程：增加库存 + 记录库存日志（含批次号、有效期）
 * 出库流程：安全扣减库存（WHERE stock >= qty 防超卖） + 记录库存日志
 */
@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    /** 药品列表（支持关键词模糊搜索，无关键词时返回全部启用状态药品） */
    @GetMapping
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<List<Medicine>> list(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return Result.ok(medicineService.search(keyword));
        }
        return Result.ok(medicineService.findAll());
    }

    /** 库存预警 — 查询库存低于安全存量的药品 */
    @GetMapping("/low-stock")
    @RequireRole({"admin", "pharmacist"})
    public Result<List<Medicine>> lowStock() {
        return Result.ok(medicineService.findLowStock());
    }

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<Medicine> detail(@PathVariable Long id) {
        return Result.ok(medicineService.findById(id));
    }

    @PostMapping
    @RequireRole({"admin", "pharmacist"})
    public Result<Medicine> create(@Valid @RequestBody Medicine medicine) {
        return Result.ok(medicineService.create(medicine));
    }

    @PutMapping("/{id}")
    @RequireRole({"admin", "pharmacist"})
    public Result<Medicine> update(@PathVariable Long id, @Valid @RequestBody Medicine medicine) {
        return Result.ok(medicineService.update(id, medicine));
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        medicineService.delete(id);
        return Result.ok();
    }

    /** 入库 — 增加库存 + 记录日志（含批次号、有效期） */
    @PostMapping("/{id}/stock-in")
    @RequireRole({"admin", "pharmacist"})
    public Result<Void> stockIn(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        medicineService.stockIn(id, request.getQuantity(), request.getBatchNo(),
                request.getExpiryDate(), request.getOperator(), request.getRemark());
        return Result.ok();
    }

    /** 出库 — 安全扣减库存（防超卖）+ 记录日志 */
    @PostMapping("/{id}/stock-out")
    @RequireRole({"admin", "pharmacist"})
    public Result<Void> stockOut(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        medicineService.stockOut(id, request.getQuantity(), request.getBatchNo(),
                request.getExpiryDate(), request.getOperator(), request.getRemark());
        return Result.ok();
    }
}
