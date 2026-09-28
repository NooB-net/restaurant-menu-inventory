package com.restaurant.inventory.service;

import com.restaurant.inventory.dao.DatabaseManager;
import com.restaurant.inventory.dao.UserDao;
import com.restaurant.inventory.model.Role;
import com.restaurant.inventory.model.User;
import com.restaurant.inventory.util.ThreadPoolManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.List;

/**
 * Service managing user accounts, authentication, active session, and profile updates.
 * Backed by SQLite database persistence (UserDao) with thread pool execution.
 */
public final class UserService {

    public record AuthResult(boolean success, String message, User user) { }

    private static final UserService INSTANCE = new UserService();

    public static UserService getInstance() {
        return INSTANCE;
    }

    private final UserDao userDao = new UserDao();
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObjectProperty<User> currentUser = new SimpleObjectProperty<>(null);

    private UserService() {
        DatabaseManager.initializeDatabase();
        loadUsersFromDb();
    }

    private void loadUsersFromDb() {
        List<User> fromDb = userDao.findAll();
        if (fromDb.isEmpty()) {
            seedUsers();
        } else {
            users.setAll(fromDb);
        }
    }

    public ObservableList<User> getUsers() {
        return users;
    }

    public ObjectProperty<User> currentUserProperty() {
        return currentUser;
    }

    public User getCurrentUser() {
        return currentUser.get();
    }

    public void setCurrentUser(User user) {
        this.currentUser.set(user);
    }

    /**
     * Authenticates user credentials.
     * Returns detailed AuthResult with clear warning/error messages for invalid inputs.
     */
    public AuthResult authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return new AuthResult(false, "Username cannot be empty. Please enter your username.", null);
        }
        String trimmedUser = username.trim();
        User user = findUser(trimmedUser);
        if (user == null) {
            return new AuthResult(false,
                    "Account not found: No user registered with username \"" + trimmedUser + "\".", null);
        }

        // Only administrators require a password. Standard users do not need a password.
        if (user.getRole() == Role.ADMIN) {
            if (password == null || password.isEmpty()) {
                return new AuthResult(false, "Password cannot be empty. Please enter your password.", null);
            }
            if (!user.getPassword().equals(password)) {
                return new AuthResult(false,
                        "Incorrect password for \"" + trimmedUser + "\". Please verify and try again.", null);
            }
        }

        currentUser.set(user);
        return new AuthResult(true, "Login successful! Welcome back, " + user.getFullName() + ".", user);
    }

    public void logout() {
        currentUser.set(null);
    }

    public User findUser(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst()
                .orElse(null);
    }

    /** Registers a new user and persists to SQLite. */
    public AuthResult register(String username, String password, String fullName, Role role,
                               String email, String phone, String country, LocalDate dob,
                               String gender, String bio, String avatarPath) {
        if (username == null || username.trim().isEmpty()) {
            return new AuthResult(false, "Username is required.", null);
        }
        if (findUser(username) != null) {
            return new AuthResult(false, "Username \"" + username + "\" is already taken.", null);
        }
        if (password == null || (password.length() > 0 && password.length() < 4)) {
            return new AuthResult(false, "Password must be at least 4 characters long (or leave blank for standard users).", null);
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            return new AuthResult(false, "Full name is required.", null);
        }

        User newUser = new User(
                username.trim(),
                password == null ? "" : password,
                fullName.trim(),
                role == null ? Role.USER : role,
                email == null ? "" : email.trim(),
                phone == null ? "" : phone.trim(),
                country == null ? "United States" : country,
                dob == null ? LocalDate.of(1995, 1, 1) : dob,
                gender == null ? "Other" : gender,
                bio == null ? "" : bio.trim(),
                avatarPath == null ? "salad.png" : avatarPath
        );

        users.add(newUser);
        // Persist asynchronously in thread pool
        ThreadPoolManager.execute(() -> userDao.create(newUser));
        return new AuthResult(true, "Account created successfully.", newUser);
    }

    public void updateUser(User user) {
        ThreadPoolManager.execute(() -> userDao.update(user));
    }

    /** Resets users to default starting state. */
    public void resetUsers() {
        users.clear();
        seedUsers();
        currentUser.set(null);
    }

    /** Preloaded sample accounts for Admin and Normal Users. */
    private void seedUsers() {
        User admin = new User(
                "admin",
                "admin123",
                "Sarah Jenkins",
                Role.ADMIN,
                "admin@gourmetkitchen.com",
                "+1 (555) 234-5678",
                "United States",
                LocalDate.of(1988, 6, 15),
                "Female",
                "Head Kitchen Administrator & Inventory Supervisor",
                "salad.png"
        );

        User user = new User(
                "user",
                "",
                "Alex Morgan",
                Role.USER,
                "alex.morgan@example.com",
                "+1 (555) 876-5432",
                "Canada",
                LocalDate.of(1996, 3, 22),
                "Male",
                "Regular customer & Italian food lover",
                "burger.png"
        );

        User ayesha = new User(
                "ayesha",
                "",
                "Ayesha Rahman",
                Role.USER,
                "ayesha.r@example.com",
                "+880 1711-223344",
                "Bangladesh",
                LocalDate.of(1994, 9, 3),
                "Female",
                "Food critic & dessert enthusiast",
                "dessert.png"
        );

        users.addAll(admin, user, ayesha);
        for (User u : users) {
            userDao.create(u);
        }
    }
}
