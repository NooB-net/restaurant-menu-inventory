package com.restaurant.inventory.controller;

import com.restaurant.inventory.model.Dish;
import com.restaurant.inventory.model.Ingredient;
import com.restaurant.inventory.model.RecipeLine;
import com.restaurant.inventory.model.Role;
import com.restaurant.inventory.model.User;
import com.restaurant.inventory.service.InventoryService;
import com.restaurant.inventory.service.UserService;
import com.restaurant.inventory.util.AlertUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * "Menu & Orders" tab: visual menu catalog, availability checks, ordering,
 * and Administrator picture management (Add / Change / Remove pictures).
 */
public class OrderController implements Initializable {

    // ---- injected from OrderView.fxml
    @FXML private HBox quickBar;
    @FXML private HBox actionBar;
    @FXML private ListView<Dish> dishList;
    @FXML private ListView<String> historyList;
    @FXML private ImageView dishImage;
    @FXML private Label selectedDishLabel;
    @FXML private Label dishCategoryBadge;
    @FXML private Label orderSummaryLabel;
    @FXML private Label stockAlertLabel;
    @FXML private Spinner<Integer> qtySpinner;
    @FXML private TableView<RecipeLine> recipeTable;
    @FXML private TableColumn<RecipeLine, String> recIngredientCol;
    @FXML private TableColumn<RecipeLine, String> recNeededCol;
    @FXML private TableColumn<RecipeLine, String> recInStockCol;
    @FXML private TableColumn<RecipeLine, String> recStatusCol;

    // Filters
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private ComboBox<String> stockFilterCombo;

    // Warning Banner for selected dish
    @FXML private HBox dishWarningBanner;
    @FXML private Label dishWarningLabel;
    @FXML private Label servingsBadge;

    // Administrator only picture controls
    @FXML private VBox adminPictureControls;
    @FXML private Label adminBadgeLabel;

    private final InventoryService service = InventoryService.getInstance();
    private final UserService userService = UserService.getInstance();

