package com.medicare.controller;

import com.medicare.model.Patient;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;

/**
 * 患者新增/编辑弹窗控制器
 * 供患者管理模块和挂号预约模块复用
 */
public class PatientDialogController {

    @FXML private TextField txtIdCard;
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cmbGender;
    @FXML private DatePicker dpBirthDate;
    @FXML private TextField txtPhone;
    @FXML private TextField txtAddress;
    @FXML private TextField txtAllergy;
    @FXML private Label lblMessage;

    private Patient editingPatient = null;

    @FXML
    public void initialize() {
        cmbGender.setItems(FXCollections.observableArrayList("女性", "男性"));
        cmbGender.getSelectionModel().select(1); // 默认男性
    }

    /**
     * 预填充数据（编辑模式）
     */
    public void setPatient(Patient patient) {
        this.editingPatient = patient;
        if (patient != null) {
            txtIdCard.setText(patient.getIdCard());
            txtName.setText(patient.getName());
            cmbGender.getSelectionModel().select(patient.getGender() != null ? patient.getGender() : 1); // 默认男性
            dpBirthDate.setValue(patient.getBirthDate());
            txtPhone.setText(patient.getPhone());
            txtAddress.setText(patient.getAddress());
            txtAllergy.setText(patient.getAllergyInfo());
        }
    }

    /**
     * 获取表单数据，校验失败返回 null
     */
    public Patient getPatient() {
        String idCard = txtIdCard.getText().trim();
        String name = txtName.getText().trim();

        if (idCard.isEmpty() || name.isEmpty()) {
            showError("身份证号和姓名不能为空");
            return null;
        }

        Patient patient = new Patient();
        if (editingPatient != null) {
            patient.setId(editingPatient.getId());
        }
        patient.setIdCard(idCard);
        patient.setName(name);
        patient.setGender(cmbGender.getSelectionModel().getSelectedIndex());
        patient.setBirthDate(dpBirthDate.getValue());
        patient.setPhone(txtPhone.getText().trim());
        patient.setAddress(txtAddress.getText().trim());
        patient.setAllergyInfo(txtAllergy.getText().trim());
        return patient;
    }

    private void showError(String msg) {
        lblMessage.setStyle("-fx-text-fill: #c0392b;");
        lblMessage.setText(msg);
    }
}
