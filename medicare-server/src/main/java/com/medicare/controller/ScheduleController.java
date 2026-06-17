package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.Result;
import com.medicare.dto.ScheduleVO;
import com.medicare.entity.Schedule;
import com.medicare.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班控制器 — 排班 CRUD + 可用号源查询
 * <p>
 * 排班是挂号的前置依赖，包含日期、时段、总号源、剩余号源；
 * 创建时自动将剩余号源设为总号源数；挂号时原子扣减剩余号源
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /** 排班列表 — 可按日期、科室筛选，返回含医生名/科室名的 VO */
    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<ScheduleVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long deptId) {
        return Result.ok(scheduleService.findScheduleVOList(date, deptId));
    }

    /** 可用号源 — 仅返回剩余号源 > 0 的排班（供挂号选择） */
    @GetMapping("/available")
    @RequireRole({"admin", "doctor"})
    public Result<List<ScheduleVO>> available(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long deptId) {
        return Result.ok(scheduleService.findAvailableSchedules(date, deptId));
    }

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<Schedule> detail(@PathVariable Long id) {
        return Result.ok(scheduleService.findById(id));
    }

    /** 创建排班 — 校验医生存在 + 自动初始化剩余号源 */
    @PostMapping
    @RequireRole("admin")
    public Result<Schedule> create(@Valid @RequestBody Schedule schedule) {
        return Result.ok(scheduleService.create(schedule));
    }

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<Schedule> update(@PathVariable Long id, @Valid @RequestBody Schedule schedule) {
        return Result.ok(scheduleService.update(id, schedule));
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return Result.ok();
    }
}
