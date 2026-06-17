package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.InventoryLogVO;
import com.medicare.dto.PrescriptionVO;
import com.medicare.dto.Result;
import com.medicare.entity.Prescription;
import com.medicare.entity.PrescriptionItem;
import com.medicare.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public Result<Prescription> create(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> prescMap = (Map<String, Object>) body.get("prescription");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsMaps = (List<Map<String, Object>>) body.get("items");

        Prescription prescription = new Prescription();
        prescription.setRecordId(((Number) prescMap.get("recordId")).longValue());
        prescription.setPatientId(((Number) prescMap.get("patientId")).longValue());
        prescription.setDoctorId(((Number) prescMap.get("doctorId")).longValue());

        List<PrescriptionItem> items = itemsMaps.stream().map(m -> {
            PrescriptionItem item = new PrescriptionItem();
            item.setMedicineId(((Number) m.get("medicineId")).longValue());
            item.setQuantity(((Number) m.get("quantity")).intValue());
            item.setDosage((String) m.get("dosage"));
            item.setUsageDesc((String) m.get("usageDesc"));
            item.setUnitPrice(new java.math.BigDecimal(m.get("unitPrice").toString()));
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
