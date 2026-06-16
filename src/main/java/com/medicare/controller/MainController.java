package com.medicare.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 主界面控制器
 * 负责左侧导航切换和右侧内容区加载
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private StackPane contentPane;
    @FXML
    private Label lblCurrentUser;
    @FXML
    private Label lblTodayReg;
    @FXML
    private Label lblWaiting;
    @FXML
    private Label lblStockAlert;
    @FXML
    private Label lblStatus;

    @FXML
    public void initialize() {
        logger.info("主界面控制器初始化完成");
        refreshDashboard();
        // 显示当前登录用户
        com.medicare.model.SysUser user = LoginController.getCurrentUser();
        if (user != null) {
            lblCurrentUser.setText(user.getRealName() + " (" + user.getUsername() + ")");
        }
    }

    /**
     * 刷新仪表盘数据
     */
    private void refreshDashboard() {
        // TODO: M3 阶段接入 Service 层获取实时数据
        lblTodayReg.setText("0");
        lblWaiting.setText("0");
        lblStockAlert.setText("0");
    }

    /**
     * 加载内容区域
     */
    private void loadContent(String fxmlPath) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().clear();
            contentPane.getChildren().add(node);
            logger.info("加载界面: {}", fxmlPath);
        } catch (IOException e) {
            logger.error("加载界面失败: {}", fxmlPath, e);
            showPlaceholder("界面加载失败: " + fxmlPath);
        }
    }

    /**
     * 显示占位提示
     */
    private void showPlaceholder(String message) {
        VBox placeholder = new VBox(new Label(message));
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        contentPane.getChildren().clear();
        contentPane.getChildren().add(placeholder);
    }

    // ==================== 导航事件处理 ====================

    @FXML
    private void showPatientMgmt() {
        lblStatus.setText("患者管理");
        loadContent("/fxml/PatientView.fxml");
    }

    @FXML
    private void showBasicData() {
        lblStatus.setText("基础数据");
        loadContent("/fxml/BasicDataView.fxml");
    }

    @FXML
    private void showRegistration() {
        lblStatus.setText("挂号预约");
        loadContent("/fxml/RegistrationView.fxml");
    }

    @FXML
    private void showDoctorWorkstation() {
        lblStatus.setText("医生工作站");
        loadContent("/fxml/DoctorWorkstationView.fxml");
    }

    @FXML
    private void showMedicalRecord() {
        lblStatus.setText("病历管理");
        loadContent("/fxml/MedicalRecordView.fxml");
    }

    @FXML
    private void showPharmacy() {
        lblStatus.setText("药品库存");
        loadContent("/fxml/PharmacyView.fxml");
    }

    @FXML
    private void showPrescription() {
        lblStatus.setText("处方管理");
        loadContent("/fxml/PrescriptionView.fxml");
    }

    @FXML
    private void showSettings() {
        lblStatus.setText("系统设置");
        loadContent("/fxml/SettingsView.fxml");
    }

    @FXML
    private void handleLogout() {
        logger.info("用户退出登录");
        LoginController.clearCurrentUser();
        try {
            javafx.scene.Node source = (javafx.scene.Node) lblStatus.getScene().getRoot();
            javafx.stage.Stage stage = (javafx.stage.Stage) source.getScene().getWindow();
            javafx.scene.Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());
            stage.setTitle("MediCare 智慧医疗门诊管理系统 - 登录");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            logger.error("返回登录界面失败", e);
            System.exit(0);
        }
    }
}
