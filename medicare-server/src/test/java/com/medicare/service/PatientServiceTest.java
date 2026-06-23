package com.medicare.service;

import com.medicare.entity.Patient;
import com.medicare.exception.BusinessException;
import com.medicare.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PatientService 单元测试 — 覆盖患者 CRUD + 搜索 + 唯一性校验
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private Patient samplePatient;

    @BeforeEach
    void setUp() {
        samplePatient = new Patient();
        samplePatient.setId(1L);
        samplePatient.setIdCard("320106199901011234");
        samplePatient.setName("张三");
        samplePatient.setGender(1);
        samplePatient.setBirthDate(java.time.LocalDate.of(1999, 1, 1));
        samplePatient.setPhone("13800138000");
        samplePatient.setAddress("南京市");
        samplePatient.setAllergyInfo("无");
    }

    @Test
    void findAll_shouldReturnList() {
        when(patientRepository.findAll()).thenReturn(Collections.singletonList(samplePatient));
        List<Patient> result = patientService.findAll();
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getName());
    }

    @Test
    void findById_existing_shouldReturnPatient() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        Patient result = patientService.findById(1L);
        assertEquals("张三", result.getName());
    }

    @Test
    void findById_notFound_shouldThrowBusinessException() {
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> patientService.findById(1L));
    }

    @Test
    void search_shouldReturnList() {
        when(patientRepository.search("张")).thenReturn(Collections.singletonList(samplePatient));
        List<Patient> result = patientService.search("张");
        assertEquals(1, result.size());
    }

    @Test
    void create_success_shouldReturnSavedPatient() {
        when(patientRepository.existsByIdCard("320106199901011234")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(samplePatient);
        Patient result = patientService.create(samplePatient);
        assertEquals("张三", result.getName());
    }

    @Test
    void create_duplicateIdCard_shouldThrowBusinessException() {
        when(patientRepository.existsByIdCard("320106199901011234")).thenReturn(true);
        assertThrows(BusinessException.class, () -> patientService.create(samplePatient));
    }

    @Test
    void update_success_shouldReturnUpdatedPatient() {
        Patient updated = new Patient();
        updated.setIdCard("320106199901011234");
        updated.setName("张三-修改");
        updated.setGender(1);
        updated.setBirthDate(java.time.LocalDate.of(1999, 1, 1));
        updated.setPhone("13900139000");
        updated.setAddress("南京市玄武区");
        updated.setAllergyInfo("青霉素过敏");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(patientRepository.existsByIdCardAndIdNot("320106199901011234", 1L)).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(updated);

        Patient result = patientService.update(1L, updated);
        assertEquals("张三-修改", result.getName());
    }

    @Test
    void update_duplicateIdCard_shouldThrowBusinessException() {
        Patient updated = new Patient();
        updated.setIdCard("320106199901011234");
        updated.setName("张三");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(patientRepository.existsByIdCardAndIdNot("320106199901011234", 1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> patientService.update(1L, updated));
    }

    @Test
    void delete_existing_shouldSucceed() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        doNothing().when(patientRepository).deleteById(1L);
        assertDoesNotThrow(() -> patientService.delete(1L));
    }

    @Test
    void delete_notFound_shouldThrowBusinessException() {
        when(patientRepository.existsById(1L)).thenReturn(false);
        assertThrows(BusinessException.class, () -> patientService.delete(1L));
    }
}
