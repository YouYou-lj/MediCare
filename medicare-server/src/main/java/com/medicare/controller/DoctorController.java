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

/**
 * 医生控制器 — 医生 CRUD + 按科室筛选
 * <p>
 * 医生隶属于科室，创建时校验科室是否存在；列表查询返回 DoctorVO（含科室名称）
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    /** 医生列表 — 可按科室ID筛选，返回含科室名的 VO */
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
