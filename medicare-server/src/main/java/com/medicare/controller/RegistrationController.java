package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.RegistrationVO;
import com.medicare.dto.Result;
import com.medicare.entity.Registration;
import com.medicare.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 挂号控制器 — 挂号预约核心流程
 * <p>
 * 业务流程：管理员挂号(POST) → 叫号(PUT /{id}/call, 状态0→1) → 完成就诊(PUT /{id}/complete, 状态1→2)
 *                    └→ 取消挂号(DELETE /{id}, 状态→已取消 + 回增号源)
 * <p>
 * 挂号状态码：0=候诊  1=就诊中  2=已完成  3=已取消
 */
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /** 查询今日挂号列表（可按日期、状态筛选，默认今天） */
    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<RegistrationVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer status) {
        if (date == null) date = LocalDate.now();
        return Result.ok(registrationService.findTodayList(date, status));
    }

    /**
     * 挂号 — 原子扣减号源 + 保存挂号记录 + 从 schedule 复制 doctorId
     * 事务保证：号源扣减与挂号记录保存在同一事务内，防止超卖
     */
    @PostMapping
    @RequireRole("admin")
    public Result<Registration> register(@RequestBody Map<String, Long> body) {
        Long patientId = body.get("patientId");
        Long scheduleId = body.get("scheduleId");
        return Result.ok(registrationService.register(patientId, scheduleId));
    }

    /** 叫号 — 挂号状态 0(候诊) → 1(就诊中) */
    @PutMapping("/{id}/call")
    @RequireRole({"admin", "doctor"})
    public Result<Void> callPatient(@PathVariable Long id) {
        registrationService.callPatient(id);
        return Result.ok();
    }

    /** 完成就诊 — 挂号状态 1(就诊中) → 2(已完成) */
    @PutMapping("/{id}/complete")
    @RequireRole({"admin", "doctor"})
    public Result<Void> complete(@PathVariable Long id) {
        registrationService.completeRegistration(id);
        return Result.ok();
    }

    /** 取消挂号 — 状态→已取消 + 事务内回增号源 */
    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> cancel(@PathVariable Long id) {
        registrationService.cancelRegistration(id);
        return Result.ok();
    }
}
