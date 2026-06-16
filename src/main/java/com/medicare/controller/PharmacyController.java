package com.medicare.controller;

import com.medicare.model.Medicine;
import com.medicare.service.MedicineService;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 药品库存管理控制器（表格为主 + 弹窗编辑）
 */
public class PharmacyController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(PharmacyController.class);

    private final MedicineService medicineService = new MedicineService();
    private final ObservableList<Medicine> medicineList = FXCollections.observableArrayList();

    // ========== 顶部工具栏 ==========
    @FXML private TextField txtSearch;
    @FXML private Label lblTotalMedicines, lblLowStock, lblNearExpiry;

    // ========== 药品列表表格 ==========
    @FXML private TableView<Medicine> tableMedicines;
    @FXML private TableColumn<Medicine, Long> colMedId;
    @FXML private TableColumn<Medicine, String> colMedName, colMedSpec, colMedUnit;
    @FXML private TableColumn<Medicine, Integer> colMedStock, colMedSafety;
    @FXML private TableColumn<Medicine, BigDecimal> colMedPrice;
    @FXML private TableColumn<Medicine, LocalDate> colMedExpiry;
    @FXML private TableColumn<Medicine, Integer> colMedStatus;
    @FXML private TableColumn<Medicine, Void> colMedAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("药品库存管理界面初始化");
        initMedicineTable();
        loadMedicineData();
        loadAlerts();
    }

    // ============================================================
    // 表格初始化（含操作按钮列）
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
        colMedPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : price.setScale(2, RoundingMode.HALF_UP).toString());
            }
        });

        colMedExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        colMedExpiry.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            @Override protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) { setText(null); setStyle(""); }
                else {
                    setText(date.format(fmt));
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

        // 操作列：编辑 + 删除 + 出入库
        colMedAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("编辑");
            private final Button btnDel = new Button("删除");
            private final Button btnStock = new Button("出入库");
            private final HBox box = new HBox(5, btnEdit, btnDel, btnStock);

            {
                btnEdit.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-text-fill: #2980b9;");
                btnDel.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-text-fill: #c0392b;");
                btnStock.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-text-fill: #27ae60;");
                btnEdit.setOnAction(e -> onEditMedicine(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> onDeleteMedicine(getTableView().getItems().get(getIndex())));
                btnStock.setOnAction(e -> onStockOp(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableMedicines.setItems(medicineList);
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
    // 新增药品（弹窗）
    // ============================================================

    @FXML private void handleNewMedicine() {
        openMedicineDialog(null, "新增药品");
    }

    private void onEditMedicine(Medicine medicine) {
        openMedicineDialog(medicine, "编辑药品");
    }

    private void openMedicineDialog(Medicine medicine, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MedicineDialog.fxml"));
            DialogPane pane = loader.load();
            MedicineDialogController controller = loader.getController();
            controller.setMedicine(medicine);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle(title);

            final Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.setText("保存");
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                Medicine m = controller.getMedicine();
                if (m == null) {
                    event.consume();
                    return;
                }
                event.consume();
                okButton.setDisable(true);
                saveMedicine(m, dialog, okButton);
            });

            dialog.showAndWait();
        } catch (IOException e) {
            logger.error("打开药品弹窗失败", e);
            showAlert(Alert.AlertType.ERROR, "打开弹窗失败: " + e.getMessage());
        }
    }

    private void saveMedicine(Medicine medicine, Dialog<ButtonType> dialog, Button okButton) {
        if (medicine.getId() != null) {
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    medicineService.updateMedicine(medicine);
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadMedicineData(); loadAlerts(); dialog.close();
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "药品更新成功"));
            }));
            task.setOnFailed(e -> Platform.runLater(() -> {
                okButton.setDisable(false);
                showAlert(Alert.AlertType.ERROR, task.getException().getMessage());
            }));
            new Thread(task).start();
        } else {
            Task<Long> task = new Task<>() {
                @Override protected Long call() throws Exception { return medicineService.addMedicine(medicine); }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadMedicineData(); loadAlerts(); dialog.close();
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "药品新增成功"));
            }));
            task.setOnFailed(e -> Platform.runLater(() -> {
                okButton.setDisable(false);
                showAlert(Alert.AlertType.ERROR, task.getException().getMessage());
            }));
            new Thread(task).start();
        }
    }

    // ============================================================
    // 删除药品
    // ============================================================

    private void onDeleteMedicine(Medicine medicine) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除药品");
        alert.setHeaderText(null);
        alert.setContentText("确定删除 " + medicine.getName() + " 吗？");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws Exception {
                        medicineService.deleteMedicine(medicine.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    loadMedicineData(); loadAlerts();
                    showAlert(Alert.AlertType.INFORMATION, "删除成功");
                }));
                task.setOnFailed(e -> Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, task.getException().getMessage())));
                new Thread(task).start();
            }
        });
    }

    // ============================================================
    // 出入库（TODO：待开发）
    // ============================================================

    private void onStockOp(Medicine medicine) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StockDialog.fxml"));
            DialogPane pane = loader.load();
            StockDialogController controller = loader.getController();
            controller.setMedicine(medicine);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("出入库操作");

            final Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.setText("执行");
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (!controller.validate()) {
                    event.consume();
                    return;
                }
                event.consume();
                okButton.setDisable(true);
                doStockOp(controller, dialog, okButton);
            });

            dialog.showAndWait();
        } catch (IOException e) {
            logger.error("打开出入库弹窗失败", e);
            showAlert(Alert.AlertType.ERROR, "打开弹窗失败: " + e.getMessage());
        }
    }

    private void doStockOp(StockDialogController controller, Dialog<ButtonType> dialog, Button okButton) {
        Medicine m = controller.getMedicine();
        boolean isIn = controller.isIn();
        int qty = controller.getQuantity();
        String batchNo = controller.getBatchNo();
        java.time.LocalDate expiry = controller.getExpiryDate();
        String operator = controller.getOperator();
        String remark = controller.getRemark();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                if (isIn) {
                    medicineService.stockIn(m.getId(), qty, batchNo, expiry, operator, remark);
                } else {
                    medicineService.stockOut(m.getId(), qty, operator, remark);
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            dialog.close();
            loadMedicineData();
            loadAlerts();
            Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION,
                    (isIn ? "入库" : "出库") + "成功: " + qty));
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            okButton.setDisable(false);
            showAlert(Alert.AlertType.ERROR, task.getException().getMessage());
        }));
        new Thread(task).start();
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
