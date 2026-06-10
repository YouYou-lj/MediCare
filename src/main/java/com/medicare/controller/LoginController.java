package com.medicare.controller;

import com.medicare.model.SysUser;
import com.medicare.service.SysUserService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 登录控制器
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final SysUserService sysUserService = new SysUserService();

    // 当前登录用户（静态，供全局访问）
    private static SysUser currentUser = null;

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblMessage;

    @FXML
    public void initialize() {
        logger.info("登录界面初始化");
        txtUsername.setText("admin");
        Platform.runLater(() -> txtUsername.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty()) {
            showError("请输入用户名");
            return;
        }
        if (password.isEmpty()) {
            showError("请输入密码");
            return;
        }

        Task<SysUser> task = new Task<>() {
            @Override
            protected SysUser call() throws Exception {
                return sysUserService.login(username, password);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            currentUser = task.getValue();
            showInfo("登录成功，正在进入系统...");
            openMainWindow();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            showError(ex.getMessage());
            txtPassword.clear();
            txtPassword.requestFocus();
        }));

        new Thread(task).start();
    }

    private void openMainWindow() {
        try {
            Stage currentStage = (Stage) txtUsername.getScene().getWindow();

            Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml"));
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            currentStage.setTitle("MediCare 智慧医疗门诊管理系统 v1.0");
            currentStage.setScene(scene);
            currentStage.setMinWidth(1000);
            currentStage.setMinHeight(700);
            currentStage.show();

            logger.info("主界面已打开，登录用户: {}", currentUser.getRealName());
        } catch (IOException e) {
            logger.error("打开主界面失败", e);
            showError("系统错误: " + e.getMessage());
        }
    }

    private void showInfo(String msg) {
        lblMessage.setStyle("-fx-text-fill: #27ae60;");
        lblMessage.setText(msg);
    }

    private void showError(String msg) {
        lblMessage.setStyle("-fx-text-fill: #c0392b;");
        lblMessage.setText(msg);
    }

    public static SysUser getCurrentUser() {
        return currentUser;
    }

    public static void clearCurrentUser() {
        currentUser = null;
    }
}
