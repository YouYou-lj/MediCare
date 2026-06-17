package com.medicare.controller;

import com.medicare.dto.DashboardStats;
import com.medicare.dto.Result;
import com.medicare.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardStats> stats() {
        return Result.ok(dashboardService.getDashboardStats());
    }
}
