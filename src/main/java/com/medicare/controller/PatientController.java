package com.medicare.controller;

import com.medicare.model.Patient;
import com.medicare.service.PatientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 患者管理控制器（表格为主 + 弹窗编辑）
 * 复用 PatientDialog 进行新增/编辑
 */
public class PatientController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private final PatientService patientService = new PatientService();
    private final ObservableList<Patient> patientList = FXCollections.observableArrayList();

    // ========== 顶部工具栏 ==========
    @FXML private TextField txtSearch;
    @FXML private Label lblRecordCount;

    // ========== 患者列表表格 ==========
    @FXML private TableView<Patient> tablePatients;
    @FXML private TableColumn<Patient, Long> colId;
    @FXML private TableColumn<Patient, String> colIdCard, colName, colPhone, colAddress;
    @FXML private TableColumn<Patient, Integer> colGender;
    @FXML private TableColumn<Patient, LocalDate> colBirthDate;
    @FXML private TableColumn<Patient, LocalDateTime> colCreateTime;
    @FXML private TableColumn<Patient, Void> colAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("患者管理界面初始化");
        initTable();
        loadData();
    }

    // ============================================================
    // 表格初始化（含操作按钮列）
    // ============================================================

    private void initTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colIdCard.setCellValueFactory(new PropertyValueFactory<>("idCard"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colGender.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer gender, boolean empty) {
                super.updateItem(gender, empty);
                setText(empty || gender == null ? null : (gender == 0 ? "女性" : gender == 1 ? "男性" : "其他"));
            }
        });

        colBirthDate.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));

        colCreateTime.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        colCreateTime.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText(empty || dt == null ? null : dt.format(fmt));
            }
        });

        // 操作列：编辑 + 删除
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("编辑");
            private final Button btnDel = new Button("删除");
            private final HBox box = new HBox(5, btnEdit, btnDel);

            {
                btnEdit.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-text-fill: #2980b9;");
                btnDel.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-text-fill: #c0392b;");
                btnEdit.setOnAction(e -> onEditPatient(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> onDeletePatient(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tablePatients.setItems(patientList);
    }

    // ============================================================
    // 数据加载
    // ============================================================

    private void loadData() {
        Task<List<Patient>> task = new Task<>() {
            @Override protected List<Patient> call() throws Exception { return patientService.listAll(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            patientList.setAll(task.getValue());
            lblRecordCount.setText("共 " + patientList.size() + " 条记录");
        }));
        task.setOnFailed(e -> logger.error("加载患者数据失败", task.getException()));
        new Thread(task).start();
    }

    // ============================================================
    // 搜索 / 刷新
    // ============================================================

    @FXML private void handleSearch() {
        String kw = txtSearch.getText();
        Task<List<Patient>> task = new Task<>() {
            @Override protected List<Patient> call() throws Exception { return patientService.search(kw); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            patientList.setAll(task.getValue());
            lblRecordCount.setText("共 " + patientList.size() + " 条记录");
        }));
        task.setOnFailed(e -> logger.error("查询患者失败", task.getException()));
        new Thread(task).start();
    }

    @FXML private void handleRefresh() {
        txtSearch.clear();
        loadData();
    }

    // ============================================================
    // 新增 / 编辑患者（弹窗）
    // ============================================================

    @FXML private void handleNew() {
        openPatientDialog(null, "新增患者");
    }

    private void onEditPatient(Patient patient) {
        openPatientDialog(patient, "编辑患者");
    }

    private void openPatientDialog(Patient patient, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PatientDialog.fxml"));
            DialogPane pane = loader.load();
            PatientDialogController controller = loader.getController();
            controller.setPatient(patient);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle(title);

            final Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.setText("保存");
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                Patient p = controller.getPatient();
                if (p == null) {
                    event.consume();
                    return;
                }
                event.consume();
                okButton.setDisable(true);
                savePatient(p, dialog, okButton);
            });

            dialog.showAndWait();
        } catch (IOException e) {
            logger.error("打开患者弹窗失败", e);
            showAlert(Alert.AlertType.ERROR, "打开弹窗失败: " + e.getMessage());
        }
    }

    private void savePatient(Patient patient, Dialog<ButtonType> dialog, Button okButton) {
        if (patient.getId() != null) {
            // 更新
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    patientService.updatePatient(patient);
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadData();
                dialog.close();
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "患者更新成功"));
            }));
            task.setOnFailed(e -> Platform.runLater(() -> {
                okButton.setDisable(false);
                showAlert(Alert.AlertType.ERROR, task.getException().getMessage());
            }));
            new Thread(task).start();
        } else {
            // 新增
            Task<Long> task = new Task<>() {
                @Override protected Long call() throws Exception {
                    return patientService.registerPatient(patient);
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadData();
                dialog.close();
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "患者建档成功"));
            }));
            task.setOnFailed(e -> Platform.runLater(() -> {
                okButton.setDisable(false);
                showAlert(Alert.AlertType.ERROR, task.getException().getMessage());
            }));
            new Thread(task).start();
        }
    }

    // ============================================================
    // 删除患者
    // ============================================================

    private void onDeletePatient(Patient patient) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除患者");
        alert.setHeaderText(null);
        alert.setContentText("确定删除患者 " + patient.getName() + " 的档案吗？此操作不可恢复！");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws Exception {
                        patientService.deletePatient(patient.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    loadData();
                    showAlert(Alert.AlertType.INFORMATION, "删除成功");
                }));
                task.setOnFailed(e -> Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, task.getException().getMessage())));
                new Thread(task).start();
            }
        });
    }

    // ============================================================
    // 通用辅助
    // ============================================================

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "错误" : "提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
