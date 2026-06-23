package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.Result;
import com.medicare.entity.Patient;
import com.medicare.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 患者控制器 — 患者 CRUD + 搜索
 * <p>
 * 患者是挂号的前置依赖，身份证号唯一；删除前需确认无关联挂号记录
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "患者管理", description = "患者管理相关接口")
public class PatientController {

    private final PatientService patientService;

    /** 查询全部患者列表 */
    @GetMapping
    @RequireRole({"admin", "doctor"})
    @Operation(summary = "查询患者列表")
    public Result<List<Patient>> list() {
        return Result.ok(patientService.findAll());
    }

    /** 模糊搜索 — 按姓名/身份证号/手机号匹配 */
    @GetMapping("/search")
    @RequireRole({"admin", "doctor"})
    @Operation(summary = "搜索患者")
    public Result<List<Patient>> search(@RequestParam String keyword) {
        return Result.ok(patientService.search(keyword));
    }

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    @Operation(summary = "根据ID查询患者详情")
    public Result<Patient> detail(@PathVariable Long id) {
        return Result.ok(patientService.findById(id));
    }

    /** 创建患者 — 身份证号唯一性校验 */
    @PostMapping
    @RequireRole({"admin", "doctor"})
    @Operation(summary = "新增患者")
    public Result<Patient> create(@Valid @RequestBody Patient patient) {
        return Result.ok(patientService.create(patient));
    }

    /** 更新患者 — 身份证号唯一性校验（排除自身） */
    @PutMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    @Operation(summary = "更新患者")
    public Result<Patient> update(@PathVariable Long id, @Valid @RequestBody Patient patient) {
        return Result.ok(patientService.update(id, patient));
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    @Operation(summary = "删除患者")
    public Result<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return Result.ok();
    }
}
