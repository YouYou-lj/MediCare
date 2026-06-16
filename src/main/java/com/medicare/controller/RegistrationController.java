package com.medicare.controller;

import com.medicare.model.Department;
import com.medicare.model.Patient;
import com.medicare.model.Registration;
import com.medicare.model.Schedule;
import com.medicare.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 挂号预约控制器
 * 核心业务流程：选号源 → 弹窗选患者 → 挂号 → 叫号/完成/取消
 */
public class RegistrationController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    private final ScheduleService scheduleService = new ScheduleService();
    private final RegistrationService registrationService = new RegistrationService();
    private final DepartmentService departmentService = new DepartmentService();

    private final ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();
    private final ObservableList<Registration> regList = FXCollections.observableArrayList();
    private final ObservableList<Department> deptOptions = FXCollections.observableArrayList();

    private Integer currentFilterStatus = null;

    // ========== FXML 注入 ==========
    @FXML private DatePicker dpWorkDate;
    @FXML private ComboBox<Department> cmbDepartmentFilter;
    @FXML private TableView<Schedule> tableSchedules;
    @FXML private TableColumn<Schedule, Long> colSchId;
    @FXML private TableColumn<Schedule, String> colSchDoctor, colSchDept, colSchSlot;
    @FXML private TableColumn<Schedule, Integer> colSchTotal, colSchRemain;
    @FXML private Label lblScheduleCount;
    @FXML private TableView<Registration> tableRegistrations;
    @FXML private TableColumn<Registration, Integer> colRegSeq;
    @FXML private TableColumn<Registration, String> colRegPatient, colRegDoctor, colRegDept, colRegSlot;
    @FXML private TableColumn<Registration, Integer> colRegStatus;
    @FXML private TableColumn<Registration, LocalDateTime> colRegTime;
    @FXML private Label lblRegCount;
    @FXML private Label lblStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("挂号预约界面初始化");
        initScheduleTable();
        initRegTable();
        dpWorkDate.setValue(LocalDate.now());
        loadDeptOptions();
        loadScheduleData();
        loadRegData();
    }

    // ============================================================
    // 表格初始化
    // ============================================================

    private void initScheduleTable() {
        colSchId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSchDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colSchDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colSchSlot.setCellValueFactory(new PropertyValueFactory<>("timeSlot"));
        colSchTotal.setCellValueFactory(new PropertyValueFactory<>("totalSlots"));
        colSchRemain.setCellValueFactory(new PropertyValueFactory<>("remainSlots"));
        colSchRemain.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer remain, boolean empty) {
                super.updateItem(remain, empty);
                if (empty || remain == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(remain));
                    setStyle(remain <= 3 ? "-fx-text-fill: #c0392b; -fx-font-weight: bold;" : "-fx-text-fill: #27ae60;");
                }
            }
        });
        tableSchedules.setItems(scheduleList);
    }

    private void initRegTable() {
        colRegSeq.setCellValueFactory(new PropertyValueFactory<>("seqNo"));
        colRegPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colRegDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colRegDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colRegSlot.setCellValueFactory(new PropertyValueFactory<>("timeSlot"));
        colRegStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colRegStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(Registration.getStatusText(status));
                    switch (status) {
                        case 0 -> setStyle("-fx-text-fill: #f39c12;");
                        case 1 -> setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                        case 2 -> setStyle("-fx-text-fill: #27ae60;");
                        case 3 -> setStyle("-fx-text-fill: #7f8c8d;");
                    }
                }
            }
        });
        colRegTime.setCellValueFactory(new PropertyValueFactory<>("regTime"));
        colRegTime.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText(empty || dt == null ? null : dt.format(fmt));
            }
        });
        tableRegistrations.setItems(regList);
    }

    // ============================================================
    // 数据加载
    // ============================================================

    private void loadDeptOptions() {
        Task<List<Department>> task = new Task<>() {
            @Override protected List<Department> call() throws Exception {
                return departmentService.listAll();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            deptOptions.setAll(task.getValue());
            cmbDepartmentFilter.setItems(FXCollections.observableArrayList(task.getValue()));
        }));
        task.setOnFailed(e -> logger.error("加载科室失败", task.getException()));
        new Thread(task).start();
    }

    private void loadScheduleData() {
        LocalDate date = dpWorkDate.getValue();
        Department filter = cmbDepartmentFilter.getValue();
        Task<List<Schedule>> task = new Task<>() {
            @Override protected List<Schedule> call() throws Exception {
                List<Schedule> list = date != null ? scheduleService.listByDate(date) : scheduleService.listAll();
                if (filter != null) {
                    list.removeIf(s -> !filter.getName().equals(s.getDepartmentName()));
                }
                return list;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            scheduleList.setAll(task.getValue());
            lblScheduleCount.setText("共 " + scheduleList.size() + " 条");
        }));
        task.setOnFailed(e -> logger.error("加载号源失败", task.getException()));
        new Thread(task).start();
    }

    private void loadRegData() {
        Task<List<Registration>> task = new Task<>() {
            @Override protected List<Registration> call() throws Exception {
                List<Registration> list = registrationService.listToday();
                if (currentFilterStatus != null) {
                    list.removeIf(r -> !currentFilterStatus.equals(r.getStatus()));
                }
                return list;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            regList.setAll(task.getValue());
            lblRegCount.setText("共 " + regList.size() + " 条");
        }));
        task.setOnFailed(e -> logger.error("加载挂号记录失败", task.getException()));
        new Thread(task).start();
    }

    // ============================================================
    // 号源查询
    // ============================================================

    @FXML
    private void handleSearchSchedule() {
        loadScheduleData();
    }

    // ============================================================
    // 挂号（弹窗选择患者）
    // ============================================================

    @FXML
    private void handleRegister() {
        Schedule schedule = tableSchedules.getSelectionModel().getSelectedItem();
        if (schedule == null) {
            lblStatus.setText("请选择号源");
            return;
        }
        if (schedule.getRemainSlots() == null || schedule.getRemainSlots() <= 0) {
            lblStatus.setText("该号源已售罄");
            return;
        }

        openPatientSelectDialog(schedule);
    }

    private void openPatientSelectDialog(Schedule schedule) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PatientSelectDialog.fxml"));
            DialogPane pane = loader.load();
            PatientSelectDialogController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("选择患者");

            final Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.setText("确定挂号");
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                Patient patient = controller.getSelectedPatient();
                if (patient == null) {
                    event.consume();
                    lblStatus.setText("请先选择患者");
                    return;
                }
                event.consume();
                dialog.close();
                doRegister(schedule, patient);
            });

            dialog.showAndWait();
        } catch (IOException e) {
            logger.error("打开患者选择弹窗失败", e);
            lblStatus.setText("打开弹窗失败");
        }
    }

    private void doRegister(Schedule schedule, Patient patient) {
        Registration reg = new Registration();
        reg.setPatientId(patient.getId());
        reg.setScheduleId(schedule.getId());
        reg.setFee(BigDecimal.valueOf(10.00));

        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception {
                return registrationService.register(reg);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            lblStatus.setText("挂号成功！患者: " + patient.getName() + " 序号: " + reg.getSeqNo());
            loadScheduleData();
            loadRegData();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            lblStatus.setText("挂号失败: " + task.getException().getMessage());
        }));
        new Thread(task).start();
    }

    // ============================================================
    // 叫号 / 完成 / 取消
    // ============================================================

    @FXML
    private void handleCall() {
        Registration reg = tableRegistrations.getSelectionModel().getSelectedItem();
        if (reg == null) { lblStatus.setText("请先选择挂号记录"); return; }
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                registrationService.callPatient(reg.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            lblStatus.setText("叫号成功: " + reg.getPatientName());
            loadRegData();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> lblStatus.setText(task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleComplete() {
        Registration reg = tableRegistrations.getSelectionModel().getSelectedItem();
        if (reg == null) { lblStatus.setText("请先选择挂号记录"); return; }
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                registrationService.completeRegistration(reg.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            lblStatus.setText("就诊完成: " + reg.getPatientName());
            loadRegData();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> lblStatus.setText(task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleCancelReg() {
        Registration reg = tableRegistrations.getSelectionModel().getSelectedItem();
        if (reg == null) { lblStatus.setText("请先选择挂号记录"); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("取消挂号");
        alert.setHeaderText(null);
        alert.setContentText("确定取消 " + reg.getPatientName() + " 的挂号吗？");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws Exception {
                        registrationService.cancelRegistration(reg.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    lblStatus.setText("已取消挂号: " + reg.getPatientName());
                    loadScheduleData();
                    loadRegData();
                }));
                task.setOnFailed(e -> Platform.runLater(() -> lblStatus.setText(task.getException().getMessage())));
                new Thread(task).start();
            }
        });
    }

    // ============================================================
    // 状态筛选
    // ============================================================

    @FXML private void filterAll() { currentFilterStatus = null; loadRegData(); }
    @FXML private void filterWaiting() { currentFilterStatus = Registration.STATUS_WAITING; loadRegData(); }
    @FXML private void filterInProgress() { currentFilterStatus = Registration.STATUS_IN_PROGRESS; loadRegData(); }
    @FXML private void filterCompleted() { currentFilterStatus = Registration.STATUS_COMPLETED; loadRegData(); }
}
