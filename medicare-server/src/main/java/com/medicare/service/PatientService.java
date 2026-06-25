package com.medicare.service;

import com.medicare.entity.Patient;
import com.medicare.entity.Prescription;
import com.medicare.entity.PrescriptionItem;
import com.medicare.entity.Registration;
import com.medicare.entity.InventoryLog;
import com.medicare.exception.BusinessException;
import com.medicare.repository.MedicalRecordRepository;
import com.medicare.repository.PatientRepository;
import com.medicare.util.CodeUtils;
import com.medicare.repository.PrescriptionItemRepository;
import com.medicare.repository.PrescriptionRepository;
import com.medicare.repository.RegistrationRepository;
import com.medicare.repository.MedicineRepository;
import com.medicare.repository.InventoryLogRepository;
import com.medicare.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 患者服务 — 患者 CRUD + 搜索，身份证号唯一性校验
 */
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final RegistrationRepository registrationRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ScheduleRepository scheduleRepository;

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
    @Transactional
    public Patient create(Patient patient) {
        if (patientRepository.existsByIdCard(patient.getIdCard())) {
            throw new BusinessException("身份证号已存在");
        }
        patient = patientRepository.save(patient);
        patient.setCode(CodeUtils.generateCode("PAT", patient.getId()));
        return patientRepository.save(patient);
    }

    /** 更新患者 — 身份证号唯一性校验（排除自身） */
    @Transactional
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

    @Transactional
    public void delete(Long id) {
        Patient existing = findById(id);
        long registrationCount = registrationRepository.countByPatientId(id);
        long medicalRecordCount = medicalRecordRepository.countByPatientId(id);
        long prescriptionCount = prescriptionRepository.countByPatientId(id);
        if (registrationCount > 0 || medicalRecordCount > 0 || prescriptionCount > 0) {
            throw new BusinessException(409, "该患者已有挂号/病历/处方记录，不能直接删除。请先处理关联业务数据，或保留患者档案。");
        }
        patientRepository.delete(existing);
    }

    /**
     * 管理员显式确认后使用：按外键依赖顺序清理患者关联业务数据，再删除患者档案。
     * 未取药处方在删除前会回滚库存，已取药处方不回滚库存。
     */
    @Transactional
    public void deleteWithRelated(Long id) {
        Patient existing = findById(id);
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByCreateTimeDesc(id);
        for (Prescription prescription : prescriptions) {
            cleanupPrescription(prescription);
        }

        medicalRecordRepository.deleteAll(medicalRecordRepository.findByPatientIdOrderByCreateTimeDesc(id));

        List<Registration> registrations = registrationRepository.findByPatientIdOrderByCreateTimeDesc(id);
        for (Registration registration : registrations) {
            if (registration.getStatus() != null
                    && registration.getStatus() != Registration.STATUS_CANCELLED
                    && registration.getStatus() != Registration.STATUS_COMPLETED) {
                scheduleRepository.incrementRemain(registration.getScheduleId());
            }
        }
        registrationRepository.deleteAll(registrations);
        patientRepository.delete(existing);
    }

    private void cleanupPrescription(Prescription prescription) {
        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionId(prescription.getId());
        if (prescription.getStatus() != null
                && prescription.getStatus() != Prescription.STATUS_CANCELLED
                && prescription.getStatus() != Prescription.STATUS_DISPENSED) {
            for (PrescriptionItem item : items) {
                medicineRepository.incrementStock(item.getMedicineId(), item.getQuantity());
                InventoryLog log = new InventoryLog();
                log.setMedicineId(item.getMedicineId());
                log.setType(InventoryLog.TYPE_STOCK_IN);
                log.setQuantity(item.getQuantity());
                log.setOperator("system");
                log.setRemark("删除患者关联处方回滚 - 处方ID:" + prescription.getId());
                log = inventoryLogRepository.save(log);
                log.setCode(CodeUtils.generateCode("INV", log.getId()));
                inventoryLogRepository.save(log);
            }
        }
        prescriptionItemRepository.deleteAll(items);
        prescriptionRepository.delete(prescription);
    }
}
