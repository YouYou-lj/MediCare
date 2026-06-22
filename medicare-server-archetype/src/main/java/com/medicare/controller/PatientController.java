package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.Result;
import com.medicare.entity.Patient;
import com.medicare.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 患者控制器 — 患者 CRUD + 搜索
 * <p>
 * 患者是挂号的前置依赖，身份证号唯一；删除前需确认无关联挂号记录
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    /** 查询全部患者列表 */
    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<Patient>> list() {
        return Result.ok(null);
    }

    /** 模糊搜索 — 按姓名/身份证号/手机号匹配 */
    @GetMapping("/search")
    @RequireRole({"admin", "doctor"})
    public Result<List<Patient>> search(@RequestParam String keyword) {
        return Result.ok(null);
    }

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<Patient> detail(@PathVariable Long id) {
        return Result.ok(null);
    }

    /** 创建患者 — 身份证号唯一性校验 */
    @PostMapping
    @RequireRole({"admin", "doctor"})
    public Result<Patient> create(@Valid @RequestBody Patient patient) {
        return Result.ok(null);
    }

    /** 更新患者 — 身份证号唯一性校验（排除自身） */
    @PutMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<Patient> update(@PathVariable Long id, @Valid @RequestBody Patient patient) {
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        return Result.ok();
    }
}
