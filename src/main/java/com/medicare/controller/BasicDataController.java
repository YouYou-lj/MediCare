package com.medicare.controller;

import com.medicare.model.Department;
import com.medicare.model.Doctor;
import com.medicare.model.Schedule;
import com.medicare.service.DepartmentService;
import com.medicare.service.DoctorService;
import com.medicare.service.ScheduleService;
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
import java.util.List;
import java.util.ResourceBundle;

/**
 * 基础数据管理控制器（科室/医生/排班三合一）
 * 所有耗时操作在后台线程执行
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class BasicDataController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(BasicDataController.class);

    private final DepartmentService departmentService = new DepartmentService();
    private final DoctorService doctorService = new DoctorService();
    private final ScheduleService scheduleService = new ScheduleService();

    private final ObservableList<Department> deptList = FXCollections.observableArrayList();
    private final ObservableList<Doctor> doctorList = FXCollections.observableArrayList();
    private final ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();
    private final ObservableList<Department> deptOptions = FXCollections.observableArrayList();
    private final ObservableList<Doctor> doctorOptions = FXCollections.observableArrayList();

    private Department editingDept = null;
    private Doctor editingDoctor = null;
    private Schedule editingSchedule = null;

    // ========== 科室 Tab ==========
    @FXML private TextField txtDeptSearch;
    @FXML private TableView<Department> tableDepartments;
    @FXML private TableColumn<Department, Long> colDeptId;
    @FXML private TableColumn<Department, String> colDeptName;
    @FXML private TableColumn<Department, String> colDeptLocation;
    @FXML private TableColumn<Department, String> colDeptPhone;
    @FXML private Label lblDeptCount, lblDeptMessage;
    @FXML private TextField txtDeptName, txtDeptLocation, txtDeptPhone;

    // ========== 医生 Tab ==========
    @FXML private TextField txtDoctorSearch;
    @FXML private ComboBox<Department> cmbDoctorDeptFilter, cmbDoctorDept;
    @FXML private TableView<Doctor> tableDoctors;
    @FXML private TableColumn<Doctor, Long> colDoctorId;
    @FXML private TableColumn<Doctor, String> colDoctorName;
    @FXML private TableColumn<Doctor, String> colDoctorDept;
    @FXML private TableColumn<Doctor, String> colDoctorTitle;
    @FXML private TableColumn<Doctor, Integer> colDoctorStatus;
    @FXML private Label lblDoctorCount, lblDoctorMessage;
    @FXML private TextField txtDoctorName;
    @FXML private ComboBox<String> cmbDoctorTitle, cmbDoctorStatus;

    // ========== 排班 Tab ==========
    @FXML private DatePicker dpScheduleDate;
    @FXML private ComboBox<Department> cmbScheduleDeptFilter;
    @FXML private TableView<Schedule> tableSchedules;
    @FXML private TableColumn<Schedule, Long> colScheduleId;
    @FXML private TableColumn<Schedule, String> colScheduleDate;
    @FXML private TableColumn<Schedule, String> colScheduleDoctor;
    @FXML private TableColumn<Schedule, String> colScheduleDept;
    @FXML private TableColumn<Schedule, String> colScheduleSlot;
    @FXML private TableColumn<Schedule, Integer> colScheduleTotal;
    @FXML private TableColumn<Schedule, Integer> colScheduleRemain;
    @FXML private Label lblScheduleCount, lblScheduleMessage;
    @FXML private ComboBox<Doctor> cmbScheduleDoctor;
    @FXML private DatePicker dpScheduleWorkDate;
    @FXML private ComboBox<String> cmbScheduleSlot;
    @FXML private Spinner<Integer> spinScheduleTotal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("基础数据管理界面初始化");
        initDeptTab();
        initDoctorTab();
        initScheduleTab();
        loadDeptData();
        loadDoctorData();
        loadScheduleData();
        loadDeptOptions();
    }

    // ============================================================
    // 初始化
    // ============================================================

    private void initDeptTab() {
        colDeptId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDeptName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDeptLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colDeptPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        tableDepartments.setItems(deptList);
        tableDepartments.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) loadDeptToForm(n);
        });
    }

    private void initDoctorTab() {
        colDoctorId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDoctorName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDoctorDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colDoctorTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDoctorStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDoctorStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : (s == 1 ? "在职" : "停用"));
            }
        });
        tableDoctors.setItems(doctorList);
        tableDoctors.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) loadDoctorToForm(n);
        });
        cmbDoctorTitle.setItems(FXCollections.observableArrayList(DoctorService.TITLES));
        cmbDoctorTitle.getSelectionModel().select(0);
        cmbDoctorStatus.setItems(FXCollections.observableArrayList("停用", "在职"));
        cmbDoctorStatus.getSelectionModel().select(1);
    }

    private void initScheduleTab() {
        colScheduleId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colScheduleDate.setCellValueFactory(new PropertyValueFactory<>("workDate"));
        colScheduleDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colScheduleDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colScheduleSlot.setCellValueFactory(new PropertyValueFactory<>("timeSlot"));
        colScheduleTotal.setCellValueFactory(new PropertyValueFactory<>("totalSlots"));
        colScheduleRemain.setCellValueFactory(new PropertyValueFactory<>("remainSlots"));
        tableSchedules.setItems(scheduleList);
        tableSchedules.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) loadScheduleToForm(n);
        });
        cmbScheduleSlot.setItems(FXCollections.observableArrayList(ScheduleService.TIME_SLOTS));
        cmbScheduleSlot.getSelectionModel().select(0);
        spinScheduleTotal.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 20));
        dpScheduleDate.setValue(LocalDate.now());
        dpScheduleWorkDate.setValue(LocalDate.now());
    }

    // ============================================================
    // 数据加载
    // ============================================================

    private void loadDeptData() {
        Task<List<Department>> task = new Task<>() {
            @Override protected List<Department> call() throws Exception {
                return departmentService.listAll();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            deptList.setAll(task.getValue());
            lblDeptCount.setText("共 " + deptList.size() + " 条");
        }));
        task.setOnFailed(e -> logger.error("加载科室失败", task.getException()));
        new Thread(task).start();
    }

    private void loadDoctorData() {
        Task<List<Doctor>> task = new Task<>() {
            @Override protected List<Doctor> call() throws Exception {
                return doctorService.listAll();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            doctorList.setAll(task.getValue());
            lblDoctorCount.setText("共 " + doctorList.size() + " 条");
        }));
        task.setOnFailed(e -> logger.error("加载医生失败", task.getException()));
        new Thread(task).start();
    }

    private void loadScheduleData() {
        Task<List<Schedule>> task = new Task<>() {
            @Override protected List<Schedule> call() throws Exception {
                return scheduleService.listByDate(LocalDate.now());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            scheduleList.setAll(task.getValue());
            lblScheduleCount.setText("共 " + scheduleList.size() + " 条");
        }));
        task.setOnFailed(e -> logger.error("加载排班失败", task.getException()));
        new Thread(task).start();
    }

    private void loadDeptOptions() {
        Task<List<Department>> task = new Task<>() {
            @Override protected List<Department> call() throws Exception {
                return departmentService.listAll();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            deptOptions.setAll(task.getValue());
            cmbDoctorDept.setItems(deptOptions);
            ObservableList<Department> filterList = FXCollections.observableArrayList(task.getValue());
            cmbDoctorDeptFilter.setItems(filterList);
            cmbScheduleDeptFilter.setItems(filterList);
        }));
        task.setOnFailed(e -> logger.error("加载科室选项失败", task.getException()));
        new Thread(task).start();
    }

    private void loadDoctorOptions() {
        Task<List<Doctor>> task = new Task<>() {
            @Override protected List<Doctor> call() throws Exception {
                return doctorService.listAll();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            doctorOptions.setAll(task.getValue());
            cmbScheduleDoctor.setItems(doctorOptions);
        }));
        task.setOnFailed(e -> logger.error("加载医生选项失败", task.getException()));
        new Thread(task).start();
    }

    // ============================================================
    // 科室 Tab
    // ============================================================

    @FXML private void handleDeptSearch() {
        String kw = txtDeptSearch.getText();
        Task<List<Department>> task = new Task<>() {
            @Override protected List<Department> call() throws Exception {
                return departmentService.search(kw);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            deptList.setAll(task.getValue());
            lblDeptCount.setText("共 " + deptList.size() + " 条");
        }));
        new Thread(task).start();
    }

    @FXML private void handleDeptNew() {
        editingDept = null;
        clearDeptForm();
    }

    @FXML private void handleDeptSave() {
        Department d = new Department();
        d.setName(txtDeptName.getText().trim());
        d.setLocation(txtDeptLocation.getText().trim());
        d.setPhone(txtDeptPhone.getText().trim());
        if (d.getName().isEmpty()) { showError(lblDeptMessage, "科室名称不能为空"); return; }

        if (editingDept != null) {
            d.setId(editingDept.getId());
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    departmentService.updateDepartment(d);
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadDeptData(); loadDeptOptions(); showInfo(lblDeptMessage, "更新成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblDeptMessage, task.getException().getMessage())));
            new Thread(task).start();
        } else {
            Task<Long> task = new Task<>() {
                @Override protected Long call() throws Exception {
                    return departmentService.addDepartment(d);
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadDeptData(); loadDeptOptions(); clearDeptForm(); showInfo(lblDeptMessage, "新增成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblDeptMessage, task.getException().getMessage())));
            new Thread(task).start();
        }
    }

    @FXML private void handleDeptClear() { clearDeptForm(); }

    @FXML private void handleDeptDelete() {
        if (editingDept == null) { showError(lblDeptMessage, "请先选择科室"); return; }
        confirm("删除科室", "确定删除 " + editingDept.getName() + " 吗？", () -> {
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    departmentService.deleteDepartment(editingDept.getId());
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadDeptData(); loadDeptOptions(); clearDeptForm(); showInfo(lblDeptMessage, "删除成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblDeptMessage, task.getException().getMessage())));
            new Thread(task).start();
        });
    }

    private void loadDeptToForm(Department d) {
        editingDept = d;
        txtDeptName.setText(d.getName());
        txtDeptLocation.setText(d.getLocation());
        txtDeptPhone.setText(d.getPhone());
    }

    private void clearDeptForm() {
        editingDept = null;
        txtDeptName.clear(); txtDeptLocation.clear(); txtDeptPhone.clear();
        lblDeptMessage.setText("");
        tableDepartments.getSelectionModel().clearSelection();
    }

    // ============================================================
    // 医生 Tab
    // ============================================================

    @FXML private void handleDoctorSearch() {
        String kw = txtDoctorSearch.getText();
        Department filter = cmbDoctorDeptFilter.getValue();
        Task<List<Doctor>> task = new Task<>() {
            @Override protected List<Doctor> call() throws Exception {
                List<Doctor> list = doctorService.search(kw);
                if (filter != null) list.removeIf(d -> !filter.getId().equals(d.getDepartmentId()));
                return list;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            doctorList.setAll(task.getValue());
            lblDoctorCount.setText("共 " + doctorList.size() + " 条");
        }));
        new Thread(task).start();
    }

    @FXML private void handleDoctorNew() {
        editingDoctor = null;
        clearDoctorForm();
        loadDoctorOptions();
    }

    @FXML private void handleDoctorSave() {
        Doctor d = new Doctor();
        d.setName(txtDoctorName.getText().trim());
        Department dept = cmbDoctorDept.getValue();
        d.setDepartmentId(dept != null ? dept.getId() : null);
        d.setTitle(cmbDoctorTitle.getValue());
        d.setStatus(cmbDoctorStatus.getSelectionModel().getSelectedIndex());
        if (d.getName().isEmpty() || d.getDepartmentId() == null || d.getTitle() == null) {
            showError(lblDoctorMessage, "姓名、科室、职称不能为空"); return;
        }

        if (editingDoctor != null) {
            d.setId(editingDoctor.getId());
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    doctorService.updateDoctor(d);
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadDoctorData(); showInfo(lblDoctorMessage, "更新成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblDoctorMessage, task.getException().getMessage())));
            new Thread(task).start();
        } else {
            Task<Long> task = new Task<>() {
                @Override protected Long call() throws Exception {
                    return doctorService.addDoctor(d);
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadDoctorData(); clearDoctorForm(); showInfo(lblDoctorMessage, "新增成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblDoctorMessage, task.getException().getMessage())));
            new Thread(task).start();
        }
    }

    @FXML private void handleDoctorClear() { clearDoctorForm(); }

    @FXML private void handleDoctorDelete() {
        if (editingDoctor == null) { showError(lblDoctorMessage, "请先选择医生"); return; }
        confirm("删除医生", "确定删除 " + editingDoctor.getName() + " 吗？", () -> {
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    doctorService.deleteDoctor(editingDoctor.getId());
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadDoctorData(); clearDoctorForm(); showInfo(lblDoctorMessage, "删除成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblDoctorMessage, task.getException().getMessage())));
            new Thread(task).start();
        });
    }

    private void loadDoctorToForm(Doctor d) {
        editingDoctor = d;
        txtDoctorName.setText(d.getName());
        deptOptions.stream().filter(x -> x.getId().equals(d.getDepartmentId())).findFirst()
                .ifPresent(cmbDoctorDept.getSelectionModel()::select);
        cmbDoctorTitle.setValue(d.getTitle());
        cmbDoctorStatus.getSelectionModel().select(d.getStatus() != null && d.getStatus() == 1 ? 1 : 0);
    }

    private void clearDoctorForm() {
        editingDoctor = null;
        txtDoctorName.clear();
        cmbDoctorDept.getSelectionModel().clearSelection();
        cmbDoctorTitle.getSelectionModel().select(0);
        cmbDoctorStatus.getSelectionModel().select(1);
        lblDoctorMessage.setText("");
        tableDoctors.getSelectionModel().clearSelection();
    }

    // ============================================================
    // 排班 Tab
    // ============================================================

    @FXML private void handleScheduleSearch() {
        LocalDate date = dpScheduleDate.getValue();
        Department filter = cmbScheduleDeptFilter.getValue();
        Task<List<Schedule>> task = new Task<>() {
            @Override protected List<Schedule> call() throws Exception {
                List<Schedule> list = date != null ? scheduleService.listByDate(date) : scheduleService.listAll();
                if (filter != null) list.removeIf(s -> !filter.getName().equals(s.getDepartmentName()));
                return list;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            scheduleList.setAll(task.getValue());
            lblScheduleCount.setText("共 " + scheduleList.size() + " 条");
        }));
        new Thread(task).start();
    }

    @FXML private void handleScheduleNew() {
        editingSchedule = null;
        clearScheduleForm();
        loadDoctorOptions();
    }

    @FXML private void handleScheduleSave() {
        Schedule s = new Schedule();
        Doctor doc = cmbScheduleDoctor.getValue();
        s.setDoctorId(doc != null ? doc.getId() : null);
        s.setWorkDate(dpScheduleWorkDate.getValue());
        s.setTimeSlot(cmbScheduleSlot.getValue());
        s.setTotalSlots(spinScheduleTotal.getValue());
        s.setRemainSlots(spinScheduleTotal.getValue());
        if (s.getDoctorId() == null || s.getWorkDate() == null || s.getTimeSlot() == null) {
            showError(lblScheduleMessage, "医生、日期、时段不能为空"); return;
        }

        if (editingSchedule != null) {
            s.setId(editingSchedule.getId());
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    scheduleService.updateSchedule(s);
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadScheduleData(); showInfo(lblScheduleMessage, "更新成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblScheduleMessage, task.getException().getMessage())));
            new Thread(task).start();
        } else {
            Task<Long> task = new Task<>() {
                @Override protected Long call() throws Exception {
                    return scheduleService.addSchedule(s);
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadScheduleData(); clearScheduleForm(); showInfo(lblScheduleMessage, "新增成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblScheduleMessage, task.getException().getMessage())));
            new Thread(task).start();
        }
    }

    @FXML private void handleScheduleClear() { clearScheduleForm(); }

    @FXML private void handleScheduleDelete() {
        if (editingSchedule == null) { showError(lblScheduleMessage, "请先选择排班"); return; }
        confirm("删除排班", "确定删除该排班记录吗？", () -> {
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    scheduleService.deleteSchedule(editingSchedule.getId());
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadScheduleData(); clearScheduleForm(); showInfo(lblScheduleMessage, "删除成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblScheduleMessage, task.getException().getMessage())));
            new Thread(task).start();
        });
    }

    private void loadScheduleToForm(Schedule s) {
        editingSchedule = s;
        doctorOptions.stream().filter(x -> x.getId().equals(s.getDoctorId())).findFirst()
                .ifPresent(cmbScheduleDoctor.getSelectionModel()::select);
        dpScheduleWorkDate.setValue(s.getWorkDate());
        cmbScheduleSlot.setValue(s.getTimeSlot());
        spinScheduleTotal.getValueFactory().setValue(s.getTotalSlots());
    }

    private void clearScheduleForm() {
        editingSchedule = null;
        cmbScheduleDoctor.getSelectionModel().clearSelection();
        dpScheduleWorkDate.setValue(LocalDate.now());
        cmbScheduleSlot.getSelectionModel().select(0);
        spinScheduleTotal.getValueFactory().setValue(20);
        lblScheduleMessage.setText("");
        tableSchedules.getSelectionModel().clearSelection();
    }

    // ============================================================
    // 通用辅助
    // ============================================================

    private void confirm(String title, String content, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) onConfirm.run(); });
    }

    private void showInfo(Label label, String msg) {
        label.setStyle("-fx-text-fill: #27ae60;");
        label.setText(msg);
    }

    private void showError(Label label, String msg) {
        label.setStyle("-fx-text-fill: #c0392b;");
        label.setText(msg);
    }
}
