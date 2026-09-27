package com.restaurant.inventory.controller;

import com.restaurant.inventory.model.Person;
import com.restaurant.inventory.service.InventoryService;
import com.restaurant.inventory.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
 * "Staff" tab: a registration form + a table of staff members (Person model).
 * Topics: TableView + ObservableList + Person model, RadioButton (gender),
 *         ToggleGroup (skill level), ComboBox (country), DatePicker,
 *         FileChooser + ImageView (photo displayed in table).
 */
public class StaffController implements Initializable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    // ---- form controls
    @FXML private TextField nameField;
    @FXML private ToggleGroup genderGroup;
    @FXML private ToggleGroup skillGroup;
    @FXML private RadioButton beginnerRadio;
    @FXML private Label genderLabel;
    @FXML private ComboBox<String> countryCombo;
    @FXML private DatePicker dobPicker;
    @FXML private Label dobLabel;
    @FXML private ImageView photoView;
    @FXML private Label photoNameLabel;
    @FXML private Label formMessageLabel;

    // ---- table
    @FXML private TableView<Person> staffTable;
    @FXML private TableColumn<Person, String> nameCol;
    @FXML private TableColumn<Person, String> genderCol;
    @FXML private TableColumn<Person, String> skillCol;
    @FXML private TableColumn<Person, String> countryCol;
    @FXML private TableColumn<Person, String> dobCol;
    @FXML private TableColumn<Person, String> photoCol;

    private final InventoryService service = InventoryService.getInstance();
    private String selectedPhotoUri;
    private String selectedPhotoName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupGender();
        setupSkillLevel();
        setupCountries();
        setupDatePicker();

        photoNameLabel.setText("No photo chosen");
    }

    // ================================================================= TABLEVIEW + Person

    private void setupTable() {
        staffTable.setItems(service.getStaff());

        nameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
        genderCol.setCellValueFactory(cell -> cell.getValue().genderProperty());
        skillCol.setCellValueFactory(cell -> cell.getValue().skillLevelProperty());
        countryCol.setCellValueFactory(cell -> cell.getValue().countryProperty());
        dobCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getDateOfBirth();
            return new SimpleStringProperty(date == null ? "" : date.format(DATE_FORMAT));
        });

        // Photo column: show actual ImageView with the photo
        photoCol.setCellValueFactory(cell -> cell.getValue().photoFileProperty());
        photoCol.setCellFactory(col -> new TableCell<Person, String>() {
            private final ImageView imgView = new ImageView();
            private final javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(48, 48);

            {
                imgView.setFitWidth(48);
                imgView.setFitHeight(48);
                imgView.setPreserveRatio(true);
                imgView.setSmooth(true);
                clip.setArcWidth(16);
                clip.setArcHeight(16);
                imgView.setClip(clip);
                setAlignment(javafx.geometry.Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String photoUri, boolean empty) {
                super.updateItem(photoUri, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    imgView.setImage(null);
                    setGraphic(null);
                } else {
                    Image img = resolveStaffPhoto(photoUri);
                    imgView.setImage(img);
                    setGraphic(imgView);
                }
            }
        });
    }

    @FXML
    private void onRemoveStaff() {
        Person selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Nothing selected", "Click a row in the table first.");
            return;
        }
        service.getStaff().remove(selected);
    }

    // ================================================================= RADIO BUTTONS

    private void setupGender() {
        genderLabel.setText("Selected: (none)");
        genderGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null) {
                genderLabel.setText("Selected: (none)");
            } else {
                genderLabel.setText("Selected: " + ((RadioButton) newToggle).getText());
            }
        });
    }

    private void setupSkillLevel() {
        beginnerRadio.setSelected(true);
    }

    // ================================================================= COMBOBOX

    private void setupCountries() {
        List<String> countries = Arrays.stream(Locale.getISOCountries())
                .map(code -> new Locale("", code).getDisplayCountry(Locale.ENGLISH))
                .filter(name -> !name.isBlank())
                .sorted()
                .collect(Collectors.toList());
        countryCombo.setItems(FXCollections.observableArrayList(countries));
        countryCombo.setVisibleRowCount(10);
    }

    // ================================================================= DATEPICKER

    private void setupDatePicker() {
        dobLabel.setText("Date of birth: (not selected)");

        dobPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });

        dobPicker.valueProperty().addListener((observable, oldDate, newDate) ->
                dobLabel.setText(newDate == null
                        ? "Date of birth: (not selected)"
                        : "Date of birth: " + newDate.format(DATE_FORMAT)));
    }

    // ================================================================= FILECHOOSER + IMAGEVIEW

    @FXML
    private void onBrowsePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a photo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));

        File file = chooser.showOpenDialog(photoView.getScene().getWindow());
        if (file != null) {
            selectedPhotoUri = file.toURI().toString();
            selectedPhotoName = file.getName();
            photoView.setImage(new Image(selectedPhotoUri, 78, 78, true, true));
            photoNameLabel.setText(file.getName());
        }
    }

    // ================================================================= SUBMIT

    @FXML
    private void onSubmit() {
        String name = nameField.getText().trim();
        Toggle gender = genderGroup.getSelectedToggle();
        Toggle skill = skillGroup.getSelectedToggle();
        String country = countryCombo.getValue();
        LocalDate dob = dobPicker.getValue();

        if (name.isEmpty()) {
            AlertUtil.warning("Missing name", "Please type the staff member's name.");
            return;
        }
        if (gender == null) {
            AlertUtil.warning("Missing gender", "Please choose a gender.");
            return;
        }
        if (country == null) {
            AlertUtil.warning("Missing country", "Please choose a country.");
            return;
        }
        if (dob == null) {
            AlertUtil.warning("Missing date of birth", "Please pick a date of birth.");
            return;
        }

        // Store the URI (not just the file name) so the table can load the image
        String photoUriForTable = selectedPhotoUri != null ? selectedPhotoUri : "-";

        Person person = new Person(
                name,
                ((RadioButton) gender).getText(),
                ((RadioButton) skill).getText(),
                country,
                dob,
                "",  // hobbies removed
                photoUriForTable);
        service.getStaff().add(person);

        formMessageLabel.setStyle("-fx-text-fill: #15803d;");
        formMessageLabel.setText("✓ Staff member \"" + name + "\" added successfully.");
        clearForm();
    }

    private void clearForm() {
        nameField.clear();
        genderGroup.selectToggle(null);
        beginnerRadio.setSelected(true);
        countryCombo.setValue(null);
        dobPicker.setValue(null);
        photoView.setImage(null);
        photoNameLabel.setText("No photo chosen");
        selectedPhotoUri = null;
        selectedPhotoName = null;
    }

    private Image resolveStaffPhoto(String photoUri) {
        if (photoUri == null || photoUri.isBlank() || "-".equals(photoUri)) {
            URL def = getClass().getResource("/images/avatar.png");
            return def != null ? new Image(def.toExternalForm(), 48, 48, true, true) : null;
        }
        try {
            if (photoUri.startsWith("file:") || photoUri.startsWith("http:") || photoUri.startsWith("https:")) {
                return new Image(photoUri, 48, 48, true, true);
            }
            File f = new File(photoUri);
            if (f.exists() && f.isFile()) {
                return new Image(f.toURI().toString(), 48, 48, true, true);
            }
            URL res = getClass().getResource(photoUri.startsWith("/") ? photoUri : "/images/" + photoUri);
            if (res != null) {
                return new Image(res.toExternalForm(), 48, 48, true, true);
            }
        } catch (Exception ignored) {
        }
        URL def = getClass().getResource("/images/avatar.png");
        return def != null ? new Image(def.toExternalForm(), 48, 48, true, true) : null;
    }
}
