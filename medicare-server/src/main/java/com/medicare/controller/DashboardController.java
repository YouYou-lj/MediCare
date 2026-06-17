package com.medicare.controller;

import com.medicare.dto.DashboardStats;
import com.medicare.dto.Result;
import com.medicare.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘控制器 — 首页统计数据
 * <p>
 * 返回今日挂号数、候诊人数、库存预警数，供前端 Dashboard 展示
 */
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
