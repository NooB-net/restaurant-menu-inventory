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
 * Only administrators can create new accounts (password-protected action).
 * Users do not need a password — username alone is sufficient for login.
 */
public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordPlainField;
    @FXML private Button showPasswordBtn;

    // Warning banner
    @FXML private HBox warningBanner;
    @FXML private Label warningLabel;

    // Admin-only account creation panel
    @FXML private VBox adminCreateSection;
    @FXML private PasswordField adminAuthField;
    @FXML private TextField regUsernameField;
    @FXML private TextField regFullNameField;
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
            if (event.getCode() == KeyCode.ENTER) onLogin();
        });
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) onLogin();
        });
        passwordPlainField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) onLogin();
        });
    }

    /**
     * Login logic:
     * - Administrators require a password.
     * - Standard users log in with username only (no password required).
     */
    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            showWarning("Warning: Username cannot be empty. Please enter your username.");
            usernameField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5px;");
            usernameField.requestFocus();
            return;
        }

        // Find the user first
        User user = userService.findUser(username);

        if (user == null) {
            showWarning("Warning: No account found with username \"" + username + "\".");
            usernameField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5px;");
            usernameField.requestFocus();
            return;
        }

        // Admins require a password; users do not
        if (user.getRole() == Role.ADMIN) {
            String password = passwordField.getText();
            if (password.isEmpty()) {
                showWarning("Warning: Administrator account requires a password.");
                passwordField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5px;");
                passwordField.requestFocus();
                return;
            }
            UserService.AuthResult result = userService.authenticate(username, password);
            if (!result.success()) {
                showWarning("Warning: " + result.message());
                passwordField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5px;");
                return;
            }
        } else {
            // Standard user: login without password
            userService.setCurrentUser(user);
        }

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

    /** Show / hide the admin-only create account panel. */
    @FXML
    private void onToggleRegister() {
        boolean isVisible = !adminCreateSection.isVisible();
        adminCreateSection.setVisible(isVisible);
        adminCreateSection.setManaged(isVisible);
        if (isVisible) {
            regMessageLabel.setText("");
            adminAuthField.clear();
        }
    }

    @FXML
    private void onCancelCreate() {
        adminCreateSection.setVisible(false);
        adminCreateSection.setManaged(false);
        regMessageLabel.setText("");
    }

    /**
     * Admin creates a new account.
     * Requires admin password verification before creating any account.
     * The new user account does NOT need a password (users login with username only).
     */
    @FXML
    private void onRegister() {
        // 1. Verify admin password
        String adminPassword = adminAuthField.getText();
        User adminUser = userService.findUser("admin");
        if (adminUser == null || !adminUser.getPassword().equals(adminPassword)) {
            regMessageLabel.setStyle("-fx-text-fill: #ef4444;");
            regMessageLabel.setText("❌ Incorrect admin password. Only administrators can create accounts.");
            return;
        }

        String u = regUsernameField.getText().trim();
        String name = regFullNameField.getText().trim();
        String roleStr = regRoleCombo.getValue();
        Role role = "Administrator".equalsIgnoreCase(roleStr) ? Role.ADMIN : Role.USER;

        if (u.isEmpty() || name.isEmpty()) {
            regMessageLabel.setStyle("-fx-text-fill: #ef4444;");
            regMessageLabel.setText("Please fill out all fields.");
            return;
        }

        if (userService.findUser(u) != null) {
            regMessageLabel.setStyle("-fx-text-fill: #ef4444;");
            regMessageLabel.setText("Username \"" + u + "\" is already taken.");
            return;
        }

        // Admin accounts get a default password; user accounts get a blank password (no login password needed)
        String newPassword = (role == Role.ADMIN) ? "admin123" : "";

        UserService.AuthResult res = userService.register(
                u, newPassword, name, role,
                u + "@example.com", "", "United States",
                LocalDate.of(1995, 1, 1), "Other", "Member", "salad.png"
        );

        if (res.success()) {
            regMessageLabel.setStyle("-fx-text-fill: #16a34a;");
            regMessageLabel.setText("✓ Account created for " + name + "!");
            regUsernameField.clear();
            regFullNameField.clear();
            adminAuthField.clear();
            AlertUtil.info("Account Created", "Account created successfully for " + name + ".\n"
                    + (role == Role.ADMIN ? "Default admin password: admin123" : "User can log in with username only."));
        } else {
            regMessageLabel.setStyle("-fx-text-fill: #ef4444;");
            regMessageLabel.setText(res.message());
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
