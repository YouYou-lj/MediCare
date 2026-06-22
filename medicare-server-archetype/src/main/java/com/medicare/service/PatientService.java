package com.medicare.service;

import com.medicare.entity.Patient;
import com.medicare.exception.BusinessException;
import com.medicare.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 患者服务 — 患者 CRUD + 搜索，身份证号唯一性校验
 */
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public List<Patient> findAll() {
        return null;
    }

    public Patient findById(Long id) {
        return null;
    }

    public List<Patient> search(String keyword) {
        return null;
    }

    /** 创建患者 — 身份证号唯一性校验 */
    public Patient create(Patient patient) {
        return null;
    }

    /** 更新患者 — 身份证号唯一性校验（排除自身） */
    public Patient update(Long id, Patient patient) {
        return null;
    }

    public void delete(Long id) {
    }
}
