package com.restaurant.inventory.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

/**
 * Model representing an application user (Customer or Administrator).
 * Supports profile management with observable JavaFX properties.
 */
public class User {

    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty fullName = new SimpleStringProperty();
    private final ObjectProperty<Role> role = new SimpleObjectProperty<>();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty country = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
    private final StringProperty gender = new SimpleStringProperty();
    private final StringProperty bio = new SimpleStringProperty();
    private final StringProperty avatarPath = new SimpleStringProperty();

    public User(String username, String password, String fullName, Role role,
                String email, String phone, String country,
                LocalDate dateOfBirth, String gender, String bio, String avatarPath) {
        this.username.set(username);
        this.password.set(password);
        this.fullName.set(fullName);
        this.role.set(role);
        this.email.set(email);
        this.phone.set(phone);
        this.country.set(country);
        this.dateOfBirth.set(dateOfBirth);
        this.gender.set(gender);
        this.bio.set(bio);
        this.avatarPath.set(avatarPath);
    }

    // username
    public String getUsername() { return username.get(); }
    public void setUsername(String value) { username.set(value); }
    public StringProperty usernameProperty() { return username; }

    // password
    public String getPassword() { return password.get(); }
    public void setPassword(String value) { password.set(value); }
    public StringProperty passwordProperty() { return password; }

    // fullName
    public String getFullName() { return fullName.get(); }
    public void setFullName(String value) { fullName.set(value); }
    public StringProperty fullNameProperty() { return fullName; }

    // role
    public Role getRole() { return role.get(); }
    public void setRole(Role value) { role.set(value); }
    public ObjectProperty<Role> roleProperty() { return role; }

    public boolean isAdmin() {
        return role.get() == Role.ADMIN;
    }

    // email
    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }

    // phone
    public String getPhone() { return phone.get(); }
    public void setPhone(String value) { phone.set(value); }
    public StringProperty phoneProperty() { return phone; }

    // country
    public String getCountry() { return country.get(); }
    public void setCountry(String value) { country.set(value); }
    public StringProperty countryProperty() { return country; }

    // dateOfBirth
    public LocalDate getDateOfBirth() { return dateOfBirth.get(); }
    public void setDateOfBirth(LocalDate value) { dateOfBirth.set(value); }
    public ObjectProperty<LocalDate> dateOfBirthProperty() { return dateOfBirth; }

    // gender
    public String getGender() { return gender.get(); }
    public void setGender(String value) { gender.set(value); }
    public StringProperty genderProperty() { return gender; }

    // bio
    public String getBio() { return bio.get(); }
    public void setBio(String value) { bio.set(value); }
    public StringProperty bioProperty() { return bio; }

    // avatarPath
    public String getAvatarPath() { return avatarPath.get(); }
    public void setAvatarPath(String value) { avatarPath.set(value); }
    public StringProperty avatarPathProperty() { return avatarPath; }

    @Override
    public String toString() {
        return fullName.get() + " (" + role.get() + ")";
    }
}
