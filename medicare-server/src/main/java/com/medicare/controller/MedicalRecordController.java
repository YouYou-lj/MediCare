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

/**
 * 病历控制器 — 医生工作站写病历
 * <p>
 * 业务流程：叫号后 → 创建病历(POST) → 查看/编辑病历 → 开处方
 * 一个挂号只能对应一份病历（1:1 关系，由 Service 层做唯一性校验）
 */
@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    /** 病历列表查询（支持按患者ID或挂号ID筛选） */
    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<MedicalRecordVO>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long registrationId) {
        return Result.ok(medicalRecordService.findRecordVOList(patientId, registrationId));
    }

    /** 病历详情（按ID查询，返回含关联名称的 VO） */
    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<MedicalRecordVO> detail(@PathVariable Long id) {
        return Result.ok(medicalRecordService.findRecordVOById(id));
    }

    /** 按挂号ID查询病历（医生工作站：叫号后查看该挂号的病历） */
    @GetMapping("/by-registration/{registrationId}")
    @RequireRole({"admin", "doctor"})
    public Result<MedicalRecordVO> getByRegistration(@PathVariable Long registrationId) {
        return Result.ok(medicalRecordService.findRecordVOByRegistrationId(registrationId));
    }

    /** 创建病历 — 同一挂号不允许重复创建 */
    @PostMapping
    @RequireRole({"admin", "doctor"})
    public Result<MedicalRecord> create(@Valid @RequestBody MedicalRecord record) {
        return Result.ok(medicalRecordService.create(record));
    }

    /** 更新病历 — 仅允许修改主诉、现病史、既往史、体格检查、诊断、医嘱 */
    @PutMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<MedicalRecord> update(@PathVariable Long id, @Valid @RequestBody MedicalRecord record) {
        return Result.ok(medicalRecordService.update(id, record));
    }
}
