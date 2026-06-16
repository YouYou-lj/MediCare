package com.medicare.controller;

import com.medicare.model.SysUser;
import com.medicare.service.SysUserService;
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
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 系统设置控制器
 * 管理员管理（弹窗新增/编辑）+ 密码修改
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class SettingsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final SysUserService sysUserService = new SysUserService();
    private final ObservableList<SysUser> adminList = FXCollections.observableArrayList();

    // ========== 管理员管理 Tab ==========
    @FXML private TextField txtAdminSearch;
    @FXML private TableView<SysUser> tableAdmins;
    @FXML private TableColumn<SysUser, Long> colAdminId;
    @FXML private TableColumn<SysUser, String> colAdminUsername, colAdminRealName;
    @FXML private TableColumn<SysUser, LocalDateTime> colAdminCreateTime;
    @FXML private TableColumn<SysUser, Integer> colAdminStatus;
    @FXML private TableColumn<SysUser, Void> colAdminAction;
    @FXML private Label lblAdminCount, lblAdminMessage;

    // ========== 修改密码 Tab ==========
    @FXML private PasswordField txtOldPassword, txtNewPassword, txtConfirmPassword;
    @FXML private Label lblPwdMessage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("系统设置界面初始化");
        initAdminTable();
        loadAdminData();
    }

    // ============================================================
    // 管理员表格初始化（含操作按钮列）
    // ============================================================

    private void initAdminTable() {
        colAdminId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAdminUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colAdminRealName.setCellValueFactory(new PropertyValueFactory<>("realName"));
        colAdminStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAdminStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : (s == 1 ? "启用" : "停用"));
            }
        });
        colAdminCreateTime.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        colAdminCreateTime.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText(empty || dt == null ? null : dt.format(fmt));
            }
        });

        // 操作列：编辑 + 删除按钮
        colAdminAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("编辑");
            private final Button btnDel = new Button("删除");
            private final HBox box = new HBox(5, btnEdit, btnDel);

            {
                btnEdit.setStyle("-fx-font-size: 11px; -fx-padding: 3 10;");
                btnDel.setStyle("-fx-font-size: 11px; -fx-padding: 3 10; -fx-text-fill: #c0392b;");
                btnEdit.setOnAction(e -> onEdit(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableAdmins.setItems(adminList);
    }

    // ============================================================
    // 数据加载
    // ============================================================

    private void loadAdminData() {
        Task<List<SysUser>> task = new Task<>() {
            @Override protected List<SysUser> call() throws Exception { return sysUserService.listAllAdmin(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            adminList.setAll(task.getValue());
            lblAdminCount.setText("共 " + adminList.size() + " 条");
        }));
        task.setOnFailed(e -> logger.error("加载管理员列表失败", task.getException()));
        new Thread(task).start();
    }

    // ============================================================
    // 搜索
    // ============================================================

    @FXML private void handleAdminSearch() {
        String kw = txtAdminSearch.getText();
        Task<List<SysUser>> task = new Task<>() {
            @Override protected List<SysUser> call() throws Exception { return sysUserService.searchAdmin(kw); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            adminList.setAll(task.getValue());
            lblAdminCount.setText("共 " + adminList.size() + " 条");
        }));
        new Thread(task).start();
    }

    // ============================================================
    // 新增管理员（弹窗）
    // ============================================================

    @FXML private void handleAdminAdd() {
        openAdminDialog(null, "新增管理员");
    }

    private void onEdit(SysUser user) {
        openAdminDialog(user, "编辑管理员");
    }

    private void openAdminDialog(SysUser user, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDialog.fxml"));
            DialogPane pane = loader.load();
            AdminDialogController controller = loader.getController();
            controller.setUser(user);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle(title);

            // 自定义 OK 按钮行为
            final Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.setText("保存");
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                SysUser newUser = controller.getUser();
                if (newUser == null) {
                    event.consume(); // 阻止关闭
                    return;
                }
                event.consume(); // 先阻止关闭，等后台任务完成
                okButton.setDisable(true);
                saveAdmin(newUser, dialog, okButton);
            });

            dialog.showAndWait();
        } catch (IOException e) {
            logger.error("打开弹窗失败", e);
            showAdminError("打开弹窗失败");
        }
    }

    private void saveAdmin(SysUser user, Dialog<ButtonType> dialog, Button okButton) {
        if (user.getId() != null) {
            // 编辑
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception { sysUserService.updateAdmin(user); return null; }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadAdminData(); dialog.close(); showAdminInfo("更新成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> {
                okButton.setDisable(false); showAdminError(task.getException().getMessage());
            }));
            new Thread(task).start();
        } else {
            // 新增
            Task<Long> task = new Task<>() {
                @Override protected Long call() throws Exception { return sysUserService.addAdmin(user); }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadAdminData(); dialog.close(); showAdminInfo("新增成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> {
                okButton.setDisable(false); showAdminError(task.getException().getMessage());
            }));
            new Thread(task).start();
        }
    }

    // ============================================================
    // 删除管理员
    // ============================================================

    private void onDelete(SysUser user) {
        SysUser current = LoginController.getCurrentUser();
        if (current != null && current.getId().equals(user.getId())) {
            showAdminError("不能删除当前登录的账号"); return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除管理员");
        alert.setHeaderText(null);
        alert.setContentText("确定删除管理员 " + user.getUsername() + " 吗？");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws Exception { sysUserService.deleteAdmin(user.getId()); return null; }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    loadAdminData(); showAdminInfo("删除成功");
                }));
                task.setOnFailed(e -> Platform.runLater(() -> showAdminError(task.getException().getMessage())));
                new Thread(task).start();
            }
        });
    }

    // ============================================================
    // 密码修改
    // ============================================================

    @FXML private void handleChangePassword() {
        SysUser current = LoginController.getCurrentUser();
        if (current == null) { showPwdError("未获取到当前登录用户"); return; }

        String oldPwd = txtOldPassword.getText();
        String newPwd = txtNewPassword.getText();
        String confirmPwd = txtConfirmPassword.getText();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                sysUserService.changePassword(current.getId(), oldPwd, newPwd, confirmPwd);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            txtOldPassword.clear(); txtNewPassword.clear(); txtConfirmPassword.clear();
            showPwdInfo("密码修改成功");
        }));
        task.setOnFailed(e -> Platform.runLater(() -> showPwdError(task.getException().getMessage())));
        new Thread(task).start();
    }

    private void showAdminInfo(String msg) {
        lblAdminMessage.setStyle("-fx-text-fill: #27ae60;");
        lblAdminMessage.setText(msg);
    }

    private void showAdminError(String msg) {
        lblAdminMessage.setStyle("-fx-text-fill: #c0392b;");
        lblAdminMessage.setText(msg);
    }

    private void showPwdInfo(String msg) {
        lblPwdMessage.setStyle("-fx-text-fill: #27ae60;");
        lblPwdMessage.setText(msg);
    }

    private void showPwdError(String msg) {
        lblPwdMessage.setStyle("-fx-text-fill: #c0392b;");
        lblPwdMessage.setText(msg);
    }
}
