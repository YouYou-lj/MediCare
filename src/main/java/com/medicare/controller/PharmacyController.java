package com.medicare.controller;

import com.medicare.model.InventoryLog;
import com.medicare.model.Medicine;
import com.medicare.service.MedicineService;
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
 * 药品库存管理控制器
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class PharmacyController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(PharmacyController.class);

    private final MedicineService medicineService = new MedicineService();
    private final ObservableList<Medicine> medicineList = FXCollections.observableArrayList();
    private final ObservableList<InventoryLog> logList = FXCollections.observableArrayList();

    private Medicine editingMedicine = null;

    // ========== 药品列表 ==========
    @FXML private TextField txtSearch;
    @FXML private Label lblTotalMedicines, lblLowStock, lblNearExpiry;
    @FXML private TableView<Medicine> tableMedicines;
    @FXML private TableColumn<Medicine, Long> colMedId;
    @FXML private TableColumn<Medicine, String> colMedName, colMedSpec, colMedUnit;
    @FXML private TableColumn<Medicine, Integer> colMedStock, colMedSafety;
    @FXML private TableColumn<Medicine, String> colMedPrice;
    @FXML private TableColumn<Medicine, String> colMedExpiry;
    @FXML private TableColumn<Medicine, Integer> colMedStatus;

    // ========== 药品信息 Tab ==========
    @FXML private TextField txtMedName, txtMedSpec, txtMedUnit, txtMedPrice, txtMedBatch, txtMedPinyin, txtMedManufacturer;
    @FXML private Spinner<Integer> spinSafetyStock;
    @FXML private Label lblCurrentStock, lblMedMessage;
    @FXML private DatePicker dpMedExpiry;
    @FXML private ComboBox<String> cmbMedStatus;

    // ========== 入出库 Tab ==========
    @FXML private ComboBox<String> cmbOpType;
    @FXML private Spinner<Integer> spinOpQty;
    @FXML private TextField txtOpBatch, txtOpOperator, txtOpRemark;
    @FXML private DatePicker dpOpExpiry;
    @FXML private Label lblOpMessage;
    @FXML private TableView<InventoryLog> tableLogs;
    @FXML private TableColumn<InventoryLog, String> colLogTime, colLogBatch, colLogOperator, colLogRemark;
    @FXML private TableColumn<InventoryLog, Integer> colLogType, colLogQty;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("药品库存管理界面初始化");
        initMedicineTable();
        initLogTable();
        cmbMedStatus.setItems(FXCollections.observableArrayList("停用", "启用"));
        cmbMedStatus.getSelectionModel().select(1);
        cmbOpType.setItems(FXCollections.observableArrayList("入库", "出库"));
        cmbOpType.getSelectionModel().select(0);
        spinSafetyStock.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9999, 10));
        spinOpQty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));
        loadMedicineData();
        loadAlerts();
    }

    // ============================================================
    // 表格初始化
    // ============================================================

    private void initMedicineTable() {
        colMedId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMedName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMedSpec.setCellValueFactory(new PropertyValueFactory<>("spec"));
        colMedUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colMedStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colMedStock.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) { setText(null); setStyle(""); }
                else {
                    setText(String.valueOf(stock));
                    Medicine m = getTableView().getItems().get(getIndex());
                    if (m != null && m.isLowStock()) setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #27ae60;");
                }
            }
        });
        colMedSafety.setCellValueFactory(new PropertyValueFactory<>("safetyStock"));
        colMedPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colMedExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        colMedExpiry.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) { setText(null); setStyle(""); }
                else {
                    setText(date);
                    Medicine m = getTableView().getItems().get(getIndex());
                    if (m != null && m.isNearExpiry()) setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    else setStyle("");
                }
            }
        });
        colMedStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colMedStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : (s == 1 ? "启用" : "停用"));
            }
        });
        tableMedicines.setItems(medicineList);
        tableMedicines.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) loadMedicineToForm(n);
        });
    }

    private void initLogTable() {
        colLogTime.setCellValueFactory(new PropertyValueFactory<>("logTime"));
        colLogType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colLogType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setText(null); setStyle(""); }
                else {
                    setText(type == 1 ? "入库" : type == 2 ? "出库" : type == 3 ? "盘盈" : "盘亏");
                    setStyle(type == 1 || type == 3 ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #c0392b;");
                }
            }
        });
        colLogQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colLogBatch.setCellValueFactory(new PropertyValueFactory<>("batchNo"));
        colLogOperator.setCellValueFactory(new PropertyValueFactory<>("operator"));
        colLogRemark.setCellValueFactory(new PropertyValueFactory<>("remark"));
        tableLogs.setItems(logList);
    }

    // ============================================================
    // 数据加载
    // ============================================================

    private void loadMedicineData() {
        Task<List<Medicine>> task = new Task<>() {
            @Override protected List<Medicine> call() throws Exception { return medicineService.listAll(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            medicineList.setAll(task.getValue());
            lblTotalMedicines.setText("药品总数: " + medicineList.size());
        }));
        task.setOnFailed(e -> logger.error("加载药品失败", task.getException()));
        new Thread(task).start();
    }

    private void loadAlerts() {
        Task<List<Medicine>> lowTask = new Task<>() {
            @Override protected List<Medicine> call() throws Exception { return medicineService.listLowStock(); }
        };
        lowTask.setOnSucceeded(e -> Platform.runLater(() -> lblLowStock.setText("库存预警: " + lowTask.getValue().size())));
        new Thread(lowTask).start();

        Task<List<Medicine>> expiryTask = new Task<>() {
            @Override protected List<Medicine> call() throws Exception { return medicineService.listNearExpiry(); }
        };
        expiryTask.setOnSucceeded(e -> Platform.runLater(() -> lblNearExpiry.setText("近效期预警: " + expiryTask.getValue().size())));
        new Thread(expiryTask).start();
    }

    private void loadLogs(Long medicineId) {
        if (medicineId == null) { logList.clear(); return; }
        Task<List<InventoryLog>> task = new Task<>() {
            @Override protected List<InventoryLog> call() throws Exception { return medicineService.listLogsByMedicine(medicineId); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> logList.setAll(task.getValue())));
        task.setOnFailed(e -> logger.error("加载日志失败", task.getException()));
        new Thread(task).start();
    }

    // ============================================================
    // 搜索 / 刷新
    // ============================================================

    @FXML private void handleSearch() {
        String kw = txtSearch.getText();
        Task<List<Medicine>> task = new Task<>() {
            @Override protected List<Medicine> call() throws Exception { return medicineService.search(kw); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> medicineList.setAll(task.getValue())));
        new Thread(task).start();
    }

    @FXML private void handleRefresh() {
        txtSearch.clear();
        loadMedicineData();
        loadAlerts();
    }

    // ============================================================
    // 药品档案操作
    // ============================================================

    @FXML private void handleNewMedicine() {
        editingMedicine = null;
        clearMedicineForm();
    }

    @FXML private void handleSaveMedicine() {
        Medicine m = buildMedicineFromForm();
        if (m == null) return;

        if (editingMedicine != null) {
            m.setId(editingMedicine.getId());
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception { medicineService.updateMedicine(m); return null; }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadMedicineData(); loadAlerts(); showInfo(lblMedMessage, "更新成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblMedMessage, task.getException().getMessage())));
            new Thread(task).start();
        } else {
            Task<Long> task = new Task<>() {
                @Override protected Long call() throws Exception { return medicineService.addMedicine(m); }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadMedicineData(); loadAlerts(); clearMedicineForm(); showInfo(lblMedMessage, "新增成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showError(lblMedMessage, task.getException().getMessage())));
            new Thread(task).start();
        }
    }

    @FXML private void handleClearMedicine() { clearMedicineForm(); }

    @FXML private void handleDeleteMedicine() {
        if (editingMedicine == null) { showError(lblMedMessage, "请先选择药品"); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除药品");
        alert.setHeaderText(null);
        alert.setContentText("确定删除 " + editingMedicine.getName() + " 吗？");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws Exception { medicineService.deleteMedicine(editingMedicine.getId()); return null; }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    loadMedicineData(); loadAlerts(); clearMedicineForm(); showInfo(lblMedMessage, "删除成功");
                }));
                task.setOnFailed(e -> Platform.runLater(() -> showError(lblMedMessage, task.getException().getMessage())));
                new Thread(task).start();
            }
        });
    }

    // ============================================================
    // 入出库操作
    // ============================================================

    @FXML private void handleStockOp() {
        if (editingMedicine == null) { showError(lblOpMessage, "请先选择药品"); return; }
        String typeStr = cmbOpType.getValue();
        int qty = spinOpQty.getValue();
        String operator = txtOpOperator.getText().trim();
        String remark = txtOpRemark.getText().trim();
        String batchNo = txtOpBatch.getText().trim();
        LocalDate expiry = dpOpExpiry.getValue();

        if (qty <= 0) { showError(lblOpMessage, "数量必须大于0"); return; }

        boolean isIn = "入库".equals(typeStr);
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                if (isIn) {
                    medicineService.stockIn(editingMedicine.getId(), qty, batchNo, expiry, operator, remark);
                } else {
                    medicineService.stockOut(editingMedicine.getId(), qty, operator, remark);
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            showInfo(lblOpMessage, typeStr + "成功: " + qty);
            loadMedicineData();
            loadAlerts();
            loadLogs(editingMedicine.getId());
            lblCurrentStock.setText(String.valueOf(editingMedicine.getStock() + (isIn ? qty : -qty)));
        }));
        task.setOnFailed(e -> Platform.runLater(() -> showError(lblOpMessage, task.getException().getMessage())));
        new Thread(task).start();
    }

    // ============================================================
    // 表单辅助
    // ============================================================

    private void loadMedicineToForm(Medicine m) {
        editingMedicine = m;
        txtMedName.setText(m.getName());
        txtMedSpec.setText(m.getSpec());
        txtMedUnit.setText(m.getUnit());
        txtMedPrice.setText(m.getPrice() != null ? m.getPrice().toString() : "");
        spinSafetyStock.getValueFactory().setValue(m.getSafetyStock() != null ? m.getSafetyStock() : 10);
        lblCurrentStock.setText(String.valueOf(m.getStock()));
        dpMedExpiry.setValue(m.getExpiryDate());
        txtMedBatch.setText(m.getBatchNo());
        txtMedPinyin.setText(m.getPinyinCode());
        txtMedManufacturer.setText(m.getManufacturer());
        cmbMedStatus.getSelectionModel().select(m.getStatus() != null && m.getStatus() == 1 ? 1 : 0);
        loadLogs(m.getId());
    }

    private Medicine buildMedicineFromForm() {
        String name = txtMedName.getText().trim();
        if (name.isEmpty()) { showError(lblMedMessage, "药品名称不能为空"); return null; }
        Medicine m = new Medicine();
        m.setName(name);
        m.setSpec(txtMedSpec.getText().trim());
        m.setUnit(txtMedUnit.getText().trim());
        try {
            String priceStr = txtMedPrice.getText().trim();
            m.setPrice(priceStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(priceStr));
        } catch (NumberFormatException e) { showError(lblMedMessage, "零售价格式错误"); return null; }
        m.setSafetyStock(spinSafetyStock.getValue());
        m.setExpiryDate(dpMedExpiry.getValue());
        m.setBatchNo(txtMedBatch.getText().trim());
        m.setPinyinCode(txtMedPinyin.getText().trim());
        m.setManufacturer(txtMedManufacturer.getText().trim());
        m.setStatus(cmbMedStatus.getSelectionModel().getSelectedIndex());
        return m;
    }

    private void clearMedicineForm() {
        editingMedicine = null;
        txtMedName.clear(); txtMedSpec.clear(); txtMedUnit.clear(); txtMedPrice.clear();
        spinSafetyStock.getValueFactory().setValue(10);
        lblCurrentStock.setText("0");
        dpMedExpiry.setValue(null); txtMedBatch.clear(); txtMedPinyin.clear(); txtMedManufacturer.clear();
        cmbMedStatus.getSelectionModel().select(1);
        lblMedMessage.setText("");
        tableMedicines.getSelectionModel().clearSelection();
        logList.clear();
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
