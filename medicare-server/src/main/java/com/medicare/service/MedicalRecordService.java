package com.medicare.service;

import com.medicare.dto.MedicalRecordVO;
import com.medicare.entity.MedicalRecord;
import com.medicare.exception.BusinessException;
import com.medicare.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 病历服务 — 病历 CRUD，一个挂号只能对应一份病历
 */
@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    /** 病历列表查询（支持按患者ID或挂号ID筛选，返回含关联名称的 VO） */
    public List<MedicalRecordVO> findRecordVOList(Long patientId, Long registrationId) {
        return medicalRecordRepository.findRecordVOList(patientId, registrationId);
    }

    public MedicalRecordVO findRecordVOByRegistrationId(Long registrationId) {
        MedicalRecordVO vo = medicalRecordRepository.findRecordVOByRegistrationId(registrationId);
        if (vo == null) {
            throw new BusinessException("病历不存在");
        }
        return vo;
    }

    public MedicalRecordVO findRecordVOById(Long id) {
        MedicalRecordVO vo = medicalRecordRepository.findRecordVOById(id);
        if (vo == null) {
            throw new BusinessException("病历不存在");
        }
        return vo;
    }

    public MedicalRecord findById(Long id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("病历不存在"));
    }

    /** 创建病历 — 校验同一挂号不能重复创建 */
    public MedicalRecord create(MedicalRecord record) {
        if (medicalRecordRepository.findByRegistrationId(record.getRegistrationId()).isPresent()) {
            throw new BusinessException("该挂号已有病历记录");
        }
        return medicalRecordRepository.save(record);
    }

    /** 更新病历 — 仅更新医学字段（主诉/现病史/既往史/体格检查/诊断/医嘱） */
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
