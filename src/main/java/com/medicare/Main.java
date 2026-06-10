package com.medicare;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MediCare 智慧医疗门诊管理系统 - 主入口
 * 启动时先显示登录界面，验证成功后进入主界面
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("启动 MediCare 系统...");

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());

        primaryStage.setTitle("MediCare 智慧医疗门诊管理系统 - 登录");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        logger.info("登录界面已加载");
    }

    @Override
    public void stop() {
        logger.info("MediCare 系统正在关闭...");
        com.medicare.util.ConnectionConfig.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
