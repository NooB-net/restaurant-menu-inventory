package com.restaurant.inventory.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MODEL: a menu item (named Dish so it does not clash with javafx.scene.control.MenuItem).
 * A dish knows which ingredients it needs, so it can tell whether it can be prepared.
 * Supports dynamic image addition, replacement, and removal by Administrators.
 */
public class Dish {

    /** Built-in images stored in src/main/resources/images. */
    public static final List<String> IMAGE_FILES = List.of(
            "burger.png", "pizza.png", "pasta.png", "salad.png",
            "soup.png", "juice.png", "dessert.png");

    private final String name;
    private final String category;
    private final double price;
    private final StringProperty imageName = new SimpleStringProperty();
    private final List<RecipeLine> recipe = new ArrayList<>();

    public Dish(String name, String category, double price, String imageName) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageName.set(imageName);
    }

    /** Fluent helper: dish.needs(patty, 1).needs(bun, 1) ... */
    public Dish needs(Ingredient ingredient, double amountPerServing) {
        recipe.add(new RecipeLine(ingredient, amountPerServing));
        return this;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }

    public String getImageName() { return imageName.get(); }
    public void setImageName(String imageName) { this.imageName.set(imageName); }
    public StringProperty imageNameProperty() { return imageName; }

    /** Whether this dish has an active picture assigned. */
    public boolean hasImage() {
        String img = getImageName();
        return img != null && !img.trim().isEmpty() && !img.equalsIgnoreCase("none");
    }

    /** Removes the picture from this dish. */
    public void removeImage() {
        this.imageName.set(null);
    }

    /**
     * Resolves the JavaFX Image for this dish.
     * Supports both classpath resources (/images/...) and local file paths.
     */
    public Image getImage() {
        String img = getImageName();
        if (img == null || img.trim().isEmpty() || "none".equalsIgnoreCase(img)) {
            return null;
        }
        try {
            if (img.startsWith("file:") || img.startsWith("http:") || img.startsWith("https:")) {
                return new Image(img);
            }
            File localFile = new File(img);
            if (localFile.exists() && localFile.isFile()) {
                return new Image(localFile.toURI().toString());
            }
            URL res = getClass().getResource("/images/" + img);
            if (res != null) {
                return new Image(res.toExternalForm());
            }
        } catch (Exception e) {
            // Ignore corrupted or missing image path
        }
        return null;
    }

    public List<RecipeLine> getRecipe() { return Collections.unmodifiableList(recipe); }

    /** True if there is enough stock of EVERY ingredient for the given number of servings. */
    public boolean canMake(int servings) {
        return missingIngredients(servings).isEmpty();
    }

    /** True if at least one serving can be prepared. */
    public boolean isAvailable() {
        return canMake(1);
    }

    /** Maximum number of servings that can be prepared with current stock. */
    public int maxServings() {
        if (recipe.isEmpty()) return 999;
        int max = Integer.MAX_VALUE;
        for (RecipeLine line : recipe) {
            if (line.getAmount() <= 0) continue;
            int available = (int) (line.getIngredient().getQuantity() / line.getAmount());
            if (available < max) {
                max = available;
            }
        }
        return max == Integer.MAX_VALUE ? 0 : max;
    }

    /** Human readable list of ingredients that are not enough, e.g. "Cheese Slice (need 2, have 1)". */
    public List<String> missingIngredients(int servings) {
        List<String> missing = new ArrayList<>();
        for (RecipeLine line : recipe) {
            double needed = line.getAmount() * servings;
            double have = line.getIngredient().getQuantity();
            if (have < needed) {
                missing.add(String.format("%s (need %s %s, have %s %s)",
                        line.getIngredient().getName(),
                        Ingredient.formatAmount(needed),
                        line.getIngredient().getUnit(),
                        Ingredient.formatAmount(have),
                        line.getIngredient().getUnit()));
            }
        }
        return missing;
    }

    @Override
    public String toString() { return name; }
}
