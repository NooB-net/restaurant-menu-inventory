package com.restaurant.inventory.controller;

import com.restaurant.inventory.model.Ingredient;
import com.restaurant.inventory.service.InventoryService;
import com.restaurant.inventory.util.AlertUtil;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * "Ingredients & Purchases" tab for Administrators.
 * Shows available ingredients, items needing purchase, triggers not-enough-ingredient warnings,
 * and maintains restock and progress demonstration capabilities.
 */
public class InventoryController implements Initializable {

    // Warning Banner
    @FXML private HBox inventoryWarningBanner;
    @FXML private Label inventoryWarningLabel;

    // Available Ingredients Table
    @FXML private Label stockAlertLabel;
    @FXML private TableView<Ingredient> ingredientTable;
    @FXML private TableColumn<Ingredient, String> nameCol;
    @FXML private TableColumn<Ingredient, Number> qtyCol;
    @FXML private TableColumn<Ingredient, String> unitCol;
    @FXML private TableColumn<Ingredient, Number> minCol;
    @FXML private TableColumn<Ingredient, String> statusCol;

    // Items Needing Purchase Table
    @FXML private TableView<Ingredient> purchaseTable;
    @FXML private TableColumn<Ingredient, String> purNameCol;
    @FXML private TableColumn<Ingredient, Number> purStockCol;
    @FXML private TableColumn<Ingredient, Number> purMinCol;
    @FXML private TableColumn<Ingredient, Number> purDeficitCol;
    @FXML private TableColumn<Ingredient, Number> purSuggestedCol;
    @FXML private TableColumn<Ingredient, String> purStatusCol;

    // Restock panel
    @FXML private Label selectedIngredientLabel;
    @FXML private TextField restockField;
    @FXML private Label restockMessageLabel;

    // Accumulate / progress panel
    @FXML private TextField targetField;
    @FXML private Label sumLabel;
    @FXML private Label pressesLabel;
    @FXML private ProgressBar progressBar;

    private final InventoryService service = InventoryService.getInstance();
    private final ObservableList<Ingredient> purchaseItems = FXCollections.observableArrayList();

    // State of the Accumulate demo
    private double currentSum = 0;
    private int pressCount = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupPurchaseTable();
        refreshPurchaseList();

        stockAlertLabel.textProperty().bind(service.stockAlertProperty());

        ingredientTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, ingredient) -> {
            if (ingredient != null) {
                selectedIngredientLabel.setText("Selected: " + ingredient.getName() + " (" + ingredient.getUnit() + ")");
            } else {
                selectedIngredientLabel.setText("Select an ingredient in either table");
            }
        });

        purchaseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, ingredient) -> {
            if (ingredient != null) {
                ingredientTable.getSelectionModel().select(ingredient);
                selectedIngredientLabel.setText("Selected to purchase: " + ingredient.getName() + " (" + ingredient.getUnit() + ")");
                restockField.setText(String.valueOf((int) ingredient.getSuggestedPurchase()));
            }
        });

        // Listen for stock changes to update both tables and trigger warnings
        service.getIngredients().addListener((ListChangeListener<Ingredient>) change -> {
            ingredientTable.refresh();
            refreshPurchaseList();
        });

        service.stockVersionProperty().addListener((obs, oldVal, newVal) -> refreshPurchaseList());

        // Default values for Accumulate panel
        sumLabel.setText("Current sum: 0");
        pressesLabel.setText("Presses: 0");
        progressBar.setProgress(0);
    }

    // ================================================================= WARNING SYSTEM

    /**
     * Requirement 2 & 3: Trigger "not enough ingredient" warning banner and modal dialog.
     */
    @FXML
    public void onTriggerWarning() {
        List<Ingredient> needed = service.getItemsNeedingPurchase();
        if (needed.isEmpty()) {
            AlertUtil.info("Stock Status Healthy", "✓ All ingredients have sufficient stock! No items need purchasing.");
            return;
        }

        String summary = service.getShortageWarningSummary();
        AlertUtil.warning("Not Enough Ingredients Warning",
                "⚠ INSUFFICIENT INGREDIENTS & PURCHASE WARNING\n\n"
                + "The kitchen currently does not have enough of the following ingredients to meet menu demand:\n\n"
                + summary + "\n\nPlease use the 'Purchase / Restock' section below to replenish stock.");
    }

    private void refreshPurchaseList() {
        purchaseItems.setAll(service.getItemsNeedingPurchase());
        purchaseTable.refresh();

        if (inventoryWarningBanner != null) {
            if (!purchaseItems.isEmpty()) {
                long outCount = purchaseItems.stream().filter(Ingredient::isOutOfStock).count();
                inventoryWarningLabel.setText(String.format(
                        "Warning: Not enough ingredients! %d item(s) need purchasing (%d completely OUT OF STOCK).",
                        purchaseItems.size(), outCount));
                inventoryWarningBanner.setVisible(true);
                inventoryWarningBanner.setManaged(true);
            } else {
                inventoryWarningLabel.setText("All ingredients are in stock and healthy.");
                inventoryWarningBanner.setVisible(false);
                inventoryWarningBanner.setManaged(false);
            }
        }
    }

    // ================================================================= AVAILABLE INGREDIENTS TABLE

    private void setupTable() {
        ingredientTable.setItems(service.getIngredients());

        nameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
        unitCol.setCellValueFactory(cell -> cell.getValue().unitProperty());
        qtyCol.setCellValueFactory(cell -> cell.getValue().quantityProperty());
        minCol.setCellValueFactory(cell -> cell.getValue().minLevelProperty());
        statusCol.setCellValueFactory(cell -> cell.getValue().statusProperty());

        qtyCol.setCellFactory(column -> numberCell());
        minCol.setCellFactory(column -> numberCell());
        statusCol.setCellFactory(column -> statusCell());
    }

    // ================================================================= ITEMS NEEDING PURCHASE TABLE

    private void setupPurchaseTable() {
        purchaseTable.setItems(purchaseItems);

        purNameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
        purStockCol.setCellValueFactory(cell -> cell.getValue().quantityProperty());
        purMinCol.setCellValueFactory(cell -> cell.getValue().minLevelProperty());
        purDeficitCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getDeficit()));
        purSuggestedCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getSuggestedPurchase()));
        purStatusCol.setCellValueFactory(cell -> cell.getValue().statusProperty());

        purStockCol.setCellFactory(column -> numberCell());
        purMinCol.setCellFactory(column -> numberCell());
        purDeficitCol.setCellFactory(column -> numberCell());
        purSuggestedCol.setCellFactory(column -> numberCell());
        purStatusCol.setCellFactory(column -> statusCell());
    }

    private TableCell<Ingredient, Number> numberCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : Ingredient.formatAmount(value.doubleValue()));
            }
        };
    }

    private TableCell<Ingredient, String> statusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                switch (status) {
                    case "OUT OF STOCK" -> setStyle("-fx-text-fill: white; -fx-background-color: #dc2626; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    case "LOW"          -> setStyle("-fx-text-fill: #92400e; -fx-background-color: #fef3c7; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    default             -> setStyle("-fx-text-fill: #15803d; -fx-background-color: #dcfce7; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        };
    }

    // ================================================================= RESTOCK & PURCHASE ACTIONS

    @FXML
    private void onRestock() {
        Ingredient ingredient = ingredientTable.getSelectionModel().getSelectedItem();
        if (ingredient == null) {
            ingredient = purchaseTable.getSelectionModel().getSelectedItem();
        }
        if (ingredient == null) {
            AlertUtil.warning("Nothing Selected", "Please select an ingredient from the table to restock.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(restockField.getText().trim());
        } catch (NumberFormatException e) {
            AlertUtil.error("Invalid Number", "Please enter a valid amount (e.g. 10 or 250).");
            return;
        }
        if (amount <= 0) {
            AlertUtil.error("Invalid Number", "The amount must be greater than zero.");
            return;
        }

        ingredient.add(amount);
        service.updateAlert();
        refreshPurchaseList();

        restockMessageLabel.setText("✓ Added " + Ingredient.formatAmount(amount) + " " + ingredient.getUnit()
                + " to " + ingredient.getName() + ".");
        restockField.clear();
        AlertUtil.info("Stock Replenished", "Added " + Ingredient.formatAmount(amount) + " " + ingredient.getUnit()
                + " to " + ingredient.getName() + ".");
    }

    @FXML
    private void onPurchaseSelected() {
        Ingredient item = purchaseTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            AlertUtil.warning("No Selection", "Please click an item in the 'Items That Need to Be Purchased' table.");
            return;
        }
        double suggested = item.getSuggestedPurchase();
        if (suggested <= 0) {
            suggested = Math.max(10, item.getMinLevel() * 2);
        }
        item.add(suggested);
        service.updateAlert();
        refreshPurchaseList();
        AlertUtil.info("Purchase Successful", "Purchased " + Ingredient.formatAmount(suggested) + " " + item.getUnit()
                + " of " + item.getName() + ". Stock is now healthy.");
    }

    @FXML
    private void onPurchaseAllNeeded() {
        List<Ingredient> needed = service.getItemsNeedingPurchase();
        if (needed.isEmpty()) {
            AlertUtil.info("Stock Complete", "All ingredients are already sufficiently stocked.");
            return;
        }

        int count = service.purchaseAllDeficits();
        refreshPurchaseList();
        AlertUtil.info("Bulk Purchase Complete",
                "Successfully purchased and restocked " + count + " shortage items!\nAll ingredients now meet safe kitchen levels.");
    }

    // ================================================================= PROGRESSBAR DEMO

    @FXML
    private void onAccumulate() {
        double number;
        try {
            number = Double.parseDouble(targetField.getText().trim());
        } catch (NumberFormatException e) {
            AlertUtil.error("Invalid Number", "Type a whole number in the box first (for example 5).");
            return;
        }
        int target = (int) number;
        if (number != target || target < 1) {
            AlertUtil.error("Invalid Number", "Please enter a whole number of 1 or more.");
            return;
        }
        if (pressCount >= target) {
            AlertUtil.info("Already Complete", "The progress bar is already full. Press Reset to start again.");
            return;
        }

        pressCount++;
        currentSum += number;

        sumLabel.setText("Current sum: " + Ingredient.formatAmount(currentSum));
        pressesLabel.setText("Presses: " + pressCount + " / " + target);
        progressBar.setProgress((double) pressCount / target);

        if (pressCount == target) {
            AlertUtil.info("Done", "The progress bar is full after " + target + " presses.");
        }
    }

    @FXML
    private void onResetAccumulate() {
        currentSum = 0;
        pressCount = 0;
        sumLabel.setText("Current sum: 0");
        pressesLabel.setText("Presses: 0");
        progressBar.setProgress(0);
    }
}
