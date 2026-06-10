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
        loadContent("/fxml/DoctorWorkstationView.fxml");
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
        showPlaceholder("系统设置 - 开发中");
    }

    @FXML
    private void handleLogout() {
        logger.info("用户退出登录");
        // TODO: M2 阶段实现登录界面跳转
        System.exit(0);
    }
}