    private FilteredList<Dish> filteredDishes;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ---------- Spinner: numeric quantity 1..10, start value 1
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        qtySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            recipeTable.refresh();
            updateDishAvailabilityWarning();
        });

        // Setup role-based access for Picture Management
        applyRolePermissions();
        userService.currentUserProperty().addListener((obs, oldUser, newUser) -> applyRolePermissions());

        // Setup filterable dish list
        setupDishList();

        historyList.setItems(service.getOrderHistory());

        setupRecipeTable();
        buildQuickSearchBar();
        buildActionButtons();

        // Bottom label always shows what is out of stock
        stockAlertLabel.textProperty().bind(service.stockAlertProperty());

        // Whenever stock changes, refresh lists and live warning status
        service.stockVersionProperty().addListener((observable, oldValue, newValue) -> {
            dishList.refresh();
            refreshSelectedLabel();
            updateDishAvailabilityWarning();
        });

        dishList.getSelectionModel().selectFirst();
    }

    /**
     * Requirement 5: Administrators can add or remove pictures of menu items.
     * Users can only view and select menu items, and check availability.
     */
    private void applyRolePermissions() {
        User current = userService.getCurrentUser();
        boolean isAdmin = current != null && current.getRole() == Role.ADMIN;

        if (adminPictureControls != null) {
            adminPictureControls.setVisible(isAdmin);
            adminPictureControls.setManaged(isAdmin);
        }
        if (adminBadgeLabel != null) {
            adminBadgeLabel.setVisible(isAdmin);
            adminBadgeLabel.setManaged(isAdmin);
        }
    }

    private void setupDishList() {
        filteredDishes = new FilteredList<>(service.getDishes(), p -> true);
        dishList.setItems(filteredDishes);

        // Custom cell displaying food thumbnail, name, price, and availability tag
        dishList.setCellFactory(listView -> new ListCell<>() {
            private final ImageView thumbnail = new ImageView();
            private final Label nameLabel = new Label();
            private final Label priceLabel = new Label();
            private final Label statusPill = new Label();
            private final HBox root = new HBox(8);

            {
                thumbnail.setFitWidth(42);
                thumbnail.setFitHeight(42);
                thumbnail.setPreserveRatio(true);
                thumbnail.getStyleClass().add("dish-thumb");

                nameLabel.getStyleClass().add("dish-list-name");
                priceLabel.getStyleClass().add("dish-list-price");
                statusPill.getStyleClass().add("status-pill-mini");

                VBox details = new VBox(2, nameLabel, priceLabel);
                root.getChildren().addAll(thumbnail, details, statusPill);
                root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Dish dish, boolean empty) {
                super.updateItem(dish, empty);
                if (empty || dish == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    nameLabel.setText(dish.getName());
                    priceLabel.setText(String.format("$%.2f • %s", dish.getPrice(), dish.getCategory()));

                    // Thumbnail image or placeholder
                    Image img = dish.getImage();
                    if (img != null) {
                        thumbnail.setImage(img);
                    } else {
                        thumbnail.setImage(loadResourceImage("placeholder.png"));
                    }

                    if (dish.isAvailable()) {
                        statusPill.setText("✓ In Stock (" + dish.maxServings() + ")");
                        statusPill.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d;");
                        setStyle("");
                    } else {
                        statusPill.setText("⚠ Out of Stock");
                        statusPill.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                        setStyle("-fx-background-color: #fff1f2;");
                    }
                    setText(null);
                    setGraphic(root);
                }
            }
        });

        dishList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldDish, newDish) -> showDish(newDish));

        // Category filter setup
        categoryFilterCombo.getItems().addAll("All Categories", "Starters", "Main Course", "Drinks", "Desserts");
        categoryFilterCombo.setValue("All Categories");
        categoryFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Stock filter setup
        stockFilterCombo.getItems().addAll("All Items", "Available Only", "Out of Stock Only");
        stockFilterCombo.setValue("All Items");
        stockFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        String cat = categoryFilterCombo.getValue();
        String stock = stockFilterCombo.getValue();

        filteredDishes.setPredicate(dish -> {
            boolean matchesCat = cat == null || "All Categories".equals(cat) || dish.getCategory().equalsIgnoreCase(cat);
            boolean matchesStock = true;
            if ("Available Only".equals(stock)) {
                matchesStock = dish.isAvailable();
            } else if ("Out of Stock Only".equals(stock)) {
                matchesStock = !dish.isAvailable();
            }
            return matchesCat && matchesStock;
        });

        if (!filteredDishes.isEmpty()) {
            dishList.getSelectionModel().selectFirst();
        } else {
            showDish(null);
        }
    }

    // ================================================================= EVENT HANDLING FROM CODE

    private void buildQuickSearchBar() {
        Label title = new Label("Quick find:");
        title.getStyleClass().add("field-label");

        TextField searchField = new TextField();
        searchField.setPromptText("Type dish name and press Enter");
        searchField.setPrefWidth(260);
        searchField.getStyleClass().add("modern-input");

        Label resultLabel = new Label();
        resultLabel.getStyleClass().add("info-label");

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String text = searchField.getText().trim().toLowerCase();
                if (text.isEmpty()) {
                    resultLabel.setText("Type something first.");
                    return;
                }
                Dish found = service.getDishes().stream()
                        .filter(dish -> dish.getName().toLowerCase().contains(text))
                        .findFirst()
                        .orElse(null);
                if (found == null) {
                    resultLabel.setText("No dish matches \"" + text + "\".");
                } else {
                    dishList.getSelectionModel().select(found);
                    dishList.scrollTo(found);
                    resultLabel.setText("Found: " + found.getName());
                }
            }
        });

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("secondary-btn");
        clearButton.setOnAction(event -> {
            searchField.clear();
            resultLabel.setText("");
            searchField.requestFocus();
        });

        quickBar.getChildren().addAll(title, searchField, clearButton, resultLabel);
    }

    private void buildActionButtons() {
        Button checkAvailabilityBtn = new Button("Check Availability");
        checkAvailabilityBtn.getStyleClass().add("secondary-btn");
        checkAvailabilityBtn.setOnAction(event -> checkAvailability());

        Button placeOrderButton = new Button("Place Order");
        placeOrderButton.getStyleClass().add("modern-primary-btn");
        placeOrderButton.setOnAction(event -> placeOrder());

        actionBar.getChildren().addAll(checkAvailabilityBtn, placeOrderButton);
    }

    // ================================================================= AVAILABILITY CHECK & ORDER LOGIC

    /**
     * Requirement 2 & 5: Check availability and trigger warning messages when ingredients are short.
     */
    @FXML
    private void checkAvailability() {
        Dish dish = dishList.getSelectionModel().getSelectedItem();
        if (dish == null) {
            AlertUtil.warning("No Selection", "Please select a dish from the menu to check.");
            return;
        }

        int quantity = currentQty();
        List<String> missing = dish.missingIngredients(quantity);

        if (!missing.isEmpty()) {
            AlertUtil.warning("Not Enough Ingredients Warning",
                    "⚠ INSUFFICIENT INGREDIENTS WARNING\n\nCannot prepare " + quantity + " x " + dish.getName() + ".\n"
                    + "The kitchen is short on the following ingredients:\n"
                    + "• " + String.join("\n• ", missing) + "\n\nPlease restock these ingredients or order a smaller quantity.");
        } else {
            AlertUtil.info("Ingredients Available",
                    "✓ Great news! All ingredients are available in stock.\n"
                    + "You can prepare " + quantity + " serving(s) of " + dish.getName() + ".\n"
                    + "(Maximum servings available: " + dish.maxServings() + ")");
        }
    }

    private void placeOrder() {
        Dish dish = dishList.getSelectionModel().getSelectedItem();
        if (dish == null) {
            AlertUtil.warning("No Dish Selected", "Please select a dish from the menu first.");
            return;
        }

        int quantity = currentQty();
        InventoryService.OrderResult result = service.placeOrder(dish, quantity);

        if (result.success()) {
            orderSummaryLabel.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
            orderSummaryLabel.setText(result.message());

            // Requirement 2: Show warning messages when an ingredient runs out completely
            if (!result.depletedIngredients().isEmpty()) {
                AlertUtil.warning("Ingredient Ran Out Warning",
                        "⚠ CRITICAL STOCK WARNING: Ingredient(s) depleted!\n\n"
                        + "The following ingredient(s) have completely run out (stock is now 0):\n"
                        + "• " + String.join("\n• ", result.depletedIngredients()) + "\n\n"
                        + "Dishes requiring these ingredients can no longer be ordered until restocked!");
            }
        } else {
            orderSummaryLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
            orderSummaryLabel.setText(result.message());
            // Requirement 2: Show warning messages when there aren't enough ingredients
            AlertUtil.error("Insufficient Stock Warning", result.message());
        }

        updateDishAvailabilityWarning();
        dishList.refresh();
    }

    // ================================================================= SELECTED DISH VIEW

    private void showDish(Dish dish) {
        if (dish == null) {
            dishImage.setImage(loadResourceImage("placeholder.png"));
            recipeTable.setItems(FXCollections.observableArrayList());
            selectedDishLabel.setText("No dish selected");
            dishCategoryBadge.setText("");
            dishWarningBanner.setVisible(false);
            dishWarningBanner.setManaged(false);
            servingsBadge.setText("");
            return;
        }

        // Dish picture: custom or preset or placeholder
        Image img = dish.getImage();
        dishImage.setImage(img != null ? img : loadResourceImage("placeholder.png"));

        recipeTable.setItems(FXCollections.observableArrayList(dish.getRecipe()));
        refreshSelectedLabel();
        updateDishAvailabilityWarning();
    }

    private void refreshSelectedLabel() {
        Dish dish = dishList.getSelectionModel().getSelectedItem();
        if (dish == null) {
            selectedDishLabel.setText("Select a dish from the menu");
            dishCategoryBadge.setText("");
            servingsBadge.setText("");
            return;
        }

        selectedDishLabel.setText(String.format("%s  -  $%.2f", dish.getName(), dish.getPrice()));
        dishCategoryBadge.setText(dish.getCategory());

        int max = dish.maxServings();
        if (dish.isAvailable()) {
            servingsBadge.setText("✓ " + max + " serving(s) can be made");
            servingsBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d;");
        } else {
            servingsBadge.setText("⚠ 0 servings available");
            servingsBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c;");
        }
    }

    /**
     * Requirement 2: Live warning message for not enough ingredients to prepare the item.
     */
    private void updateDishAvailabilityWarning() {
        Dish dish = dishList.getSelectionModel().getSelectedItem();
        if (dish == null) {
            dishWarningBanner.setVisible(false);
            dishWarningBanner.setManaged(false);
            return;
        }

        int requestedQty = currentQty();
        List<String> missing = dish.missingIngredients(requestedQty);

        if (!missing.isEmpty()) {
            dishWarningLabel.setText("Warning: Not enough ingredients to prepare " + requestedQty + " x " + dish.getName()
                    + "! Missing: " + String.join(", ", missing));
            dishWarningBanner.setStyle("-fx-background-color: #fef2f2; -fx-border-color: #ef4444; -fx-border-radius: 8px; -fx-background-radius: 8px;");
            dishWarningBanner.setVisible(true);
            dishWarningBanner.setManaged(true);
        } else {
            dishWarningLabel.setText("All ingredients are available in stock for " + requestedQty + " serving(s).");
            dishWarningBanner.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #86efac; -fx-border-radius: 8px; -fx-background-radius: 8px;");
            dishWarningBanner.setVisible(true);
            dishWarningBanner.setManaged(true);
        }
    }

    // ================================================================= MENU MANAGEMENT (ADMIN ONLY)

    /**
     * Requirement 5: Administrators can add or upload pictures of menu items.
     */
    @FXML
    private void onUploadPicture() {
        Dish dish = dishList.getSelectionModel().getSelectedItem();
        if (dish == null) {
            AlertUtil.warning("No Selection", "Please select a dish first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Upload Food Photo for " + dish.getName());
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.webp)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp"));

        File file = chooser.showOpenDialog(dishImage.getScene().getWindow());
        if (file != null) {
            dish.setImageName(file.toURI().toString());
            Image newImg = dish.getImage();
            dishImage.setImage(newImg != null ? newImg : loadResourceImage("placeholder.png"));
            dishList.refresh();
            AlertUtil.info("Picture Updated", "Successfully updated picture for " + dish.getName() + ".");
        }
    }

    /**
     * Requirement 5: Administrators can cycle or select from built-in images.
     */
    @FXML
    private void onChangeImage() {
        Dish dish = dishList.getSelectionModel().getSelectedItem();
        if (dish == null) {
            AlertUtil.warning("No Selection", "Please select a dish first.");
            return;
        }
        List<String> files = Dish.IMAGE_FILES;
        int next = (files.indexOf(dish.getImageName()) + 1) % files.size();
        dish.setImageName(files.get(next));
        Image newImg = dish.getImage();
        dishImage.setImage(newImg != null ? newImg : loadResourceImage("placeholder.png"));
        dishList.refresh();
    }

    /**
     * Requirement 5: Administrators can remove pictures of menu items.
     */
    @FXML
    private void onRemovePicture() {
        Dish dish = dishList.getSelectionModel().getSelectedItem();
        if (dish == null) {
            AlertUtil.warning("No Selection", "Please select a dish first.");
            return;
        }
        dish.removeImage();
        dishImage.setImage(loadResourceImage("placeholder.png"));
        dishList.refresh();
        AlertUtil.info("Picture Removed", "Removed picture for " + dish.getName() + ". Reverted to placeholder.");
    }

    private Image loadResourceImage(String fileName) {
        URL url = getClass().getResource("/images/" + fileName);
        return url == null ? null : new Image(url.toExternalForm());
    }

    // ================================================================= RECIPE TABLE

    private int currentQty() {
        Integer value = qtySpinner.getValue();
        return value == null ? 1 : value;
    }

    private void setupRecipeTable() {
        recIngredientCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getIngredient().getName()));

        recNeededCol.setCellValueFactory(cell -> {
            RecipeLine line = cell.getValue();
            return Bindings.createStringBinding(
                    () -> Ingredient.formatAmount(line.getAmount() * currentQty())
                            + " " + line.getIngredient().getUnit(),
                    qtySpinner.valueProperty());
        });

        recInStockCol.setCellValueFactory(cell -> {
            Ingredient ingredient = cell.getValue().getIngredient();
            return Bindings.createStringBinding(
                    () -> Ingredient.formatAmount(ingredient.getQuantity()) + " " + ingredient.getUnit(),
                    ingredient.quantityProperty());
        });

        recStatusCol.setCellValueFactory(cell -> {
            RecipeLine line = cell.getValue();
            return Bindings.createStringBinding(
                    () -> line.getIngredient().getQuantity() >= line.getAmount() * currentQty()
                            ? "OK" : "NOT ENOUGH",
                    line.getIngredient().quantityProperty(), qtySpinner.valueProperty());
        });

        recStatusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("OK".equals(status)) {
                        setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold; -fx-background-color: #dcfce7;");
                    } else {
                        setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-background-color: #fee2e2;");
                    }
                }
            }
        });
    }
}
