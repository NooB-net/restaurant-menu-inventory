package com.restaurant.inventory.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Advanced OOP Concept: Abstract Class
 * Provides base fields and common behavior for catalog items (such as Dishes).
 */
public abstract class MenuItemBase implements Identifiable {

    protected final StringProperty name = new SimpleStringProperty();
    protected final StringProperty category = new SimpleStringProperty();

    public MenuItemBase(String name, String category) {
        this.name.set(name);
        this.category.set(category);
    }

    @Override
    public String getId() {
        return getName();
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getCategory() {
        return category.get();
    }

    public void setCategory(String category) {
        this.category.set(category);
    }

    public StringProperty categoryProperty() {
        return category;
    }

    /** Abstract method to calculate price or special discounted price */
    public abstract double getPrice();

    /** Abstract method checking availability */
    public abstract boolean isAvailable();
}
