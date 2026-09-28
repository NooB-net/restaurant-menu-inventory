package com.restaurant.inventory.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

/**
 * Advanced OOP Concept: Abstract Class
 * Serves as the base entity for human profiles in the system (Staff, Users).
 */
public abstract class AbstractPerson implements Identifiable {

    protected final StringProperty name = new SimpleStringProperty();
    protected final StringProperty gender = new SimpleStringProperty();
    protected final StringProperty country = new SimpleStringProperty();
    protected final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();

    public AbstractPerson(String name, String gender, String country, LocalDate dateOfBirth) {
        this.name.set(name);
        this.gender.set(gender);
        this.country.set(country);
        this.dateOfBirth.set(dateOfBirth);
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

    public String getGender() {
        return gender.get();
    }

    public void setGender(String gender) {
        this.gender.set(gender);
    }

    public StringProperty genderProperty() {
        return gender;
    }

    public String getCountry() {
        return country.get();
    }

    public void setCountry(String country) {
        this.country.set(country);
    }

    public StringProperty countryProperty() {
        return country;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth.get();
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth.set(dateOfBirth);
    }

    public ObjectProperty<LocalDate> dateOfBirthProperty() {
        return dateOfBirth;
    }

    /** Abstract method for role or position description */
    public abstract String getRoleTitle();
}
