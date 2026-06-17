package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.*;
import com.medicare.entity.Medicine;
import com.medicare.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<List<Medicine>> list(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return Result.ok(medicineService.search(keyword));
        }
        return Result.ok(medicineService.findAll());
    }

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

    @PostMapping("/{id}/stock-in")
    @RequireRole({"admin", "pharmacist"})
    public Result<Void> stockIn(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        medicineService.stockIn(id, request.getQuantity(), request.getBatchNo(),
                request.getExpiryDate(), request.getOperator(), request.getRemark());
        return Result.ok();
    }

    @PostMapping("/{id}/stock-out")
    @RequireRole({"admin", "pharmacist"})
    public Result<Void> stockOut(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        medicineService.stockOut(id, request.getQuantity(), request.getBatchNo(),
                request.getExpiryDate(), request.getOperator(), request.getRemark());
        return Result.ok();
    }
}
