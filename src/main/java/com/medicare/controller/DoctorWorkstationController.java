package com.medicare.controller;

import com.medicare.model.*;
import com.medicare.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 医生工作站控制器
 * 叫号、病历书写、历史病历查看
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class DoctorWorkstationController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(DoctorWorkstationController.class);

    private final DoctorService doctorService = new DoctorService();
    private final RegistrationService registrationService = new RegistrationService();
    private final MedicalRecordService medicalRecordService = new MedicalRecordService();
    private final PatientService patientService = new PatientService();

    private final ObservableList<Doctor> doctorOptions = FXCollections.observableArrayList();
    private final ObservableList<Registration> queueList = FXCollections.observableArrayList();
    private final ObservableList<MedicalRecord> historyList = FXCollections.observableArrayList();

    private Registration selectedRegistration = null;
    private Doctor selectedDoctor = null;

    // ========== FXML 注入 ==========
    @FXML private ComboBox<Doctor> cmbDoctor;
    @FXML private TableView<Registration> tableQueue;
    @FXML private TableColumn<Registration, Integer> colQueueSeq;
    @FXML private TableColumn<Registration, String> colQueuePatient, colQueueDept, colQueueSlot;
    @FXML private TableColumn<Registration, Integer> colQueueStatus;
    @FXML private Label lblPatientInfo, lblRegInfo, lblRecordMessage;
    @FXML private TextField txtChiefComplaint, txtDiagnosis;
    @FXML private TextArea txtPresentIllness, txtPastHistory, txtPhysicalExam, txtAdvice;
    @FXML private TableView<MedicalRecord> tableHistory;
    @FXML private TableColumn<MedicalRecord, LocalDateTime> colHistDate;
    @FXML private TableColumn<MedicalRecord, String> colHistDoctor, colHistDiagnosis;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("医生工作站界面初始化");
        initQueueTable();
        initHistoryTable();
        loadDoctorOptions();
    }

    // ============================================================
    // 表格初始化
    // ============================================================

    private void initQueueTable() {
        colQueueSeq.setCellValueFactory(new PropertyValueFactory<>("seqNo"));
        colQueuePatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colQueueDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colQueueSlot.setCellValueFactory(new PropertyValueFactory<>("timeSlot"));
        colQueueStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colQueueStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); }
                else {
                    setText(Registration.getStatusText(status));
                    switch (status) {
                        case 0 -> setStyle("-fx-text-fill: #f39c12;");
                        case 1 -> setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                        case 2 -> setStyle("-fx-text-fill: #27ae60;");
                        default -> setStyle("");
                    }
                }
            }
        });
        tableQueue.setItems(queueList);
        tableQueue.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) onSelectRegistration(n);
        });
    }

    private void initHistoryTable() {
        colHistDate.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        colHistDate.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText(empty || dt == null ? null : dt.format(fmt));
            }
        });
        colHistDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colHistDiagnosis.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));
        tableHistory.setItems(historyList);
    }

    // ============================================================
    // 数据加载
    // ============================================================

    private void loadDoctorOptions() {
        Task<List<Doctor>> task = new Task<>() {
            @Override protected List<Doctor> call() throws Exception { return doctorService.listAll(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            doctorOptions.setAll(task.getValue());
            cmbDoctor.setItems(doctorOptions);
            cmbDoctor.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                selectedDoctor = n;
                if (n != null) loadQueue();
            });
        }));
        task.setOnFailed(e -> logger.error("加载医生失败", task.getException()));
        new Thread(task).start();
    }

    private void loadQueue() {
        if (selectedDoctor == null) return;
        Task<List<Registration>> task = new Task<>() {
            @Override protected List<Registration> call() throws Exception {
                return registrationService.listWaitingByDoctor(selectedDoctor.getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> queueList.setAll(task.getValue())));
        task.setOnFailed(e -> logger.error("加载候诊列表失败", task.getException()));
        new Thread(task).start();
    }

    private void loadHistory(Long patientId) {
        if (patientId == null) { historyList.clear(); return; }
        Task<List<MedicalRecord>> task = new Task<>() {
            @Override protected List<MedicalRecord> call() throws Exception {
                return medicalRecordService.listByPatient(patientId);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> historyList.setAll(task.getValue())));
        task.setOnFailed(e -> logger.error("加载历史病历失败", task.getException()));
        new Thread(task).start();
    }

    // ============================================================
    // 选中患者
    // ============================================================

    private void onSelectRegistration(Registration reg) {
        selectedRegistration = reg;
        lblPatientInfo.setText("患者: " + reg.getPatientName() + " | 科室: " + reg.getDepartmentName() + " | 时段: " + reg.getTimeSlot());
        lblRegInfo.setText("挂号序号: " + reg.getSeqNo() + " | 状态: " + Registration.getStatusText(reg.getStatus()));

        // 加载已有病历（如果有）
        Task<MedicalRecord> task = new Task<>() {
            @Override protected MedicalRecord call() throws Exception {
                return medicalRecordService.getByRegistration(reg.getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            MedicalRecord record = task.getValue();
            if (record != null) {
                loadRecordToForm(record);
                showInfo("已加载历史病历");
            } else {
                clearForm();
                showInfo("新患者，请填写病历");
            }
        }));
        new Thread(task).start();

        // 加载历史病历列表
        loadHistory(reg.getPatientId());
    }

    // ============================================================
    // 叫号
    // ============================================================

    @FXML
    private void handleCall() {
        Registration reg = tableQueue.getSelectionModel().getSelectedItem();
        if (reg == null) { showError("请先选择患者"); return; }
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                registrationService.callPatient(reg.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            loadQueue();
            showInfo("叫号成功: " + reg.getPatientName());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> showError(task.getException().getMessage())));
        new Thread(task).start();
    }

    // ============================================================
    // 保存病历
    // ============================================================

    @FXML
    private void handleSaveRecord() {
        if (selectedRegistration == null) { showError("请先选择患者"); return; }
        if (selectedDoctor == null) { showError("请先选择医生"); return; }

        String chief = txtChiefComplaint.getText().trim();
        String diagnosis = txtDiagnosis.getText().trim();
        if (chief.isEmpty() || diagnosis.isEmpty()) {
            showError("主诉和诊断不能为空"); return;
        }

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(selectedRegistration.getId());
        record.setPatientId(selectedRegistration.getPatientId());
        record.setDoctorId(selectedDoctor.getId());
        record.setChiefComplaint(chief);
        record.setPresentIllness(txtPresentIllness.getText().trim());
        record.setPastHistory(txtPastHistory.getText().trim());
        record.setPhysicalExam(txtPhysicalExam.getText().trim());
        record.setDiagnosis(diagnosis);
        record.setAdvice(txtAdvice.getText().trim());

        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception {
                return medicalRecordService.saveRecord(record);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            showInfo("病历保存成功");
            loadHistory(selectedRegistration.getPatientId());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> showError(task.getException().getMessage())));
        new Thread(task).start();
    }

    // ============================================================
    // 完成就诊
    // ============================================================

    @FXML
    private void handleComplete() {
        if (selectedRegistration == null) { showError("请先选择患者"); return; }
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                registrationService.completeRegistration(selectedRegistration.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            loadQueue();
            showInfo("就诊完成");
        }));
        task.setOnFailed(e -> Platform.runLater(() -> showError(task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleRefresh() {
        loadQueue();
    }

    // ============================================================
    // 表单辅助
    // ============================================================

    private void loadRecordToForm(MedicalRecord record) {
        txtChiefComplaint.setText(record.getChiefComplaint());
        txtPresentIllness.setText(record.getPresentIllness());
        txtPastHistory.setText(record.getPastHistory());
        txtPhysicalExam.setText(record.getPhysicalExam());
        txtDiagnosis.setText(record.getDiagnosis());
        txtAdvice.setText(record.getAdvice());
    }

    private void clearForm() {
        txtChiefComplaint.clear();
        txtPresentIllness.clear();
        txtPastHistory.clear();
        txtPhysicalExam.clear();
        txtDiagnosis.clear();
        txtAdvice.clear();
    }

    private void showInfo(String msg) {
        lblRecordMessage.setStyle("-fx-text-fill: #27ae60;");
        lblRecordMessage.setText(msg);
    }

    private void showError(String msg) {
        lblRecordMessage.setStyle("-fx-text-fill: #c0392b;");
        lblRecordMessage.setText(msg);
    }
}
