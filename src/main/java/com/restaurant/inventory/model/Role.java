package com.restaurant.inventory.model;

/**
 * Role of a user in the application.
 */
public enum Role {
    ADMIN("Administrator"),
    USER("Standard User");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
