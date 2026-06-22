package com.medicare.service;

import com.medicare.dto.DashboardStats;
import com.medicare.repository.MedicineRepository;
import com.medicare.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 仪表盘服务 — 聚合统计数据（今日挂号数、候诊数、库存预警数）
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RegistrationRepository registrationRepository;
    private final MedicineRepository medicineRepository;

    /**
     * 聚合统计：今日挂号数 + 候诊数 + 库存预警数
     * 注意：当前实现调用 3 次 SQL 查询，数据量大时可改为单次聚合查询
     */
    public DashboardStats getDashboardStats() {
        return null;
    }
}
