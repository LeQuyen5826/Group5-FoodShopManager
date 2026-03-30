package com.foodshop;

import com.foodshop.config.HibernateUtil;
import com.foodshop.service.AuthService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        try {
            // Khởi tạo Hibernate (kết nối DB)
            HibernateUtil.getSessionFactory();

            // Tạo admin mặc định nếu chưa có
            AuthService.getInstance().createDefaultAdminIfNotExists();

            // Chuyển sang màn hình đăng nhập
            showLoginScreen();

        } catch (Exception e) {
            System.err.println("❌ Lỗi khởi động ứng dụng: " + e.getMessage());
            e.printStackTrace();
            showDatabaseError(e.getMessage());
        }
    }

    /**
     * Hiển thị màn hình đăng nhập
     */
    public static void showLoginScreen() {
        try {
            Parent root = FXMLLoader.load(
                Objects.requireNonNull(MainApp.class.getResource("/com/foodshop/fxml/LoginView.fxml"))
            );
            Scene scene = new Scene(root, 500, 400);
            scene.getStylesheets().add(
                Objects.requireNonNull(MainApp.class.getResource("/com/foodshop/css/style.css")).toExternalForm()
            );
            primaryStage.setTitle("🍽️ FoodShop Manager - Đăng nhập");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Không tải được màn hình đăng nhập: " + e.getMessage(), e);
        }
    }

    /**
     * Hiển thị màn hình chính sau khi đăng nhập
     */
    public static void showMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(MainApp.class.getResource("/com/foodshop/fxml/MainView.fxml"))
            );
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(
                Objects.requireNonNull(MainApp.class.getResource("/com/foodshop/css/style.css")).toExternalForm()
            );
            primaryStage.setTitle("🍽️ FoodShop Manager - Quản lý Shop Online");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(700);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Không tải được màn hình chính: " + e.getMessage(), e);
        }
    }

    /**
     * Tải một FXML view vào vùng nội dung chính
     */
    public static Parent loadView(String fxmlName) throws IOException {
        String path = "/com/foodshop/fxml/" + fxmlName;
        URL url = MainApp.class.getResource(path);
        if (url == null) throw new IOException("Không tìm thấy file FXML: " + path);
        return FXMLLoader.load(url);
    }

    /**
     * Hiển thị thông báo lỗi kết nối DB
     */
    private void showDatabaseError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Lỗi kết nối Database");
        alert.setHeaderText("Không thể kết nối đến MySQL!");
        alert.setContentText(
            "Hãy kiểm tra:\n" +
            "1. MySQL Server đang chạy (Apache/XAMPP)\n" +
            "2. Thông tin kết nối trong hibernate.cfg.xml\n" +
            "3. Database 'foodshop_db' đã tồn tại\n\n" +
            "Chi tiết: " + message
        );
        alert.showAndWait();
        System.exit(1);
    }

    @Override
    public void stop() {
        // Đóng Hibernate khi thoát app
        HibernateUtil.shutdown();
        System.out.println("Ứng dụng đã đóng.");
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) {
        launch(args);
    }
}
