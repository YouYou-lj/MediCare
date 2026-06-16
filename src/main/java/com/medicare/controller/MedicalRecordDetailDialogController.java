package com.medicare.controller;

import com.medicare.model.MedicalRecord;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.time.format.DateTimeFormatter;

/**
 * 病历详情弹窗控制器（只读）
 */
public class MedicalRecordDetailDialogController {

    @FXML private Label lblPatient;
    @FXML private Label lblDoctor;
    @FXML private Label lblChief;
    @FXML private TextArea txtPresent;
    @FXML private TextArea txtPast;
    @FXML private TextArea txtPhysical;
    @FXML private Label lblDiagnosis;
    @FXML private TextArea txtAdvice;
    @FXML private Label lblCreateTime;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void setRecord(MedicalRecord record) {
        if (record == null) return;
        lblPatient.setText(record.getPatientName() != null ? record.getPatientName() : "-");
        lblDoctor.setText(record.getDoctorName() != null ? record.getDoctorName() : "-");
        lblChief.setText(record.getChiefComplaint() != null ? record.getChiefComplaint() : "-");
        txtPresent.setText(record.getPresentIllness() != null ? record.getPresentIllness() : "-");
        txtPast.setText(record.getPastHistory() != null ? record.getPastHistory() : "-");
        txtPhysical.setText(record.getPhysicalExam() != null ? record.getPhysicalExam() : "-");
        lblDiagnosis.setText(record.getDiagnosis() != null ? record.getDiagnosis() : "-");
        txtAdvice.setText(record.getAdvice() != null ? record.getAdvice() : "-");
        lblCreateTime.setText(record.getCreateTime() != null ? record.getCreateTime().format(fmt) : "-");
    }
}
