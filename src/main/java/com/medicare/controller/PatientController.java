package com.medicare.controller;

import com.medicare.model.Patient;
import com.medicare.service.PatientService;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * 患者管理控制器
 * 界面渲染、事件响应、表单校验
 * 耗时操作在后台线程执行，禁止在 UI 线程执行 SQL
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class PatientController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private final PatientService patientService = new PatientService();
    private final ObservableList<Patient> patientList = FXCollections.observableArrayList();

    // 当前编辑的患者（null 表示新建）
    private Patient editingPatient = null;

    // ========== FXML 注入 ==========

    @FXML
    private TextField txtSearch;
    @FXML
    private TableView<Patient> tablePatients;
    @FXML
    private TableColumn<Patient, Long> colId;
    @FXML
    private TableColumn<Patient, String> colIdCard;
    @FXML
    private TableColumn<Patient, String> colName;
    @FXML
    private TableColumn<Patient, Integer> colGender;
    @FXML
    private TableColumn<Patient, LocalDate> colBirthDate;
    @FXML
    private TableColumn<Patient, String> colPhone;
    @FXML
    private TableColumn<Patient, String> colAddress;
    @FXML
    private TableColumn<Patient, String> colCreateTime;
    @FXML
    private Label lblRecordCount;
    @FXML
    private TextField txtIdCard;
    @FXML
    private TextField txtName;
    @FXML
    private ComboBox<String> cmbGender;
    @FXML
    private DatePicker dpBirthDate;
    @FXML
    private TextField txtPhone;
    @FXML
    private TextField txtAddress;
    @FXML
    private TextField txtAllergy;
    @FXML
    private Label lblMessage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("患者管理界面初始化");

        // 初始化性别下拉框
        cmbGender.setItems(FXCollections.observableArrayList("女", "男", "其他"));
        cmbGender.getSelectionModel().select(0);

        // 初始化表格列
        initTableColumns();

        // 表格选中事件
        tablePatients.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        loadPatientToForm(newVal);
                    }
                }
        );

        // 加载数据
        loadData();
    }

    // ============================================================
    // 表格初始化
    // ============================================================

    private void initTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colIdCard.setCellValueFactory(new PropertyValueFactory<>("idCard"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colGender.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer gender, boolean empty) {
                super.updateItem(gender, empty);
                if (empty || gender == null) {
                    setText(null);
                } else {
                    setText(gender == 0 ? "女" : gender == 1 ? "男" : "其他");
                }
            }
        });
        colBirthDate.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colCreateTime.setCellValueFactory(new PropertyValueFactory<>("createTime"));

        tablePatients.setItems(patientList);
    }

    // ============================================================
    // 数据加载（后台线程）
    // ============================================================

    private void loadData() {
        Task<java.util.List<Patient>> task = new Task<>() {
            @Override
            protected java.util.List<Patient> call() throws Exception {
                return patientService.listAll();
            }
        };

        task.setOnSucceeded(e -> {
            patientList.clear();
            patientList.addAll(task.getValue());
            lblRecordCount.setText("共 " + patientList.size() + " 条记录");
            showInfo("数据加载完成");
        });

        task.setOnFailed(e -> {
            logger.error("加载患者数据失败", task.getException());
            showError("加载数据失败: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    // ============================================================
    // 查询
    // ============================================================

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText();

        Task<java.util.List<Patient>> task = new Task<>() {
            @Override
            protected java.util.List<Patient> call() throws Exception {
                return patientService.search(keyword);
            }
        };

        task.setOnSucceeded(e -> {
            patientList.clear();
            patientList.addAll(task.getValue());
            lblRecordCount.setText("共 " + patientList.size() + " 条记录");
            showInfo("查询完成，找到 " + patientList.size() + " 条记录");
        });

        task.setOnFailed(e -> {
            logger.error("查询患者失败", task.getException());
            showError("查询失败: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    // ============================================================
    // 新建
    // ============================================================

    @FXML
    private void handleNew() {
        editingPatient = null;
        clearForm();
        showInfo("请填写新患者信息");
    }

    // ============================================================
    // 保存（新增或更新）
    // ============================================================

    @FXML
    private void handleSave() {
        Patient patient = buildPatientFromForm();
        if (patient == null) {
            return;
        }

        Task<Void> task;
        if (editingPatient == null) {
            // 新增
            task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    Long id = patientService.registerPatient(patient);
                    patient.setId(id);
                    return null;
                }
            };
        } else {
            // 更新
            patient.setId(editingPatient.getId());
            task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    patientService.updatePatient(patient);
                    return null;
                }
            };
        }

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                if (editingPatient == null) {
                    patientList.add(0, patient);
                    showInfo("患者建档成功");
                    logger.info("新建患者: id={}, name={}", patient.getId(), patient.getName());
                } else {
                    int idx = patientList.indexOf(editingPatient);
                    if (idx >= 0) {
                        patientList.set(idx, patient);
                    }
                    editingPatient = patient;
                    showInfo("档案更新成功");
                    logger.info("更新患者: id={}, name={}", patient.getId(), patient.getName());
                }
                lblRecordCount.setText("共 " + patientList.size() + " 条记录");
            });
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.error("保存患者失败", ex);
            Platform.runLater(() -> showError("保存失败: " + ex.getMessage()));
        });

        new Thread(task).start();
    }

    // ============================================================
    // 删除
    // ============================================================

    @FXML
    private void handleDelete() {
        if (editingPatient == null || editingPatient.getId() == null) {
            showError("请先选择要删除的患者");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定删除患者 " + editingPatient.getName() + " 的档案吗？此操作不可恢复！");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                doDelete(editingPatient.getId());
            }
        });
    }

    private void doDelete(Long id) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                patientService.deletePatient(id);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                patientList.remove(editingPatient);
                clearForm();
                lblRecordCount.setText("共 " + patientList.size() + " 条记录");
                showInfo("删除成功");
                logger.info("删除患者: id={}", id);
            });
        });

        task.setOnFailed(e -> {
            logger.error("删除患者失败", task.getException());
            Platform.runLater(() -> showError("删除失败: " + task.getException().getMessage()));
        });

        new Thread(task).start();
    }

    // ============================================================
    // 刷新 / 清除
    // ============================================================

    @FXML
    private void handleRefresh() {
        txtSearch.clear();
        loadData();
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    // ============================================================
    // 表单辅助方法
    // ============================================================

    private void loadPatientToForm(Patient patient) {
        editingPatient = patient;
        txtIdCard.setText(patient.getIdCard());
        txtName.setText(patient.getName());
        cmbGender.getSelectionModel().select(patient.getGender() != null ? patient.getGender() : 0);
        dpBirthDate.setValue(patient.getBirthDate());
        txtPhone.setText(patient.getPhone());
        txtAddress.setText(patient.getAddress());
        txtAllergy.setText(patient.getAllergyInfo());
        lblMessage.setText("");
    }

    private Patient buildPatientFromForm() {
        String idCard = txtIdCard.getText().trim();
        String name = txtName.getText().trim();

        if (idCard.isEmpty() || name.isEmpty()) {
            showError("身份证号和姓名不能为空");
            return null;
        }

        Patient patient = new Patient();
        patient.setIdCard(idCard);
        patient.setName(name);
        patient.setGender(cmbGender.getSelectionModel().getSelectedIndex());
        patient.setBirthDate(dpBirthDate.getValue());
        patient.setPhone(txtPhone.getText().trim());
        patient.setAddress(txtAddress.getText().trim());
        patient.setAllergyInfo(txtAllergy.getText().trim());

        return patient;
    }

    private void clearForm() {
        editingPatient = null;
        txtIdCard.clear();
        txtName.clear();
        cmbGender.getSelectionModel().select(0);
        dpBirthDate.setValue(null);
        txtPhone.clear();
        txtAddress.clear();
        txtAllergy.clear();
        lblMessage.setText("");
        tablePatients.getSelectionModel().clearSelection();
    }

    private void showInfo(String msg) {
        lblMessage.setStyle("-fx-text-fill: #27ae60;");
        lblMessage.setText(msg);
    }

    private void showError(String msg) {
        lblMessage.setStyle("-fx-text-fill: #c0392b;");
        lblMessage.setText(msg);
    }
}
