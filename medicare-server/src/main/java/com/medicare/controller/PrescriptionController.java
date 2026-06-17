package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.InventoryLogVO;
import com.medicare.dto.PrescriptionCreateRequest;
import com.medicare.dto.PrescriptionListVO;
import com.medicare.dto.PrescriptionVO;
import com.medicare.dto.Result;
import com.medicare.entity.Prescription;
import com.medicare.entity.PrescriptionItem;
import com.medicare.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 处方控制器 — 开处方 / 取药 / 作废处方 / 库存日志
 * <p>
 * 业务流程：医生开处方(POST) → 药房取药(PUT /{id}/dispense) 或 作废处方(PUT /{id}/cancel)
 * <p>
 * 处方状态码：0=待缴费  1=已缴费/待取药  2=已取药  3=已作废
 * <p>
 * 开处方时自动扣减药品库存（safeDecrementStock 原子操作防超卖）；
 * 作废处方时逐条回滚库存并记录日志。
 */
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    /** 处方列表查询（可按患者ID、是否今天筛选） */
    @GetMapping
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<List<PrescriptionListVO>> list(@RequestParam(required = false) Long patientId,
                                            @RequestParam(required = false) Boolean today) {
        return Result.ok(prescriptionService.listPrescriptionVOs(patientId, today));
    }

    /** 处方详情 — 含明细项和关联的药品名称/规格/单位 */
    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<PrescriptionVO> detail(@PathVariable Long id) {
        return Result.ok(prescriptionService.findPrescriptionVOById(id));
    }

    /** 按病历ID查询处方（医生工作站：写完病历后查看对应处方） */
    @GetMapping("/by-record/{recordId}")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<PrescriptionVO> byRecord(@PathVariable Long recordId) {
        return Result.ok(prescriptionService.findByRecordId(recordId));
    }

    /**
     * 开立处方 — 事务操作：保存处方主表 + 逐条扣减库存 + 记录库存日志 + 保存明细项
     * 请求体使用 PrescriptionCreateRequest DTO 做参数校验，替代不安全的 Map
     */
    @PostMapping
    @RequireRole({"admin", "doctor"})
    public Result<Prescription> create(@Valid @RequestBody PrescriptionCreateRequest request) {
        PrescriptionCreateRequest.PrescriptionInfo info = request.getPrescription();
        Prescription prescription = new Prescription();
        prescription.setRecordId(info.getRecordId());
        prescription.setPatientId(info.getPatientId());
        prescription.setDoctorId(info.getDoctorId());

        List<PrescriptionItem> items = request.getItems().stream().map(itemInfo -> {
            PrescriptionItem item = new PrescriptionItem();
            item.setMedicineId(itemInfo.getMedicineId());
            item.setQuantity(itemInfo.getQuantity());
            item.setDosage(itemInfo.getDosage());
            item.setUsageDesc(itemInfo.getUsageDesc());
            item.setUnitPrice(itemInfo.getUnitPrice());
            return item;
        }).toList();

        return Result.ok(prescriptionService.createPrescription(prescription, items));
    }

    /** 取药 — 药房确认发药，处方状态→已取药 */
    @PutMapping("/{id}/dispense")
    @RequireRole({"admin", "pharmacist"})
    public Result<Void> dispense(@PathVariable Long id) {
        prescriptionService.dispense(id);
        return Result.ok();
    }

    /** 作废处方 — 逐条回滚库存 + 记录日志 + 更新处方状态 */
    @PutMapping("/{id}/cancel")
    @RequireRole({"admin", "pharmacist"})
    public Result<Void> cancel(@PathVariable Long id) {
        prescriptionService.cancelPrescription(id);
        return Result.ok();
    }

    /** 库存变动日志查询（可按药品ID筛选） */
    @GetMapping("/inventory-logs")
    @RequireRole({"admin", "pharmacist"})
    public Result<List<InventoryLogVO>> inventoryLogs(@RequestParam(required = false) Long medicineId) {
        return Result.ok(prescriptionService.findInventoryLogVOList(medicineId));
    }
}
