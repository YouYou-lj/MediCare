package com.medicare.service;

import com.medicare.entity.*;
import com.medicare.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LiveBusinessContextService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PatientRepository patientRepository;
    private final RegistrationRepository registrationRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final DoctorRepository doctorRepository;

    public String buildSnapshot() {
        StringBuilder snapshot = new StringBuilder();
        snapshot.append("【实时业务数据快照】\n");
        appendRecentPatients(snapshot);
        appendTodayRegistrations(snapshot);
        appendRecentRecords(snapshot);
        appendRecentPrescriptions(snapshot);
        appendInventory(snapshot);
        return snapshot.toString().trim();
    }

    private void appendRecentPatients(StringBuilder snapshot) {
        List<Patient> patients = patientRepository.findAll(PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"))).getContent();
        snapshot.append("- 最新患者：");
        if (patients.isEmpty()) {
            snapshot.append("暂无患者。\n");
            return;
        }
        snapshot.append(patients.stream()
                .map(patient -> "%s(ID:%s, 电话:%s, 过敏:%s)".formatted(
                        safe(patient.getName()),
                        patient.getId(),
                        safe(patient.getPhone()),
                        safe(patient.getAllergyInfo())))
                .reduce((a, b) -> a + "；" + b)
                .orElse("暂无患者")).append("\n");
    }

    private void appendTodayRegistrations(StringBuilder snapshot) {
        var registrations = registrationRepository.findTodayList(LocalDate.now(), null);
        snapshot.append("- 今日挂号：共 ").append(registrations.size()).append(" 条。");
        if (!registrations.isEmpty()) {
            snapshot.append(" 最近：");
            snapshot.append(registrations.stream().limit(6)
                    .map(item -> "%s/%s/%s/%s".formatted(
                            safe(item.getPatientName()),
                            safe(item.getDoctorName()),
                            safe(item.getDepartmentName()),
                            Registration.getStatusText(item.getStatus())))
                    .reduce((a, b) -> a + "；" + b)
                    .orElse(""));
        }
        snapshot.append("\n");
    }

    private void appendRecentRecords(StringBuilder snapshot) {
        List<MedicalRecord> records = medicalRecordRepository.findAll(PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "id"))).getContent();
        snapshot.append("- 最新病历：");
        if (records.isEmpty()) {
            snapshot.append("暂无病历。\n");
            return;
        }
        snapshot.append(records.stream()
                .map(record -> "患者ID:%s, 诊断:%s, 建议:%s".formatted(
                        record.getPatientId(),
                        safe(record.getDiagnosis()),
                        safe(record.getAdvice())))
                .reduce((a, b) -> a + "；" + b)
                .orElse("暂无病历")).append("\n");
    }

    private void appendRecentPrescriptions(StringBuilder snapshot) {
        List<Prescription> prescriptions = prescriptionRepository.findAll(PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "id"))).getContent();
        snapshot.append("- 最新处方：");
        if (prescriptions.isEmpty()) {
            snapshot.append("暂无处方。\n");
            return;
        }
        snapshot.append(prescriptions.stream()
                .map(prescription -> "处方ID:%s, 患者ID:%s, 金额:%s, 状态:%s".formatted(
                        prescription.getId(),
                        prescription.getPatientId(),
                        prescription.getTotalAmount(),
                        prescriptionStatusText(prescription.getStatus())))
                .reduce((a, b) -> a + "；" + b)
                .orElse("暂无处方")).append("\n");
    }

    private void appendInventory(StringBuilder snapshot) {
        List<Medicine> lowStock = medicineRepository.findLowStockMedicines().stream().limit(8).toList();
        long activeDoctors = doctorRepository.findByStatus(1).size();
        snapshot.append("- 当前医生：启用 ").append(activeDoctors).append(" 人。\n");
        snapshot.append("- 低库存药品：");
        if (lowStock.isEmpty()) {
            snapshot.append("暂无低库存预警。\n");
            return;
        }
        snapshot.append(lowStock.stream()
                .filter(Objects::nonNull)
                .map(medicine -> "%s 库存:%s 安全线:%s".formatted(
                        safe(medicine.getName()),
                        medicine.getStock(),
                        medicine.getSafetyStock()))
                .reduce((a, b) -> a + "；" + b)
                .orElse("暂无低库存预警")).append("\n");
    }

    private String prescriptionStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case Prescription.STATUS_PENDING -> "待缴费";
            case Prescription.STATUS_PAID -> "已缴费";
            case Prescription.STATUS_DISPENSED -> "已取药";
            case Prescription.STATUS_CANCELLED -> "已作废";
            default -> "未知";
        };
    }

    private String safe(Object value) {
        if (value == null) return "未填写";
        String text = value.toString().trim();
        return text.isBlank() ? "未填写" : text.replaceAll("[\\r\\n]+", " ");
    }
}
