package com.medicare.controller;

import com.medicare.model.Medicine;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 药品新增/编辑弹窗控制器
 */
public class MedicineDialogController {

    @FXML private TextField txtName;
    @FXML private TextField txtSpec;
    @FXML private TextField txtUnit;
    @FXML private TextField txtPrice;
    @FXML private Spinner<Integer> spinSafetyStock;
    @FXML private DatePicker dpExpiry;
    @FXML private TextField txtBatchNo;
    @FXML private TextField txtPinyin;
    @FXML private TextField txtManufacturer;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private Label lblMessage;

    private Medicine editingMedicine = null;

    @FXML
    public void initialize() {
        cmbStatus.setItems(FXCollections.observableArrayList("停用", "启用"));
        cmbStatus.getSelectionModel().select(1);
        spinSafetyStock.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9999, 10));
    }

    /**
     * 预填充数据（编辑模式）
     */
    public void setMedicine(Medicine medicine) {
        this.editingMedicine = medicine;
        if (medicine != null) {
            txtName.setText(medicine.getName());
            txtSpec.setText(medicine.getSpec());
            txtUnit.setText(medicine.getUnit());
            txtPrice.setText(medicine.getPrice() != null ? medicine.getPrice().toString() : "");
            spinSafetyStock.getValueFactory().setValue(medicine.getSafetyStock() != null ? medicine.getSafetyStock() : 10);
            dpExpiry.setValue(medicine.getExpiryDate());
            txtBatchNo.setText(medicine.getBatchNo());
            txtPinyin.setText(medicine.getPinyinCode());
            txtManufacturer.setText(medicine.getManufacturer());
            cmbStatus.getSelectionModel().select(medicine.getStatus() != null && medicine.getStatus() == 1 ? 1 : 0);
        }
    }

    /**
     * 获取表单数据，校验失败返回 null
     */
    public Medicine getMedicine() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            showError("药品名称不能为空");
            return null;
        }

        BigDecimal price;
        try {
            String priceStr = txtPrice.getText().trim();
            price = priceStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(priceStr);
        } catch (NumberFormatException e) {
            showError("零售价格式错误");
            return null;
        }

        Medicine m = new Medicine();
        if (editingMedicine != null) {
            m.setId(editingMedicine.getId());
            m.setStock(editingMedicine.getStock());
        }
        m.setName(name);
        m.setSpec(txtSpec.getText().trim());
        m.setUnit(txtUnit.getText().trim());
        m.setPrice(price);
        m.setSafetyStock(spinSafetyStock.getValue());
        m.setExpiryDate(dpExpiry.getValue());
        m.setBatchNo(txtBatchNo.getText().trim());
        m.setPinyinCode(txtPinyin.getText().trim());
        m.setManufacturer(txtManufacturer.getText().trim());
        m.setStatus(cmbStatus.getSelectionModel().getSelectedIndex());
        return m;
    }

    private void showError(String msg) {
        lblMessage.setStyle("-fx-text-fill: #c0392b;");
        lblMessage.setText(msg);
    }
}
