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

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<ScheduleVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long deptId) {
        return Result.ok(scheduleService.findScheduleVOList(date, deptId));
    }

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
