package com.restaurant.inventory.controller;

import com.restaurant.inventory.Main;
import com.restaurant.inventory.model.Role;
import com.restaurant.inventory.model.User;
import com.restaurant.inventory.service.UserService;
import com.restaurant.inventory.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Controller for the Login and Registration View.
 * Handles role-based authentication (Admin vs Standard User) and input validation warnings.
 */
public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordPlainField;
    @FXML private Button showPasswordBtn;

    // Warning banner
    @FXML private HBox warningBanner;
    @FXML private Label warningLabel;

    // Registration expandable panel
    @FXML private VBox registerPane;
    @FXML private TextField regUsernameField;
    @FXML private TextField regFullNameField;
    @FXML private PasswordField regPasswordField;
    @FXML private ComboBox<String> regRoleCombo;
    @FXML private Label regMessageLabel;

    private final UserService userService = UserService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Sync plain text password with hidden password
        passwordPlainField.textProperty().bindBidirectional(passwordField.textProperty());

        // Role combo options
        regRoleCombo.getItems().addAll("Standard User", "Administrator");
        regRoleCombo.setValue("Standard User");

        // Clear warning styles when typing
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            usernameField.setStyle("");
            hideWarning();
        });
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            passwordField.setStyle("");
            hideWarning();
        });

        // Enter key to login
        usernameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) passwordField.requestFocus();
        });
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) onLogin();
        });
        passwordPlainField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) onLogin();
        });
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // 1. Validate empty inputs with prominent warning messages
        if (username.isEmpty()) {
            showWarning("Warning: Username cannot be empty. Please enter your username.");
            usernameField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5px;");
            usernameField.requestFocus();
            AlertUtil.warning("Login Input Warning", "Please enter your username to proceed.");
            return;
        }

        if (password.isEmpty()) {
            showWarning("Warning: Password cannot be empty. Please enter your password.");
            passwordField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5px;");
            passwordField.requestFocus();
            AlertUtil.warning("Login Input Warning", "Please enter your password to proceed.");
            return;
        }

        // 2. Authenticate
        UserService.AuthResult result = userService.authenticate(username, password);
        if (!result.success()) {
            showWarning("Warning: " + result.message());
            if (result.message().toLowerCase().contains("not found") || result.message().toLowerCase().contains("no user")) {
                usernameField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5px;");
            } else {
                passwordField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5px;");
            }
            AlertUtil.warning("Authentication Failed", result.message());
            return;
        }

        // 3. Successful login
        hideWarning();
        Main.showMainView();
    }

    @FXML
    private void onTogglePassword() {
        boolean show = !passwordPlainField.isVisible();
        passwordPlainField.setVisible(show);
        passwordPlainField.setManaged(show);
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
        showPasswordBtn.setText(show ? "Hide" : "Show");
    }

    @FXML
    private void onFillAdmin() {
        usernameField.setText("admin");
        passwordField.setText("admin123");
        hideWarning();
        onLogin();
    }

    @FXML
    private void onFillUser() {
        usernameField.setText("user");
        passwordField.setText("user123");
        hideWarning();
        onLogin();
    }

    @FXML
    private void onToggleRegister() {
        boolean isVisible = !registerPane.isVisible();
        registerPane.setVisible(isVisible);
        registerPane.setManaged(isVisible);
        if (isVisible) {
            regMessageLabel.setText("");
        }
    }

    @FXML
    private void onRegister() {
        String u = regUsernameField.getText().trim();
        String name = regFullNameField.getText().trim();
        String pass = regPasswordField.getText();
        String roleStr = regRoleCombo.getValue();
        Role role = "Administrator".equalsIgnoreCase(roleStr) ? Role.ADMIN : Role.USER;

        if (u.isEmpty() || name.isEmpty() || pass.isEmpty()) {
            regMessageLabel.setStyle("-fx-text-fill: #ef4444;");
            regMessageLabel.setText("Please fill out all registration fields.");
            return;
        }

        UserService.AuthResult res = userService.register(
                u, pass, name, role,
                u + "@example.com", "", "United States",
                LocalDate.of(1995, 1, 1), "Other", "Registered member", "salad.png"
        );

        if (res.success()) {
            regMessageLabel.setStyle("-fx-text-fill: #16a34a;");
            regMessageLabel.setText("Account created! You can now log in.");
            usernameField.setText(u);
            passwordField.setText(pass);
            registerPane.setVisible(false);
            registerPane.setManaged(false);
            AlertUtil.info("Registration Success", "Account created successfully for " + name + "! Logging you in...");
            onLogin();
        } else {
            regMessageLabel.setStyle("-fx-text-fill: #ef4444;");
            regMessageLabel.setText(res.message());
            AlertUtil.warning("Registration Warning", res.message());
        }
    }

    @FXML
    private void onDismissWarning() {
        hideWarning();
    }

    private void showWarning(String message) {
        warningLabel.setText(message);
        warningBanner.setVisible(true);
        warningBanner.setManaged(true);
    }

    private void hideWarning() {
        warningBanner.setVisible(false);
        warningBanner.setManaged(false);
    }
}
