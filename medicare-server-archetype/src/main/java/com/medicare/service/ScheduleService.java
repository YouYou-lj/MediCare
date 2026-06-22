package com.medicare.service;

import com.medicare.dto.ScheduleVO;
import com.medicare.entity.Schedule;
import com.medicare.exception.BusinessException;
import com.medicare.repository.DoctorRepository;
import com.medicare.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班服务 — 排班 CRUD + 可用号源查询
 * <p>
 * 创建排班时自动将 remainSlots 设为 totalSlots；
 * 挂号时通过 ScheduleRepository.decrementRemain() 原子扣减号源
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;

    public List<ScheduleVO> findScheduleVOList(LocalDate date, Long deptId) {
        return null;
    }

    public List<ScheduleVO> findAvailableSchedules(LocalDate date, Long deptId) {
        return null;
    }

    public Schedule findById(Long id) {
        return null;
    }

    public Schedule create(Schedule schedule) {
        return null;
    }

    public Schedule update(Long id, Schedule schedule) {
        return null;
    }

    public void delete(Long id) {
    }
}
