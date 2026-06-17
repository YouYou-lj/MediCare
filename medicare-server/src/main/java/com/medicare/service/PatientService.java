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
        return patientRepository.findAll();
    }

    public Patient findById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("患者不存在"));
    }

    public List<Patient> search(String keyword) {
        return patientRepository.search(keyword);
    }

    /** 创建患者 — 身份证号唯一性校验 */
    public Patient create(Patient patient) {
        if (patientRepository.existsByIdCard(patient.getIdCard())) {
            throw new BusinessException("身份证号已存在");
        }
        return patientRepository.save(patient);
    }

    /** 更新患者 — 身份证号唯一性校验（排除自身） */
    public Patient update(Long id, Patient patient) {
        Patient existing = findById(id);
        if (patientRepository.existsByIdCardAndIdNot(patient.getIdCard(), id)) {
            throw new BusinessException("身份证号已存在");
        }
        existing.setIdCard(patient.getIdCard());
        existing.setName(patient.getName());
        existing.setGender(patient.getGender());
        existing.setBirthDate(patient.getBirthDate());
        existing.setPhone(patient.getPhone());
        existing.setAddress(patient.getAddress());
        existing.setAllergyInfo(patient.getAllergyInfo());
        return patientRepository.save(existing);
    }

    public void delete(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new BusinessException("患者不存在");
        }
        patientRepository.deleteById(id);
    }
}
