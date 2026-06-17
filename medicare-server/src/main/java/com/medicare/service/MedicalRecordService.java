package com.medicare.service;

import com.medicare.dto.MedicalRecordVO;
import com.medicare.entity.MedicalRecord;
import com.medicare.exception.BusinessException;
import com.medicare.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    public List<MedicalRecordVO> findRecordVOList(Long patientId, Long doctorId) {
        return medicalRecordRepository.findRecordVOList(patientId, doctorId);
    }

    public MedicalRecordVO findRecordVOByRegistrationId(Long registrationId) {
        MedicalRecordVO vo = medicalRecordRepository.findRecordVOByRegistrationId(registrationId);
        if (vo == null) {
            throw new BusinessException("病历不存在");
        }
        return vo;
    }

    public MedicalRecord findById(Long id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("病历不存在"));
    }

    public MedicalRecord create(MedicalRecord record) {
        if (medicalRecordRepository.findByRegistrationId(record.getRegistrationId()).isPresent()) {
            throw new BusinessException("该挂号已有病历记录");
        }
        return medicalRecordRepository.save(record);
    }

    public MedicalRecord update(Long id, MedicalRecord record) {
        MedicalRecord existing = findById(id);
        existing.setChiefComplaint(record.getChiefComplaint());
        existing.setPresentIllness(record.getPresentIllness());
        existing.setPastHistory(record.getPastHistory());
        existing.setPhysicalExam(record.getPhysicalExam());
        existing.setDiagnosis(record.getDiagnosis());
        existing.setAdvice(record.getAdvice());
        return medicalRecordRepository.save(existing);
    }
}
