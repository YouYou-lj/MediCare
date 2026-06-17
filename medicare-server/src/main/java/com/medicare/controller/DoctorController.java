package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.DoctorVO;
import com.medicare.dto.Result;
import com.medicare.entity.Doctor;
import com.medicare.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<DoctorVO>> list(@RequestParam(required = false) Long deptId) {
        return Result.ok(doctorService.findDoctorVOList(deptId));
    }

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<Doctor> detail(@PathVariable Long id) {
        return Result.ok(doctorService.findById(id));
    }

    @PostMapping
    @RequireRole("admin")
    public Result<Doctor> create(@Valid @RequestBody Doctor doctor) {
        return Result.ok(doctorService.create(doctor));
    }

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<Doctor> update(@PathVariable Long id, @Valid @RequestBody Doctor doctor) {
        return Result.ok(doctorService.update(id, doctor));
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return Result.ok();
    }
}
