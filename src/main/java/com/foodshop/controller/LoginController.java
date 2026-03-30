package com.foodshop.controller;

import com.foodshop.MainApp;
import com.foodshop.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnLogin;

    private final AuthService authService = AuthService.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(() -> txtUsername.requestFocus());

        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    @FXML
    private void handleLogin() {
        hideError();

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        btnLogin.setDisable(true);
        btnLogin.setText("Đang đăng nhập...");

        new Thread(() -> {
            try {
                authService.login(username, password);

                Platform.runLater(() -> {
                    MainApp.showMainScreen();
                });

            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    txtPassword.clear();
                    txtPassword.requestFocus();
                    resetButton();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi hệ thống: " + e.getMessage());
                    resetButton();
                });
            }
        }).start();
    }

    private void showError(String message) {
        lblError.setText("⚠ " + message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void resetButton() {
        btnLogin.setDisable(false);
        btnLogin.setText("Đăng nhập");
    }
}
