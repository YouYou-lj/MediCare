package com.medicare.service;

import com.medicare.dto.DashboardStats;
import com.medicare.entity.Medicine;
import com.medicare.repository.MedicineRepository;
import com.medicare.repository.PrescriptionRepository;
import com.medicare.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仪表盘服务 — 聚合统计数据
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RegistrationRepository registrationRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;

    /**
     * 聚合首页全部统计：基础数字 + 趋势 + 分布 + 低库存
     */
    public DashboardStats getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        DashboardStats stats = new DashboardStats();

        // === 基础统计数字 ===
        stats.setTodayRegCount(registrationRepository.findTodayList(today, null).size());
        stats.setWaitingCount(registrationRepository.findTodayList(today, 0).size());
        stats.setStockAlertCount(medicineRepository.findLowStockMedicines().size());
        stats.setCompletedCount(prescriptionRepository.countDispensedToday(today));
        stats.setPendingDispenseCount(prescriptionRepository.countPendingDispenseToday(today));

        // === 近7日挂号趋势 ===
        List<Object[]> regTrendRaw = registrationRepository.countRegByDateRange(weekAgo);
        List<DashboardStats.RegTrendItem> regTrend = new ArrayList<>();
        // 填充7天，没有数据的天补0
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString();
            long count = regTrendRaw.stream()
                    .filter(row -> row[0].toString().equals(dateStr))
                    .mapToLong(row -> ((Number) row[1]).longValue())
                    .findFirst()
                    .orElse(0L);
            DashboardStats.RegTrendItem item = new DashboardStats.RegTrendItem();
            item.setDate(dateStr);
            item.setCount(count);
            regTrend.add(item);
        }
        stats.setRegTrend(regTrend);

        // === 科室挂号分布 ===
        List<Object[]> deptRegRaw = registrationRepository.countTodayRegByDept(today);
        List<DashboardStats.DeptRegItem> deptReg = deptRegRaw.stream()
                .map(row -> {
                    DashboardStats.DeptRegItem item = new DashboardStats.DeptRegItem();
                    item.setDeptName(row[0] != null ? row[0].toString() : "未知科室");
                    item.setCount(((Number) row[1]).longValue());
                    return item;
                })
                .collect(Collectors.toList());
        stats.setDeptRegDistribution(deptReg);

        // === 低库存药品 Top5 ===
        List<Medicine> lowStockList = medicineRepository.findLowStockTopN(5);
        List<DashboardStats.LowStockItem> lowStockItems = lowStockList.stream()
                .map(m -> {
                    DashboardStats.LowStockItem item = new DashboardStats.LowStockItem();
                    item.setMedicineId(m.getId());
                    item.setName(m.getName());
                    item.setStock(m.getStock());
                    item.setSafetyStock(m.getSafetyStock());
                    return item;
                })
                .collect(Collectors.toList());
        stats.setLowStockTopN(lowStockItems);

        return stats;
    }
}
