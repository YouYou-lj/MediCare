package com.medicare.controller;

import com.medicare.model.Medicine;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;

/**
 * 出入库弹窗控制器
 */
public class StockDialogController {

    @FXML private TextField txtMedName;
    @FXML private TextField txtCurrentStock;
    @FXML private ComboBox<String> cmbOpType;
    @FXML private Spinner<Integer> spinQty;
    @FXML private TextField txtBatchNo;
    @FXML private DatePicker dpExpiry;
    @FXML private TextField txtOperator;
    @FXML private TextField txtRemark;
    @FXML private Label lblMessage;

    private Medicine medicine = null;
    private boolean isIn = true;

    @FXML
    public void initialize() {
        cmbOpType.setItems(FXCollections.observableArrayList("入库", "出库"));
        cmbOpType.getSelectionModel().select(0);
        spinQty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));

        cmbOpType.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            isIn = "入库".equals(newVal);
            updateFieldState();
        });
    }

    /**
     * 传入当前药品信息
     */
    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
        if (medicine != null) {
            txtMedName.setText(medicine.getName());
            txtCurrentStock.setText(String.valueOf(medicine.getStock() != null ? medicine.getStock() : 0));
            txtBatchNo.setText(medicine.getBatchNo());
            dpExpiry.setValue(medicine.getExpiryDate());
        }
        updateFieldState();
    }

    private void updateFieldState() {
        if (isIn) {
            txtBatchNo.setDisable(false);
            dpExpiry.setDisable(false);
        } else {
            txtBatchNo.setDisable(true);
            dpExpiry.setDisable(true);
        }
    }

    public boolean isIn() {
        return isIn;
    }

    public int getQuantity() {
        return spinQty.getValue();
    }

    public String getBatchNo() {
        return txtBatchNo.getText().trim();
    }

    public LocalDate getExpiryDate() {
        return dpExpiry.getValue();
    }

    public String getOperator() {
        return txtOperator.getText().trim();
    }

    public String getRemark() {
        return txtRemark.getText().trim();
    }

    public Medicine getMedicine() {
        return medicine;
    }

    /**
     * 校验表单，失败返回 false 并显示错误
     */
    public boolean validate() {
        int qty = spinQty.getValue();
        if (qty <= 0) {
            showError("数量必须大于 0");
            return false;
        }
        if (!isIn && medicine != null && medicine.getStock() != null && qty > medicine.getStock()) {
            showError("库存不足，当前库存: " + medicine.getStock());
            return false;
        }
        if (isIn && txtBatchNo.getText().trim().isEmpty()) {
            showError("入库时批号不能为空");
            return false;
        }
        return true;
    }

    private void showError(String msg) {
        lblMessage.setStyle("-fx-text-fill: #c0392b;");
        lblMessage.setText(msg);
    }
}
