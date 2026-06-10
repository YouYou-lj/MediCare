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

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 处方管理控制器
 * 处方开立：选择患者 → 添加药品明细 → 保存（事务级库存扣减）
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class PrescriptionController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(PrescriptionController.class);

    private final RegistrationService registrationService = new RegistrationService();
    private final MedicalRecordService medicalRecordService = new MedicalRecordService();
    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final MedicineService medicineService = new MedicineService();

    private final ObservableList<Registration> patientList = FXCollections.observableArrayList();
    private final ObservableList<Medicine> medicineSearchResult = FXCollections.observableArrayList();
    private final ObservableList<PrescriptionItem> itemList = FXCollections.observableArrayList();
    private final ObservableList<Prescription> prescriptionList = FXCollections.observableArrayList();

    private Registration selectedRegistration = null;
    private MedicalRecord selectedMedicalRecord = null;

    // ========== 左侧患者列表 ==========
    @FXML private TableView<Registration> tablePatients;
    @FXML private TableColumn<Registration, Integer> colPatSeq;
    @FXML private TableColumn<Registration, String> colPatName, colPatDoctor, colPatDept;

    // ========== 右侧处方编辑 ==========
    @FXML private Label lblPatientInfo, lblPrescriptionStatus, lblTotalAmount, lblPrescMessage;
    @FXML private TableView<PrescriptionItem> tableItems;
    @FXML private TableColumn<PrescriptionItem, String> colItemName, colItemSpec, colItemUnit, colItemUsage;
    @FXML private TableColumn<PrescriptionItem, Integer> colItemQty;
    @FXML private TableColumn<PrescriptionItem, String> colItemPrice, colItemAmount;
    @FXML private TextField txtMedSearch, txtUsage;
    @FXML private ComboBox<Medicine> cmbMedicine;
    @FXML private Spinner<Integer> spinQty;

    // ========== 今日处方列表 ==========
    @FXML private TableView<Prescription> tablePrescriptions;
    @FXML private TableColumn<Prescription, Long> colPrescId;
    @FXML private TableColumn<Prescription, String> colPrescPatient, colPrescDoctor, colPrescAmount, colPrescTime;
    @FXML private TableColumn<Prescription, Integer> colPrescStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("处方管理界面初始化");
        initPatientTable();
        initItemTable();
        initPrescriptionTable();
        spinQty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        cmbMedicine.setItems(medicineSearchResult);
        loadPatientData();
        loadPrescriptionData();
    }

    // ============================================================
    // 表格初始化
    // ============================================================

    private void initPatientTable() {
        colPatSeq.setCellValueFactory(new PropertyValueFactory<>("seqNo"));
        colPatName.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colPatDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colPatDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        tablePatients.setItems(patientList);
        tablePatients.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) onSelectPatient(n);
        });
    }

    private void initItemTable() {
        colItemName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colItemSpec.setCellValueFactory(new PropertyValueFactory<>("medicineSpec"));
        colItemQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colItemUnit.setCellValueFactory(new PropertyValueFactory<>("medicineUnit"));
        colItemPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colItemAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colItemUsage.setCellValueFactory(new PropertyValueFactory<>("usageDesc"));
        tableItems.setItems(itemList);
    }

    private void initPrescriptionTable() {
        colPrescId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPrescPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colPrescDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colPrescAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colPrescStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPrescStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); }
                else {
                    setText(Prescription.getStatusText(status));
                    switch (status) {
                        case 0 -> setStyle("-fx-text-fill: #f39c12;");
                        case 1 -> setStyle("-fx-text-fill: #3498db;");
                        case 2 -> setStyle("-fx-text-fill: #27ae60;");
                        case 3 -> setStyle("-fx-text-fill: #7f8c8d;");
                    }
                }
            }
        });
        colPrescTime.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        tablePrescriptions.setItems(prescriptionList);
    }

    // ============================================================
    // 数据加载
    // ============================================================

    private void loadPatientData() {
        Task<List<Registration>> task = new Task<>() {
            @Override protected List<Registration> call() throws Exception {
                return registrationService.listToday();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Registration> list = task.getValue();
            list.removeIf(r -> r.getStatus() != Registration.STATUS_COMPLETED);
            patientList.setAll(list);
        }));
        task.setOnFailed(e -> logger.error("加载患者失败", task.getException()));
        new Thread(task).start();
    }

    private void loadPrescriptionData() {
        Task<List<Prescription>> task = new Task<>() {
            @Override protected List<Prescription> call() throws Exception {
                return prescriptionService.listToday();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> prescriptionList.setAll(task.getValue())));
        task.setOnFailed(e -> logger.error("加载处方失败", task.getException()));
        new Thread(task).start();
    }

    // ============================================================
    // 选中患者
    // ============================================================

    private void onSelectPatient(Registration reg) {
        selectedRegistration = reg;
        lblPatientInfo.setText("患者: " + reg.getPatientName() + " | 医生: " + reg.getDoctorName() + " | 科室: " + reg.getDepartmentName());

        // 加载对应病历
        Task<MedicalRecord> task = new Task<>() {
            @Override protected MedicalRecord call() throws Exception {
                return medicalRecordService.getByRegistration(reg.getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            selectedMedicalRecord = task.getValue();
            if (selectedMedicalRecord == null) {
                lblPrescriptionStatus.setText("未找到病历，无法开立处方");
                lblPrescriptionStatus.setStyle("-fx-text-fill: #c0392b;");
            } else {
                lblPrescriptionStatus.setText("病历已确认，可以开立处方");
                lblPrescriptionStatus.setStyle("-fx-text-fill: #27ae60;");

                // 检查是否已有处方
                Task<Prescription> pTask = new Task<>() {
                    @Override protected Prescription call() throws Exception {
                        return prescriptionService.getByRecord(selectedMedicalRecord.getId());
                    }
                };
                pTask.setOnSucceeded(ev -> Platform.runLater(() -> {
                    Prescription p = pTask.getValue();
                    if (p != null) {
                        lblPrescriptionStatus.setText("该患者已有处方（" + Prescription.getStatusText(p.getStatus()) + "）");
                        lblPrescriptionStatus.setStyle("-fx-text-fill: #f39c12;");
                    }
                }));
                new Thread(pTask).start();
            }
        }));
        new Thread(task).start();

        itemList.clear();
        updateTotal();
    }

    // ============================================================
    // 药品搜索
    // ============================================================

    @FXML
    private void handleSearchMedicine() {
        String kw = txtMedSearch.getText().trim();
        if (kw.isEmpty()) return;
        Task<List<Medicine>> task = new Task<>() {
            @Override protected List<Medicine> call() throws Exception {
                return medicineService.search(kw);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            medicineSearchResult.setAll(task.getValue());
            if (!medicineSearchResult.isEmpty()) cmbMedicine.getSelectionModel().select(0);
        }));
        new Thread(task).start();
    }

    // ============================================================
    // 添加/移除明细
    // ============================================================

    @FXML
    private void handleAddItem() {
        Medicine med = cmbMedicine.getValue();
        if (med == null) { showError("请选择药品"); return; }
        int qty = spinQty.getValue();
        String usage = txtUsage.getText().trim();
        if (usage.isEmpty()) usage = "遵医嘱";

        // 检查是否已存在
        for (PrescriptionItem existing : itemList) {
            if (existing.getMedicineId().equals(med.getId())) {
                showError("该药品已添加，请先移除后重新添加"); return;
            }
        }

        PrescriptionItem item = new PrescriptionItem();
        item.setMedicineId(med.getId());
        item.setMedicineName(med.getName());
        item.setMedicineSpec(med.getSpec());
        item.setMedicineUnit(med.getUnit());
        item.setQuantity(qty);
        item.setUnitPrice(med.getPrice() != null ? med.getPrice() : BigDecimal.ZERO);
        item.setAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
        item.setUsageDesc(usage);

        itemList.add(item);
        updateTotal();
        txtUsage.clear();
        showInfo("已添加: " + med.getName());
    }

    @FXML
    private void handleRemoveItem() {
        PrescriptionItem item = tableItems.getSelectionModel().getSelectedItem();
        if (item == null) { showError("请先选择要移除的药品"); return; }
        itemList.remove(item);
        updateTotal();
    }

    private void updateTotal() {
        BigDecimal total = itemList.stream()
                .map(PrescriptionItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalAmount.setText("总金额: ￥" + total.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
    }

    // ============================================================
    // 保存处方（事务级库存扣减）
    // ============================================================

    @FXML
    private void handleSavePrescription() {
        if (selectedRegistration == null) { showError("请先选择患者"); return; }
        if (selectedMedicalRecord == null) { showError("该患者无病历记录，无法开立处方"); return; }
        if (itemList.isEmpty()) { showError("处方明细不能为空"); return; }

        Prescription prescription = new Prescription();
        prescription.setRecordId(selectedMedicalRecord.getId());
        prescription.setPatientId(selectedRegistration.getPatientId());
        prescription.setDoctorId(selectedRegistration.getScheduleId()); // 这里应该是 doctorId

        // 修正：从 selectedRegistration 获取不到 doctorId，因为 Registration 实体没有 doctorId 字段
        // 我需要通过 Schedule 查询医生ID，或者从 Registration 关联中获取
        // 当前 Registration 实体没有 doctorId，只有 scheduleId
        // 我可以通过 Registration 的 scheduleId 查询 Schedule 获取 doctorId

        // 暂时先通过 selectedRegistration 的 doctorName 无法直接获取 ID
        // 让我修改一下：在 DoctorWorkstation 中，Registration 已经有 doctorName 但没有 doctorId
        // 我需要在 RegistrationDAO 的 SQL 中加入 doctor_id

        // 实际上，Registration 实体目前只有 scheduleId 关联到 Schedule。Schedule 有 doctorId。
        // 我需要在 Registration 实体中添加 doctorId 字段，或者在 DAO 查询中加入它。

        // 为了简化，我先在 Registration 实体中添加 doctorId 字段，并修改 DAO 查询。
        // 但这会影响之前的代码...让我看看当前 Registration 实体。

        // Registration 实体有：patientId, scheduleId, regTime, status, seqNo, fee, patientName, doctorName, departmentName, timeSlot
        // 没有 doctorId。

        // 一个简单的方法：在 Controller 中，当选中患者时，通过 ScheduleService 获取 schedule 信息。
        // 或者，我直接在 Registration 实体中添加 doctorId。

        // 为了最小化改动，让我在 Controller 中通过 selectedRegistration 的 scheduleId 查询 Schedule 获取 doctorId。

        // 等等，Prescription 表需要 doctor_id。让我修改 handleSavePrescription 中的逻辑。

        // 先用一个 Task 来获取 doctorId
        Task<Long> getDoctorTask = new Task<>() {
            @Override protected Long call() throws Exception {
                ScheduleService ss = new ScheduleService();
                Schedule sch = ss.getById(selectedRegistration.getScheduleId());
                return sch != null ? sch.getDoctorId() : null;
            }
        };
        getDoctorTask.setOnSucceeded(e -> {
            Long doctorId = getDoctorTask.getValue();
            if (doctorId == null) {
                showError("无法获取医生信息"); return;
            }
            prescription.setDoctorId(doctorId);
            doSavePrescription(prescription);
        });
        new Thread(getDoctorTask).start();
    }

    private void doSavePrescription(Prescription prescription) {
        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception {
                return prescriptionService.createPrescription(prescription, new ArrayList<>(itemList));
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            showInfo("处方开立成功！处方号: " + task.getValue());
            itemList.clear();
            updateTotal();
            loadPrescriptionData();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> showError(task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleClear() {
        selectedRegistration = null;
        selectedMedicalRecord = null;
        itemList.clear();
        updateTotal();
        lblPatientInfo.setText("未选择患者");
        lblPrescriptionStatus.setText("");
        tablePatients.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleViewToday() {
        loadPrescriptionData();
    }

    @FXML
    private void handleRefresh() {
        loadPatientData();
        loadPrescriptionData();
    }

    private void showInfo(String msg) {
        lblPrescMessage.setStyle("-fx-text-fill: #27ae60;");
        lblPrescMessage.setText(msg);
    }

    private void showError(String msg) {
        lblPrescMessage.setStyle("-fx-text-fill: #c0392b;");
        lblPrescMessage.setText(msg);
    }
}
