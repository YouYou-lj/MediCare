package com.medicare.service;

import com.medicare.dto.DashboardStats;
import com.medicare.repository.MedicineRepository;
import com.medicare.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RegistrationRepository registrationRepository;
    private final MedicineRepository medicineRepository;

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        // 今日挂号数
        stats.setTodayRegCount(registrationRepository.findTodayList(LocalDate.now(), null).size());
        // 候诊数
        stats.setWaitingCount(registrationRepository.findTodayList(LocalDate.now(), 0).size());
        // 库存预警数
        stats.setStockAlertCount(medicineRepository.findLowStockMedicines().size());
        return stats;
    }
}
