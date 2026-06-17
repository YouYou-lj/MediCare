package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.PageResult;
import com.medicare.dto.Result;
import com.medicare.entity.Patient;
import com.medicare.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<PageResult<Patient>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Patient> pageData = patientService.findAll(PageRequest.of(page - 1, size));
        return Result.ok(new PageResult<>(pageData.getContent(), pageData.getTotalElements(), page, size));
    }

    @GetMapping("/search")
    @RequireRole({"admin", "doctor"})
    public Result<List<Patient>> search(@RequestParam String keyword) {
        return Result.ok(patientService.search(keyword));
    }

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<Patient> detail(@PathVariable Long id) {
        return Result.ok(patientService.findById(id));
    }

    @PostMapping
    @RequireRole({"admin", "doctor"})
    public Result<Patient> create(@Valid @RequestBody Patient patient) {
        return Result.ok(patientService.create(patient));
    }

    @PutMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<Patient> update(@PathVariable Long id, @Valid @RequestBody Patient patient) {
        return Result.ok(patientService.update(id, patient));
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return Result.ok();
    }
}
