package com.medicare.service;

import com.medicare.dao.MedicalRecordDAO;
import com.medicare.model.MedicalRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * 病历管理服务
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class MedicalRecordService {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordService.class);

    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();

    public Long saveRecord(MedicalRecord record) throws SQLException, IllegalArgumentException {
        validate(record);
        MedicalRecord existing = medicalRecordDAO.findByRegistration(record.getRegistrationId());
        if (existing != null) {
            record.setId(existing.getId());
            medicalRecordDAO.update(record);
            logger.info("更新病历: id={}, patientId={}", record.getId(), record.getPatientId());
            return existing.getId();
        } else {
            Long id = medicalRecordDAO.insert(record);
            logger.info("新建病历: id={}, patientId={}", id, record.getPatientId());
            return id;
        }
    }

    public void updateRecord(MedicalRecord record) throws SQLException, IllegalArgumentException {
        if (record.getId() == null) throw new IllegalArgumentException("病历 ID 不能为空");
        validate(record);
        int rows = medicalRecordDAO.update(record);
        if (rows == 0) throw new IllegalArgumentException("病历不存在");
        logger.info("更新病历: id={}", record.getId());
    }

    public void deleteRecord(Long id) throws SQLException, IllegalArgumentException {
        int rows = medicalRecordDAO.delete(id);
        if (rows == 0) throw new IllegalArgumentException("病历不存在");
        logger.info("删除病历: id={}", id);
    }

    public MedicalRecord getById(Long id) throws SQLException {
        return medicalRecordDAO.findById(id);
    }

    public MedicalRecord getByRegistration(Long registrationId) throws SQLException {
        return medicalRecordDAO.findByRegistration(registrationId);
    }

    public List<MedicalRecord> listByPatient(Long patientId) throws SQLException {
        return medicalRecordDAO.findByPatient(patientId);
    }

    public List<MedicalRecord> listByDoctorToday(Long doctorId) throws SQLException {
        return medicalRecordDAO.findByDoctorToday(doctorId);
    }

    private void validate(MedicalRecord record) {
        if (record == null) throw new IllegalArgumentException("病历信息不能为空");
        if (record.getPatientId() == null) throw new IllegalArgumentException("患者不能为空");
        if (record.getDoctorId() == null) throw new IllegalArgumentException("医生不能为空");
        if (record.getRegistrationId() == null) throw new IllegalArgumentException("挂号记录不能为空");
    }
}
