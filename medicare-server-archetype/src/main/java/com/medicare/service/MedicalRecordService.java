package com.medicare.service;

import com.medicare.dto.MedicalRecordVO;
import com.medicare.entity.MedicalRecord;
import com.medicare.exception.BusinessException;
import com.medicare.repository.DoctorRepository;
import com.medicare.repository.MedicalRecordRepository;
import com.medicare.repository.PatientRepository;
import com.medicare.repository.RegistrationRepository;
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
    private final RegistrationRepository registrationRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public List<MedicalRecordVO> findMedicalRecordVOList(Long patientId, Long registrationId) {
        return null;
    }

    public MedicalRecordVO findMedicalRecordVOById(Long id) {
        return null;
    }

    /** 按挂号ID查询病历 */
    public MedicalRecordVO findByRegistrationId(Long registrationId) {
        return null;
    }

    /** 创建病历 — 同一挂号不允许重复创建 */
    public MedicalRecord create(MedicalRecord record) {
        return null;
    }

    /** 更新病历 — 仅更新主诉、现病史、既往史、体格检查、诊断、医嘱字段 */
    public MedicalRecord update(Long id, MedicalRecord record) {
        return null;
    }
}
