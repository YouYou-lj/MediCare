package com.medicare.service;

import com.medicare.dto.RegistrationVO;
import com.medicare.entity.Registration;
import com.medicare.entity.Schedule;
import com.medicare.exception.BusinessException;
import com.medicare.repository.RegistrationRepository;
import com.medicare.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final ScheduleRepository scheduleRepository;

    public List<RegistrationVO> findTodayList(LocalDate date, Integer status) {
        return registrationRepository.findTodayList(date, status);
    }

    public List<RegistrationVO> findByDoctorAndStatusIn(Long doctorId, List<Integer> statuses) {
        return registrationRepository.findByDoctorAndStatusIn(doctorId, statuses);
    }

    public List<RegistrationVO> findByPatientId(Long patientId) {
        return registrationRepository.findByPatientId(patientId);
    }

    /**
     * 挂号 — 事务操作：原子扣减号源 + 保存挂号记录
     * 修复：号源扣减现在在事务内执行，原项目此操作脱离事务
     */
    @Transactional
    public Registration register(Long patientId, Long scheduleId) {
        // 1. 原子扣减号源（WHERE remain_slots > 0 防超卖）
        int affected = scheduleRepository.decrementRemain(scheduleId);
        if (affected == 0) {
            throw new BusinessException("号源不足，请选择其他号源");
        }

        // 2. 获取排班信息
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException("排班不存在"));

        // 3. 分配序号
        long seqNo = registrationRepository.countByScheduleAndNotCancelled(scheduleId) + 1;

        // 4. 创建挂号记录
        Registration reg = new Registration();
        reg.setPatientId(patientId);
        reg.setScheduleId(scheduleId);
        reg.setDoctorId(schedule.getDoctorId());  // 修复：从 schedule 获取 doctorId
        reg.setStatus(Registration.STATUS_WAITING);
        reg.setSeqNo((int) seqNo);
        reg.setFee(java.math.BigDecimal.TEN);  // 默认挂号费 10 元
        return registrationRepository.save(reg);
    }

    /**
     * 叫号 — 状态 0→1
     */
    @Transactional
    public void callPatient(Long id) {
        int affected = registrationRepository.callPatient(id);
        if (affected == 0) {
            throw new BusinessException("叫号失败，可能该挂号不在候诊状态");
        }
    }

    /**
     * 完成就诊 — 状态 1→2
     */
    @Transactional
    public void completeRegistration(Long id) {
        int affected = registrationRepository.completeRegistration(id);
        if (affected == 0) {
            throw new BusinessException("完成就诊失败，可能该挂号不在就诊中状态");
        }
    }

    /**
     * 取消挂号 — 状态改为已取消 + 回增号源
     */
    @Transactional
    public void cancelRegistration(Long id) {
        Registration reg = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("挂号不存在"));
        if (reg.getStatus() == Registration.STATUS_CANCELLED) {
            throw new BusinessException("该挂号已取消");
        }
        if (reg.getStatus() == Registration.STATUS_COMPLETED) {
            throw new BusinessException("已完成的挂号不能取消");
        }
        reg.setStatus(Registration.STATUS_CANCELLED);
        registrationRepository.save(reg);
        // 回增号源
        scheduleRepository.incrementRemain(reg.getScheduleId());
    }
}
