package com.medicare.dao;

import com.medicare.model.MedicalRecord;

import java.sql.SQLException;
import java.util.List;

/**
 * 病历数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class MedicalRecordDAO extends BaseDAO<MedicalRecord> {

    private static final String SQL_INSERT =
            "INSERT INTO medical_record (registration_id, patient_id, doctor_id, chief_complaint, " +
            "present_illness, past_history, physical_exam, diagnosis, advice) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE medical_record SET chief_complaint = ?, present_illness = ?, past_history = ?, " +
            "physical_exam = ?, diagnosis = ?, advice = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM medical_record WHERE id = ?";

    private static final String SQL_SELECT_BY_ID =
            "SELECT id, registration_id, patient_id, doctor_id, chief_complaint, present_illness, " +
            "past_history, physical_exam, diagnosis, advice, create_time, update_time " +
            "FROM medical_record WHERE id = ?";

    private static final String SQL_SELECT_BY_PATIENT =
            "SELECT id, registration_id, patient_id, doctor_id, chief_complaint, present_illness, " +
            "past_history, physical_exam, diagnosis, advice, create_time, update_time " +
            "FROM medical_record WHERE patient_id = ? ORDER BY create_time DESC";

    private static final String SQL_SELECT_BY_REGISTRATION =
            "SELECT id, registration_id, patient_id, doctor_id, chief_complaint, present_illness, " +
            "past_history, physical_exam, diagnosis, advice, create_time, update_time " +
            "FROM medical_record WHERE registration_id = ?";

    private static final String SQL_SELECT_BY_DOCTOR_TODAY =
            "SELECT mr.id, mr.registration_id, mr.patient_id, mr.doctor_id, mr.chief_complaint, " +
            "mr.present_illness, mr.past_history, mr.physical_exam, mr.diagnosis, mr.advice, " +
            "mr.create_time, mr.update_time, p.name AS patientName " +
            "FROM medical_record mr " +
            "LEFT JOIN patient p ON mr.patient_id = p.id " +
            "WHERE mr.doctor_id = ? AND DATE(mr.create_time) = CURDATE() " +
            "ORDER BY mr.create_time DESC";

    public Long insert(MedicalRecord record) throws SQLException {
        return executeInsert(SQL_INSERT,
                record.getRegistrationId(), record.getPatientId(), record.getDoctorId(),
                record.getChiefComplaint(), record.getPresentIllness(), record.getPastHistory(),
                record.getPhysicalExam(), record.getDiagnosis(), record.getAdvice());
    }

    public int update(MedicalRecord record) throws SQLException {
        return executeUpdate(SQL_UPDATE,
                record.getChiefComplaint(), record.getPresentIllness(), record.getPastHistory(),
                record.getPhysicalExam(), record.getDiagnosis(), record.getAdvice(), record.getId());
    }

    public int delete(Long id) throws SQLException {
        return executeUpdate(SQL_DELETE, id);
    }

    public MedicalRecord findById(Long id) throws SQLException {
        return querySingle(SQL_SELECT_BY_ID, id);
    }

    public List<MedicalRecord> findByPatient(Long patientId) throws SQLException {
        return queryList(SQL_SELECT_BY_PATIENT, patientId);
    }

    public MedicalRecord findByRegistration(Long registrationId) throws SQLException {
        return querySingle(SQL_SELECT_BY_REGISTRATION, registrationId);
    }

    public List<MedicalRecord> findByDoctorToday(Long doctorId) throws SQLException {
        return queryList(SQL_SELECT_BY_DOCTOR_TODAY, doctorId);
    }
}
