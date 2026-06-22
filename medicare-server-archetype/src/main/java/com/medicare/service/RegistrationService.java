package com.medicare.service;

import com.medicare.dto.RegistrationVO;
import com.medicare.entity.Registration;
import com.medicare.entity.Schedule;
import com.medicare.exception.BusinessException;
import com.medicare.repository.PatientRepository;
import com.medicare.repository.RegistrationRepository;
import com.medicare.repository.ScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 挂号服务 — 核心业务逻辑（挂号/叫号/完成/取消）
 * <p>
 * 事务保证：挂号时号源扣减与记录保存原子性；取消时号源回增与状态更新原子性
 */
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final ScheduleRepository scheduleRepository;
    private final PatientRepository patientRepository;

    public List<RegistrationVO> findRegistrationVOList(LocalDate date, Integer status) {
        return null;
    }

    /**
     * 挂号 — 事务操作：原子扣减号源 + 保存挂号记录 + 从 schedule 复制 doctorId
     */
    @Transactional
    public Registration register(Long patientId, Long scheduleId) {
        return null;
    }

    /** 叫号 — 状态 0(候诊) → 1(就诊中) */
    @Transactional
    public void callPatient(Long id) {
    }

    /** 完成就诊 — 状态 1(就诊中) → 2(已完成) */
    @Transactional
    public void complete(Long id) {
    }

    /**
     * 取消挂号 — 事务操作：状态→已取消 + 回增号源
     */
    @Transactional
    public void cancel(Long id) {
    }
}
