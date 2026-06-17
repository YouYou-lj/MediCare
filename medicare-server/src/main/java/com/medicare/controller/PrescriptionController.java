package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.InventoryLogVO;
import com.medicare.dto.PrescriptionCreateRequest;
import com.medicare.dto.PrescriptionVO;
import com.medicare.dto.Result;
import com.medicare.entity.Prescription;
import com.medicare.entity.PrescriptionItem;
import com.medicare.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<List<PrescriptionVO>> list(@RequestParam(required = false) Long patientId,
                                            @RequestParam(required = false) Boolean today) {
        return Result.ok(prescriptionService.listPrescriptionVOs(patientId, today));
    }

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<PrescriptionVO> detail(@PathVariable Long id) {
        return Result.ok(prescriptionService.findPrescriptionVOById(id));
    }

    @GetMapping("/by-record/{recordId}")
    @RequireRole({"admin", "doctor", "pharmacist"})
    public Result<PrescriptionVO> byRecord(@PathVariable Long recordId) {
        return Result.ok(prescriptionService.findByRecordId(recordId));
    }

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

    @PutMapping("/{id}/dispense")
    @RequireRole({"admin", "pharmacist"})
    public Result<Void> dispense(@PathVariable Long id) {
        prescriptionService.dispense(id);
        return Result.ok();
    }

    @PutMapping("/{id}/cancel")
    @RequireRole({"admin", "pharmacist"})
    public Result<Void> cancel(@PathVariable Long id) {
        prescriptionService.cancelPrescription(id);
        return Result.ok();
    }

    @GetMapping("/inventory-logs")
    @RequireRole({"admin", "pharmacist"})
    public Result<List<InventoryLogVO>> inventoryLogs(@RequestParam(required = false) Long medicineId) {
        return Result.ok(prescriptionService.findInventoryLogVOList(medicineId));
    }
}
