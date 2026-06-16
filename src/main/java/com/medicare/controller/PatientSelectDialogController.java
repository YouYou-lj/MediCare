package com.medicare.controller;

import com.medicare.model.Patient;
import com.medicare.service.PatientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 患者选择弹窗控制器
 * 用于挂号预约时选择患者
 */
public class PatientSelectDialogController {

    private static final Logger logger = LoggerFactory.getLogger(PatientSelectDialogController.class);

    private final PatientService patientService = new PatientService();
    private final ObservableList<Patient> patientList = FXCollections.observableArrayList();

    @FXML private TextField txtSearch;
    @FXML private TableView<Patient> tablePatients;
    @FXML private TableColumn<Patient, Long> colId;
    @FXML private TableColumn<Patient, String> colIdCard, colName, colPhone;
    @FXML private TableColumn<Patient, Integer> colGender;
    @FXML private TableColumn<Patient, LocalDate> colBirthDate;
    @FXML private Label lblCount;

    @FXML
    public void initialize() {
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
        colBirthDate.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            @Override protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : date.format(fmt));
            }
        });
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        tablePatients.setItems(patientList);

        loadAllPatients();
    }

    /**
     * 获取选中的患者
     */
    public Patient getSelectedPatient() {
        return tablePatients.getSelectionModel().getSelectedItem();
    }

    // ============================================================
    // 搜索
    // ============================================================

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText().trim();
        Task<List<Patient>> task = new Task<>() {
            @Override protected List<Patient> call() throws Exception {
                return patientService.search(keyword);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            patientList.setAll(task.getValue());
            lblCount.setText("共 " + patientList.size() + " 条");
        }));
        task.setOnFailed(e -> logger.error("查询患者失败", task.getException()));
        new Thread(task).start();
    }

    // ============================================================
    // 新增患者（打开 PatientDialog 弹窗）
    // ============================================================

    @FXML
    private void handleNewPatient() {
        openPatientDialog(null, "新增患者");
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
        }
    }

    private void savePatient(Patient patient, Dialog<ButtonType> dialog, Button okButton) {
        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception {
                return patientService.registerPatient(patient);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            patient.setId(task.getValue());
            dialog.close();
            loadAllPatients();
            // 自动选中新增加的患者
            tablePatients.getSelectionModel().select(patient);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            okButton.setDisable(false);
            logger.error("保存患者失败", task.getException());
        }));
        new Thread(task).start();
    }

    private void loadAllPatients() {
        Task<List<Patient>> task = new Task<>() {
            @Override protected List<Patient> call() throws Exception {
                return patientService.listAll();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            patientList.setAll(task.getValue());
            lblCount.setText("共 " + patientList.size() + " 条");
        }));
        task.setOnFailed(e -> logger.error("加载患者失败", task.getException()));
        new Thread(task).start();
    }
}
