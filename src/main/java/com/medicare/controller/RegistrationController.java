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
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 挂号预约控制器
 * 核心业务流程：选患者 → 选号源 → 挂号 → 叫号/完成/取消
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class RegistrationController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    private final ScheduleService scheduleService = new ScheduleService();
    private final PatientService patientService = new PatientService();
    private final RegistrationService registrationService = new RegistrationService();
    private final DepartmentService departmentService = new DepartmentService();

    private final ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();
    private final ObservableList<Registration> regList = FXCollections.observableArrayList();
    private final ObservableList<Department> deptOptions = FXCollections.observableArrayList();

    private Patient selectedPatient = null;
    private Integer currentFilterStatus = null;

    // ========== FXML 注入 ==========
    @FXML private DatePicker dpWorkDate;
    @FXML private ComboBox<Department> cmbDepartmentFilter;
    @FXML private TextField txtPatientSearch;
    @FXML private Label lblSelectedPatient;
    @FXML private TableView<Schedule> tableSchedules;
    @FXML private TableColumn<Schedule, Long> colSchId;
    @FXML private TableColumn<Schedule, String> colSchDoctor, colSchDept, colSchSlot;
    @FXML private TableColumn<Schedule, Integer> colSchTotal, colSchRemain;
    @FXML private Label lblScheduleCount;
    @FXML private TableView<Registration> tableRegistrations;
    @FXML private TableColumn<Registration, Integer> colRegSeq;
    @FXML private TableColumn<Registration, String> colRegPatient, colRegDoctor, colRegDept, colRegSlot;
    @FXML private TableColumn<Registration, Integer> colRegStatus;
    @FXML private TableColumn<Registration, String> colRegTime;
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
                    setText(new Registration().getStatusText(status));
                    switch (status) {
                        case 0 -> setStyle("-fx-text-fill: #f39c12;"); // 候诊 - 黄
                        case 1 -> setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;"); // 就诊中 - 蓝
                        case 2 -> setStyle("-fx-text-fill: #27ae60;"); // 已完成 - 绿
                        case 3 -> setStyle("-fx-text-fill: #7f8c8d;"); // 已取消 - 灰
                    }
                }
            }
        });
        colRegTime.setCellValueFactory(new PropertyValueFactory<>("regTime"));
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
    // 患者搜索
    // ============================================================

    @FXML
    private void handleSearchPatient() {
        String keyword = txtPatientSearch.getText().trim();
        if (keyword.isEmpty()) {
            lblSelectedPatient.setText("请输入身份证号或姓名");
            return;
        }
        Task<List<Patient>> task = new Task<>() {
            @Override protected List<Patient> call() throws Exception {
                return patientService.search(keyword);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Patient> list = task.getValue();
            if (list.isEmpty()) {
                lblSelectedPatient.setText("未找到患者，请先建档");
                selectedPatient = null;
            } else {
                selectedPatient = list.get(0);
                lblSelectedPatient.setText("已选择: " + selectedPatient.getName() + " (" + selectedPatient.getIdCard() + ")");
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> lblSelectedPatient.setText("查询失败")));
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
    // 挂号
    // ============================================================

    @FXML
    private void handleRegister() {
        if (selectedPatient == null) {
            lblStatus.setText("请先查找并选择患者");
            return;
        }
        Schedule schedule = tableSchedules.getSelectionModel().getSelectedItem();
        if (schedule == null) {
            lblStatus.setText("请选择号源");
            return;
        }
        if (schedule.getRemainSlots() == null || schedule.getRemainSlots() <= 0) {
            lblStatus.setText("该号源已售罄");
            return;
        }

        Registration reg = new Registration();
        reg.setPatientId(selectedPatient.getId());
        reg.setScheduleId(schedule.getId());
        reg.setFee(BigDecimal.valueOf(10.00));

        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception {
                return registrationService.register(reg);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            lblStatus.setText("挂号成功！序号: " + reg.getSeqNo());
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
