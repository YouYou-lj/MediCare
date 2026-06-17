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

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<RegistrationVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer status) {
        if (date == null) date = LocalDate.now();
        return Result.ok(registrationService.findTodayList(date, status));
    }

    @PostMapping
    @RequireRole("admin")
    public Result<Registration> register(@RequestBody Map<String, Long> body) {
        Long patientId = body.get("patientId");
        Long scheduleId = body.get("scheduleId");
        return Result.ok(registrationService.register(patientId, scheduleId));
    }

    @PutMapping("/{id}/call")
    @RequireRole({"admin", "doctor"})
    public Result<Void> callPatient(@PathVariable Long id) {
        registrationService.callPatient(id);
        return Result.ok();
    }

    @PutMapping("/{id}/complete")
    @RequireRole({"admin", "doctor"})
    public Result<Void> complete(@PathVariable Long id) {
        registrationService.completeRegistration(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> cancel(@PathVariable Long id) {
        registrationService.cancelRegistration(id);
        return Result.ok();
    }
}
