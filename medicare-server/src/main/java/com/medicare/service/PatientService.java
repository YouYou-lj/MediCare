package com.medicare.service;

import com.medicare.entity.Patient;
import com.medicare.exception.BusinessException;
import com.medicare.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public Page<Patient> findAll(Pageable pageable) {
        return patientRepository.findAll(pageable);
    }

    public Patient findById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("患者不存在"));
    }

    public List<Patient> search(String keyword) {
        return patientRepository.search(keyword);
    }

    public Patient create(Patient patient) {
        if (patientRepository.existsByIdCard(patient.getIdCard())) {
            throw new BusinessException("身份证号已存在");
        }
        return patientRepository.save(patient);
    }

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
