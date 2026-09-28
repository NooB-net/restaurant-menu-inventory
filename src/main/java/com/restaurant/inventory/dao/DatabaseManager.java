package com.restaurant.inventory.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database Integration: SQLite Database Manager.
 * Manages the SQLite database connection, table structures, and foreign key relationships.
 *
 * Tables:
 * - ingredients: id (name), unit, quantity, min_level, default_quantity
 * - dishes: id (name), category, price, image_name
 * - recipes: id, dish_name (FK -> dishes.name), ingredient_name (FK -> ingredients.name), amount
 * - staff: id, name, gender, skill_level, country, dob, photo_file
 * - users: username (PK), password, full_name, role, email, phone, country, dob, gender, bio, avatar_path
 */
public final class DatabaseManager {

    private static final String DB_FILE = "restaurant.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    private static Connection connection;

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA busy_timeout = 10000;");
        }
        return conn;
    }

    public static synchronized void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Ingredients Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ingredients (
                    name TEXT PRIMARY KEY,
                    unit TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    min_level REAL NOT NULL,
                    default_quantity REAL NOT NULL
                );
            """);

            // 2. Dishes Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dishes (
                    name TEXT PRIMARY KEY,
                    category TEXT NOT NULL,
                    price REAL NOT NULL,
                    image_name TEXT
                );
            """);

            // 3. Recipes Table (Relationship: Many-to-Many between Dishes & Ingredients)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recipes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    dish_name TEXT NOT NULL,
                    ingredient_name TEXT NOT NULL,
                    amount REAL NOT NULL,
                    FOREIGN KEY (dish_name) REFERENCES dishes(name) ON DELETE CASCADE,
                    FOREIGN KEY (ingredient_name) REFERENCES ingredients(name) ON DELETE CASCADE
                );
            """);

            // 4. Staff Directory Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS staff (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    gender TEXT,
                    skill_level TEXT,
                    country TEXT,
                    dob TEXT,
                    photo_file TEXT
                );
            """);

            // 5. Users Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    username TEXT PRIMARY KEY,
                    password TEXT,
                    full_name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    email TEXT,
                    phone TEXT,
                    country TEXT,
                    dob TEXT,
                    gender TEXT,
                    bio TEXT,
                    avatar_path TEXT
                );
            """);

            System.out.println("[DatabaseManager] SQLite tables and relationships initialized successfully.");
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
