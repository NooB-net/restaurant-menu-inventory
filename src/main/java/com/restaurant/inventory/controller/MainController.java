package com.restaurant.inventory.controller;

import com.restaurant.inventory.Main;
import com.restaurant.inventory.model.Role;
import com.restaurant.inventory.model.User;
import com.restaurant.inventory.service.InventoryService;
import com.restaurant.inventory.service.UserService;
import com.restaurant.inventory.util.AlertUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller of the main application window.
 * Manages the top navigation bar, active user session, role-based tab access,
 * MenuBar actions, and application-wide stock status bar.
 */
public class MainController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;

    // Active User Header
    @FXML private ImageView userAvatarImage;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleBadge;

    // TabPane and individual tabs for role-based permissions
    @FXML private TabPane mainTabPane;
    @FXML private Tab menuTab;
    @FXML private Tab inventoryTab;
    @FXML private Tab staffTab;
    @FXML private Tab toolsTab;
    @FXML private Tab profileTab;

    private final InventoryService service = InventoryService.getInstance();
    private final UserService userService = UserService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"));
        welcomeLabel.setText("The Gourmet Kitchen  -  " + today);

        // Status bar always reflects out-of-stock items
        statusLabel.textProperty().bind(service.stockAlertProperty());

        // Update user session information in header and enforce role access
        updateUserHeader();
        configureRoleTabs();

        // Listen for profile changes so header reflects updated name/avatar in real time
        userService.currentUserProperty().addListener((obs, oldUser, newUser) -> {
            updateUserHeader();
            configureRoleTabs();
        });
    }

    /**
     * Requirement 3: Role-based navigation.
     * Normal users: Menu page + Profile page.
     * Administrators: Menu page + Ingredients & Purchases page + Staff + Tools + Profile.
     */
    private void configureRoleTabs() {
        User user = userService.getCurrentUser();
        if (user == null || mainTabPane == null) return;

        mainTabPane.getTabs().clear();

        if (user.getRole() == Role.ADMIN) {
            // Administrator has access to all pages
            mainTabPane.getTabs().addAll(menuTab, inventoryTab, staffTab, toolsTab, profileTab);
        } else {
            // Standard User has access to Menu page and Profile page
            mainTabPane.getTabs().addAll(menuTab, profileTab);
        }
    }

    private void updateUserHeader() {
        User user = userService.getCurrentUser();
        if (user == null) {
            userNameLabel.setText("Guest");
            userRoleBadge.setText("GUEST");
            return;
        }

        userNameLabel.setText(user.getFullName());
        userRoleBadge.setText(user.getRole() == Role.ADMIN ? "ADMINISTRATOR" : "CUSTOMER");

        if (user.getRole() == Role.ADMIN) {
            userRoleBadge.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6d28d9;");
        } else {
            userRoleBadge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46;");
        }

        displayAvatar(user.getAvatarPath());
    }

    private void displayAvatar(String path) {
        if (path == null || path.isEmpty()) {
            path = "avatar.png";
        }
        try {
            if (path.startsWith("file:") || path.startsWith("http")) {
                userAvatarImage.setImage(new Image(path, 34, 34, true, true));
                return;
            }
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                userAvatarImage.setImage(new Image(f.toURI().toString(), 34, 34, true, true));
                return;
            }
            URL res = getClass().getResource("/images/" + path);
            if (res != null) {
                userAvatarImage.setImage(new Image(res.toExternalForm(), 34, 34, true, true));
                return;
            }
        } catch (Exception ignored) {
        }
        URL fallback = getClass().getResource("/images/avatar.png");
        if (fallback != null) {
            userAvatarImage.setImage(new Image(fallback.toExternalForm(), 34, 34, true, true));
        }
    }

    // ---------------------------------------------------------------- Authentication / Logout actions

    @FXML
    private void onLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to sign out?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("Sign out of your account?");
        confirm.showAndWait()
                .filter(btn -> btn == ButtonType.YES)
                .ifPresent(btn -> {
                    userService.logout();
                    Main.showLoginView();
                });
    }

    @FXML
    private void onSwitchUser() {
        userService.logout();
        Main.showLoginView();
    }

    // ---------------------------------------------------------------- MenuBar actions

    /** File -> New : start a fresh day (stock back to default, history cleared). */
    @FXML
    private void onNew() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Reset all ingredient stock to the default levels and clear the order history?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("New Day");
        confirm.setHeaderText("Start a new day?");
        confirm.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .ifPresent(button -> service.resetToDefaults());
    }

    /** File -> Open : load stock levels from a CSV file (see sample-stock.csv). */
    @FXML
    private void onOpen() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open stock file (name,quantity,unit)");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV / Text files", "*.csv", "*.txt"));

        File file = chooser.showOpenDialog(welcomeLabel.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            int count = service.loadStockFromCsv(file);
            AlertUtil.info("Stock Loaded", count + " ingredient(s) loaded from " + file.getName());
        } catch (IOException e) {
            AlertUtil.error("Could Not Read File", e.getMessage());
        }
    }

    /** File -> Exit : close the application. */
    @FXML
    private void onExit() {
        Platform.exit();
    }
}
