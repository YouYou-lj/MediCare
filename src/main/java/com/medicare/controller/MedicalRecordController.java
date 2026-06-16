package com.medicare.controller;

import com.medicare.model.MedicalRecord;
import com.medicare.service.MedicalRecordService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 病历管理控制器（独立模块）
 * 全量病历列表、搜索、详情查看、编辑、删除
 */
public class MedicalRecordController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordController.class);

    private final MedicalRecordService medicalRecordService = new MedicalRecordService();
    private final ObservableList<MedicalRecord> recordList = FXCollections.observableArrayList();

    // ========== 顶部工具栏 ==========
    @FXML private TextField txtSearch;
    @FXML private Label lblRecordCount;

    // ========== 病历列表表格 ==========
    @FXML private TableView<MedicalRecord> tableRecords;
    @FXML private TableColumn<MedicalRecord, Long> colId;
    @FXML private TableColumn<MedicalRecord, String> colPatient, colDoctor, colChief, colDiagnosis;
    @FXML private TableColumn<MedicalRecord, LocalDateTime> colCreateTime;
    @FXML private TableColumn<MedicalRecord, Void> colAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("病历管理界面初始化");
        initTable();
        loadData();
    }

    // ============================================================
    // 表格初始化（含操作按钮列）
    // ============================================================

    private void initTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colChief.setCellValueFactory(new PropertyValueFactory<>("chiefComplaint"));
        colDiagnosis.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));

        colCreateTime.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        colCreateTime.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText(empty || dt == null ? null : dt.format(fmt));
            }
        });

        // 操作列：查看 + 删除
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("查看");
            private final Button btnDel = new Button("删除");
            private final HBox box = new HBox(5, btnView, btnDel);

            {
                btnView.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-text-fill: #2980b9;");
                btnDel.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-text-fill: #c0392b;");
                btnView.setOnAction(e -> onViewRecord(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> onDeleteRecord(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableRecords.setItems(recordList);
    }

    // ============================================================
    // 数据加载
    // ============================================================

    private void loadData() {
        Task<List<MedicalRecord>> task = new Task<>() {
            @Override protected List<MedicalRecord> call() throws Exception {
                return medicalRecordService.listAll();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            recordList.setAll(task.getValue());
            lblRecordCount.setText("共 " + recordList.size() + " 条记录");
        }));
        task.setOnFailed(e -> logger.error("加载病历数据失败", task.getException()));
        new Thread(task).start();
    }

    // ============================================================
    // 搜索 / 刷新
    // ============================================================

    @FXML private void handleSearch() {
        String kw = txtSearch.getText().trim();
        Task<List<MedicalRecord>> task = new Task<>() {
            @Override protected List<MedicalRecord> call() throws Exception {
                return medicalRecordService.search(kw);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            recordList.setAll(task.getValue());
            lblRecordCount.setText("共 " + recordList.size() + " 条记录");
        }));
        task.setOnFailed(e -> logger.error("搜索病历失败", task.getException()));
        new Thread(task).start();
    }

    @FXML private void handleRefresh() {
        txtSearch.clear();
        loadData();
    }

    // ============================================================
    // 查看详情弹窗
    // ============================================================

    private void onViewRecord(MedicalRecord record) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MedicalRecordDetailDialog.fxml"));
            DialogPane pane = loader.load();
            MedicalRecordDetailDialogController controller = loader.getController();
            controller.setRecord(record);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("病历详情 - " + record.getPatientName());
            dialog.showAndWait();
        } catch (IOException e) {
            logger.error("打开病历详情弹窗失败", e);
            showAlert(Alert.AlertType.ERROR, "打开弹窗失败: " + e.getMessage());
        }
    }

    // ============================================================
    // 删除病历
    // ============================================================

    private void onDeleteRecord(MedicalRecord record) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除病历");
        alert.setHeaderText(null);
        alert.setContentText("确定删除患者 " + record.getPatientName() + " 的这条病历记录吗？此操作不可恢复！");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws Exception {
                        medicalRecordService.deleteRecord(record.getId());
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
