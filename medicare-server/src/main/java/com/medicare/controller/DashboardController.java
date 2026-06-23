package com.medicare.controller;

import com.medicare.dto.DashboardStats;
import com.medicare.dto.Result;
import com.medicare.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 仪表盘控制器 — 首页统计数据
 * <p>
 * 返回今日挂号数、候诊人数、库存预警数，供前端 Dashboard 展示
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "首页仪表盘", description = "首页仪表盘相关接口")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "获取仪表盘统计数据")
    public Result<DashboardStats> stats() {
        return Result.ok(dashboardService.getDashboardStats());
    }
}
