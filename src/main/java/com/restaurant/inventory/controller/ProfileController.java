package com.restaurant.inventory.controller;

import com.restaurant.inventory.model.Role;
import com.restaurant.inventory.model.User;
import com.restaurant.inventory.service.UserService;
import com.restaurant.inventory.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for the Profile Management View.
 * Allows both Normal Users and Administrators to view and update their personal information,
 * photo/avatar, contact information, and security credentials.
 */
public class ProfileController implements Initializable {

    // Sidebar summary
    @FXML private ImageView avatarView;
    @FXML private Label displayNameLabel;
    @FXML private Label usernameDisplayLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private Label roleDescriptionLabel;

    // Form inputs
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> countryCombo;
    @FXML private DatePicker dobPicker;
    @FXML private ToggleGroup genderGroup;
    @FXML private RadioButton maleRadio;
    @FXML private RadioButton femaleRadio;
    @FXML private RadioButton otherRadio;
    @FXML private TextArea bioArea;

    // Password fields
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label profileMessageLabel;

    private final UserService userService = UserService.getInstance();
    private String pendingAvatarPath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCountries();
        loadUserProfile();

        // Listen for user changes in session
        userService.currentUserProperty().addListener((obs, oldUser, newUser) -> {
            if (newUser != null) {
                loadUserProfile();
            }
        });
    }

    private void setupCountries() {
        List<String> countries = Arrays.stream(Locale.getISOCountries())
                .map(code -> new Locale("", code).getDisplayCountry(Locale.ENGLISH))
                .filter(name -> !name.isBlank())
                .sorted()
                .collect(Collectors.toList());
        countryCombo.setItems(FXCollections.observableArrayList(countries));
    }

    public void loadUserProfile() {
        User user = userService.getCurrentUser();
        if (user == null) return;

        displayNameLabel.setText(user.getFullName());
        usernameDisplayLabel.setText("@" + user.getUsername());
        roleBadgeLabel.setText(user.getRole().getDisplayName().toUpperCase());

        if (user.getRole() == Role.ADMIN) {
            roleBadgeLabel.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6d28d9;");
            roleDescriptionLabel.setText("System Administrator with full access to menu images, stock management, and reports.");
        } else {
            roleBadgeLabel.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46;");
            roleDescriptionLabel.setText("Registered Customer / User with menu browsing, stock availability, and ordering access.");
        }

        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
        countryCombo.setValue(user.getCountry());
        dobPicker.setValue(user.getDateOfBirth());
        bioArea.setText(user.getBio());

        String gender = user.getGender();
        if ("Male".equalsIgnoreCase(gender)) {
            maleRadio.setSelected(true);
        } else if ("Female".equalsIgnoreCase(gender)) {
            femaleRadio.setSelected(true);
        } else {
            otherRadio.setSelected(true);
        }

        pendingAvatarPath = user.getAvatarPath();
        displayAvatar(pendingAvatarPath);

        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        profileMessageLabel.setText("");
    }

    @FXML
    private void onBrowseAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Profile Avatar / Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File file = chooser.showOpenDialog(avatarView.getScene().getWindow());
        if (file != null) {
            pendingAvatarPath = file.toURI().toString();
            displayAvatar(pendingAvatarPath);
            profileMessageLabel.setStyle("-fx-text-fill: #2563eb;");
            profileMessageLabel.setText("Photo selected. Press 'Save Profile Changes' to apply.");
        }
    }

    @FXML
    private void onRemoveAvatar() {
        pendingAvatarPath = "avatar.png";
        displayAvatar(pendingAvatarPath);
        profileMessageLabel.setStyle("-fx-text-fill: #2563eb;");
        profileMessageLabel.setText("Default avatar selected. Press 'Save Profile Changes' to apply.");
    }

    private void displayAvatar(String path) {
        if (path == null || path.isEmpty()) {
            path = "avatar.png";
        }
        try {
            if (path.startsWith("file:") || path.startsWith("http")) {
                avatarView.setImage(new Image(path, 120, 120, true, true));
                return;
            }
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                avatarView.setImage(new Image(f.toURI().toString(), 120, 120, true, true));
                return;
            }
            URL res = getClass().getResource("/images/" + path);
            if (res != null) {
                avatarView.setImage(new Image(res.toExternalForm(), 120, 120, true, true));
                return;
            }
        } catch (Exception ignored) {
        }
        // Fallback default
        URL fallback = getClass().getResource("/images/avatar.png");
        if (fallback != null) {
            avatarView.setImage(new Image(fallback.toExternalForm(), 120, 120, true, true));
        }
    }

    @FXML
    private void onSaveProfile() {
        User user = userService.getCurrentUser();
        if (user == null) return;

        String name = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String country = countryCombo.getValue();
        LocalDate dob = dobPicker.getValue();
        String bio = bioArea.getText().trim();

        Toggle selectedGender = genderGroup.getSelectedToggle();
        String gender = selectedGender != null ? ((RadioButton) selectedGender).getText() : "Other";

        // Validation
        if (name.isEmpty()) {
            profileMessageLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            profileMessageLabel.setText("Warning: Full Name cannot be empty.");
            AlertUtil.warning("Profile Warning", "Full name is required.");
            fullNameField.requestFocus();
            return;
        }

        if (!email.isEmpty() && (!email.contains("@") || !email.contains("."))) {
            profileMessageLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            profileMessageLabel.setText("Warning: Please enter a valid email address.");
            AlertUtil.warning("Profile Warning", "The email address format is invalid.");
            emailField.requestFocus();
            return;
        }

        // Password change handling
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (!newPass.isEmpty()) {
            if (!user.getPassword().equals(currentPass)) {
                profileMessageLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                profileMessageLabel.setText("Warning: Current password does not match.");
                AlertUtil.warning("Security Warning", "Current password verification failed.");
                currentPasswordField.requestFocus();
                return;
            }
            if (newPass.length() < 4) {
                profileMessageLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                profileMessageLabel.setText("Warning: New password must be at least 4 characters.");
                AlertUtil.warning("Security Warning", "New password is too short (min 4 characters).");
                newPasswordField.requestFocus();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                profileMessageLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                profileMessageLabel.setText("Warning: New password and confirmation do not match.");
                AlertUtil.warning("Security Warning", "New password and confirm password do not match.");
                confirmPasswordField.requestFocus();
                return;
            }
            user.setPassword(newPass);
        }

        // Apply profile updates
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setCountry(country == null ? "" : country);
        user.setDateOfBirth(dob);
        user.setGender(gender);
        user.setBio(bio);
        if (pendingAvatarPath != null) {
            user.setAvatarPath(pendingAvatarPath);
        }

        displayNameLabel.setText(name);

        profileMessageLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
        profileMessageLabel.setText("Profile updated successfully!");

        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();

        AlertUtil.info("Profile Updated", "Your personal information has been saved successfully!");
    }

    @FXML
    private void onResetForm() {
        loadUserProfile();
    }
}
