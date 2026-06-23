package com.medicare.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计
 */
@Data
public class DashboardStats {

    private long todayRegCount;      // 今日挂号数
    private long waitingCount;       // 候诊人数
    private long stockAlertCount;    // 库存预警数
    private long completedCount;       // 今日已完成就诊
    private long pendingDispenseCount; // 待取药处方数

    // 近7日挂号趋势
    private List<RegTrendItem> regTrend;

    // 科室挂号分布
    private List<DeptRegItem> deptRegDistribution;

    // 低库存药品 TopN
    private List<LowStockItem> lowStockTopN;

    @Data
    public static class RegTrendItem {
        private String date;
        private long count;
    }

    @Data
    public static class DeptRegItem {
        private String deptName;
        private long count;
    }

    @Data
    public static class LowStockItem {
        private Long medicineId;
        private String name;
        private Integer stock;
        private Integer safetyStock;
    }
}
