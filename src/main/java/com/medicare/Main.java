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
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("启动 MediCare 系统...");

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml"));
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("MediCare 智慧医疗门诊管理系统 v1.0");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();

        logger.info("MediCare 主界面已加载");
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
