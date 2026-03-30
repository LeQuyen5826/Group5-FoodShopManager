package com.foodshop.controller;

import com.foodshop.MainApp;
import com.foodshop.model.User;
import com.foodshop.service.AuthService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private Label lblCurrentUser;
    @FXML private Label lblUserRole;
    @FXML private Label lblPageTitle;
    @FXML private Label lblDateTime;
    @FXML private StackPane contentPane;

    @FXML private Button btnDashboard;
    @FXML private Button btnProducts;
    @FXML private Button btnOrders;
    @FXML private Button btnCustomers;
    @FXML private Button btnInventory;
    @FXML private Button btnPromotions;
    @FXML private Button btnReports;
    @FXML private Button btnUsers;

    private Button activeNavButton;
    private final AuthService auth = AuthService.getInstance();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = auth.getCurrentUser();

        if (user != null) {
            lblCurrentUser.setText(user.getFullName());
            if (lblUserRole != null) {
                lblUserRole.setText(user.getRole().toString());
                lblUserRole.setStyle(switch (user.getRole()) {
                    case ADMIN     -> "-fx-text-fill: #f59e0b; -fx-font-size: 10px;";
                    case STAFF     -> "-fx-text-fill: #34d399; -fx-font-size: 10px;";
                    case WAREHOUSE -> "-fx-text-fill: #60a5fa; -fx-font-size: 10px;";
                });
            }
        }

        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
            lblDateTime.setText(LocalDateTime.now().format(formatter))
        ));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        applyMenuPermissions();
        showDashboard();
    }

    private void applyMenuPermissions() {
        setMenuVisible(btnOrders,     auth.canManageOrders());
        setMenuVisible(btnCustomers,  auth.canManageCustomers());
        setMenuVisible(btnInventory,  auth.canManageInventory());
        setMenuVisible(btnPromotions, auth.canManageOrders()); // Admin + Staff
        setMenuVisible(btnReports,    auth.canViewReports());
        setMenuVisible(btnUsers,      auth.canManageUsers());
    }

    private void setMenuVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    @FXML public void showDashboard() {
        loadView("DashboardView.fxml", "📊 Dashboard", btnDashboard);
    }

    @FXML public void showProducts() {
        loadView("ProductView.fxml", "🥦 Quản lý sản phẩm", btnProducts);
    }

    @FXML public void showOrders() {
        if (!auth.canManageOrders()) { denyAccess(); return; }
        loadView("OrderView.fxml", "🛒 Quản lý đơn hàng", btnOrders);
    }

    @FXML public void showCustomers() {
        if (!auth.canManageCustomers()) { denyAccess(); return; }
        loadView("CustomerView.fxml", "👥 Quản lý khách hàng", btnCustomers);
    }

    @FXML public void showInventory() {
        if (!auth.canManageInventory()) { denyAccess(); return; }
        loadView("InventoryView.fxml", "📦 Quản lý kho hàng", btnInventory);
    }

    @FXML public void showPromotions() {
        if (!auth.canManageOrders()) { denyAccess(); return; }
        loadView("PromotionView.fxml", "🎁 Khuyến mãi & Tích điểm", btnPromotions);
    }

    @FXML public void showReports() {
        if (!auth.canViewReports()) { denyAccess(); return; }
        loadView("ReportView.fxml", "📈 Báo cáo doanh thu", btnReports);
    }

    @FXML public void showUsers() {
        if (!auth.canManageUsers()) { denyAccess(); return; }
        loadView("UserView.fxml", "⚙ Quản lý người dùng", btnUsers);
    }

    @FXML public void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận đăng xuất");
        confirm.setHeaderText("Bạn có muốn đăng xuất không?");
        confirm.setContentText("Mọi dữ liệu chưa lưu sẽ bị mất.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            auth.logout();
            MainApp.showLoginScreen();
        }
    }

    private void denyAccess() {
        showAlert("🚫 Bạn không có quyền truy cập chức năng này!\n" +
            "Vui lòng liên hệ Quản trị viên.", Alert.AlertType.WARNING);
    }

    private void loadView(String fxmlName, String title, Button navBtn) {
        try {
            Parent view = MainApp.loadView(fxmlName);
            contentPane.getChildren().setAll(view);
            lblPageTitle.setText(title);
            if (activeNavButton != null)
                activeNavButton.getStyleClass().remove("nav-btn-active");
            if (navBtn != null) {
                navBtn.getStyleClass().add("nav-btn-active");
                activeNavButton = navBtn;
            }
        } catch (Exception e) {
            showAlert("Không thể tải màn hình: " + fxmlName + "\n" + e.getMessage(),
                Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    public static void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Lỗi" :
                       type == Alert.AlertType.WARNING ? "Cảnh báo" : "Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
