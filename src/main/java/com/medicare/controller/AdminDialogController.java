package com.medicare.controller;

import com.medicare.model.SysUser;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * 管理员新增/编辑弹窗控制器
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class AdminDialogController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtRealName;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private Label lblMessage;

    private SysUser editingUser = null;

    @FXML
    public void initialize() {
        cmbStatus.setItems(FXCollections.observableArrayList("停用", "启用"));
        cmbStatus.getSelectionModel().select(1);
    }

    /**
     * 预填充数据（编辑模式）
     */
    public void setUser(SysUser user) {
        this.editingUser = user;
        if (user != null) {
            txtUsername.setText(user.getUsername());
            txtPassword.setText(user.getPassword());
            txtRealName.setText(user.getRealName());
            cmbStatus.getSelectionModel().select(user.getStatus() != null && user.getStatus() == 1 ? 1 : 0);
        }
    }

    public SysUser getUser() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String realName = txtRealName.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setStyle("-fx-text-fill: #c0392b;");
            lblMessage.setText("用户名和密码不能为空");
            return null;
        }

        SysUser user = new SysUser();
        if (editingUser != null) {
            user.setId(editingUser.getId());
        }
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setStatus(cmbStatus.getSelectionModel().getSelectedIndex());
        user.setRole(SysUser.ROLE_ADMIN);
        return user;
    }
}
