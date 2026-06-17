package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.MedicalRecordVO;
import com.medicare.dto.Result;
import com.medicare.entity.MedicalRecord;
import com.medicare.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<MedicalRecordVO>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId) {
        return Result.ok(medicalRecordService.findRecordVOList(patientId, doctorId));
    }

    @GetMapping("/by-registration/{registrationId}")
    @RequireRole({"admin", "doctor"})
    public Result<MedicalRecordVO> getByRegistration(@PathVariable Long registrationId) {
        return Result.ok(medicalRecordService.findRecordVOByRegistrationId(registrationId));
    }

    @PostMapping
    @RequireRole({"admin", "doctor"})
    public Result<MedicalRecord> create(@Valid @RequestBody MedicalRecord record) {
        return Result.ok(medicalRecordService.create(record));
    }

    @PutMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<MedicalRecord> update(@PathVariable Long id, @Valid @RequestBody MedicalRecord record) {
        return Result.ok(medicalRecordService.update(id, record));
    }
}
