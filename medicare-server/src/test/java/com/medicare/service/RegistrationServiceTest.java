package com.medicare.service;

import com.medicare.dto.RegistrationVO;
import com.medicare.entity.Registration;
import com.medicare.entity.Schedule;
import com.medicare.exception.BusinessException;
import com.medicare.repository.RegistrationRepository;
import com.medicare.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RegistrationService 单元测试 — 覆盖挂号、叫号、完成、取消
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private Schedule sampleSchedule;
    private Registration sampleRegistration;

    @BeforeEach
    void setUp() {
        sampleSchedule = new Schedule();
        sampleSchedule.setId(1L);
        sampleSchedule.setDoctorId(2L);
        sampleSchedule.setWorkDate(LocalDate.now());
        sampleSchedule.setTimeSlot("上午");
        sampleSchedule.setTotalSlots(20);
        sampleSchedule.setRemainSlots(15);

        sampleRegistration = new Registration();
        sampleRegistration.setId(1L);
        sampleRegistration.setPatientId(3L);
        sampleRegistration.setScheduleId(1L);
        sampleRegistration.setDoctorId(2L);
        sampleRegistration.setStatus(Registration.STATUS_WAITING);
        sampleRegistration.setSeqNo(1);
        sampleRegistration.setFee(BigDecimal.TEN);
    }

    @Test
    void findTodayList_shouldReturnList() {
        RegistrationVO vo = mock(RegistrationVO.class);
        when(registrationRepository.findTodayList(LocalDate.now(), null)).thenReturn(Collections.singletonList(vo));
        List<RegistrationVO> result = registrationService.findTodayList(LocalDate.now(), null);
        assertEquals(1, result.size());
    }

    @Test
    void register_success_shouldReturnRegistration() {
        when(scheduleRepository.decrementRemain(1L)).thenReturn(1);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(sampleSchedule));
        when(registrationRepository.countByScheduleAndNotCancelled(1L)).thenReturn(5L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        Registration result = registrationService.register(3L, 1L);
        assertEquals(Registration.STATUS_WAITING, result.getStatus());
        assertEquals(6, result.getSeqNo());
    }

    @Test
    void register_noSlots_shouldThrowBusinessException() {
        when(scheduleRepository.decrementRemain(1L)).thenReturn(0);
        assertThrows(BusinessException.class, () -> registrationService.register(3L, 1L));
    }

    @Test
    void callPatient_success_shouldNotThrow() {
        when(registrationRepository.callPatient(1L)).thenReturn(1);
        assertDoesNotThrow(() -> registrationService.callPatient(1L));
    }

    @Test
    void callPatient_failed_shouldThrowBusinessException() {
        when(registrationRepository.callPatient(1L)).thenReturn(0);
        assertThrows(BusinessException.class, () -> registrationService.callPatient(1L));
    }

    @Test
    void completeRegistration_success_shouldNotThrow() {
        when(registrationRepository.completeRegistration(1L)).thenReturn(1);
        assertDoesNotThrow(() -> registrationService.completeRegistration(1L));
    }

    @Test
    void completeRegistration_failed_shouldThrowBusinessException() {
        when(registrationRepository.completeRegistration(1L)).thenReturn(0);
        assertThrows(BusinessException.class, () -> registrationService.completeRegistration(1L));
    }

    @Test
    void cancelRegistration_waiting_shouldSucceed() {
        sampleRegistration.setStatus(Registration.STATUS_WAITING);
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(sampleRegistration));
        when(registrationRepository.save(any(Registration.class))).thenReturn(sampleRegistration);
        when(scheduleRepository.incrementRemain(1L)).thenReturn(1);

        assertDoesNotThrow(() -> registrationService.cancelRegistration(1L));
        assertEquals(Registration.STATUS_CANCELLED, sampleRegistration.getStatus());
    }

    @Test
    void cancelRegistration_alreadyCancelled_shouldThrowBusinessException() {
        sampleRegistration.setStatus(Registration.STATUS_CANCELLED);
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(sampleRegistration));
        assertThrows(BusinessException.class, () -> registrationService.cancelRegistration(1L));
    }

    @Test
    void cancelRegistration_completed_shouldThrowBusinessException() {
        sampleRegistration.setStatus(Registration.STATUS_COMPLETED);
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(sampleRegistration));
        assertThrows(BusinessException.class, () -> registrationService.cancelRegistration(1L));
    }
}
