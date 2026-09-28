package com.restaurant.inventory.dao;

import com.restaurant.inventory.model.Dish;
import com.restaurant.inventory.model.Ingredient;
import com.restaurant.inventory.model.RecipeLine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Manipulation: Full CRUD implementation for Dishes and their Recipe relationships in SQLite.
 */
public class DishDao implements GenericDao<Dish, String> {

    @Override
    public void create(Dish entity) {
        String insertDish = "INSERT OR REPLACE INTO dishes (name, category, price, image_name) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertDish)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getCategory());
            pstmt.setDouble(3, entity.getPrice());
            pstmt.setString(4, entity.getImageName());
            pstmt.executeUpdate();

            // Insert recipe relationship lines
            saveRecipeLines(conn, entity);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveRecipeLines(Connection conn, Dish dish) throws SQLException {
        String delRecipe = "DELETE FROM recipes WHERE dish_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(delRecipe)) {
            pstmt.setString(1, dish.getName());
            pstmt.executeUpdate();
        }

        String insertRecipe = "INSERT INTO recipes (dish_name, ingredient_name, amount) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertRecipe)) {
            for (RecipeLine line : dish.getRecipe()) {
                pstmt.setString(1, dish.getName());
                pstmt.setString(2, line.getIngredient().getName());
                pstmt.setDouble(3, line.getAmount());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    @Override
    public Optional<Dish> findById(String name) {
        String sql = "SELECT * FROM dishes WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Dish dish = new Dish(
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getDouble("price"),
                            rs.getString("image_name")
                    );
                    return Optional.of(dish);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Dish> findAllWithIngredients(Map<String, Ingredient> ingredientLookup) {
        // Step 1: Load all dishes (close connection before loading recipes to avoid
        // SQLite "multiple active statements on same connection" limitation)
        List<Dish> list = new ArrayList<>();
        String sql = "SELECT * FROM dishes ORDER BY category, name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Dish(
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getString("image_name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Step 2: Load recipe lines for each dish using fresh separate connections
        for (Dish dish : list) {
            loadRecipeForDish(dish, ingredientLookup);
        }
        return list;
    }

    private void loadRecipeForDish(Dish dish, Map<String, Ingredient> ingredientLookup) {
        String sql = "SELECT ingredient_name, amount FROM recipes WHERE dish_name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dish.getName());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String ingName = rs.getString("ingredient_name");
                    double amount = rs.getDouble("amount");
                    Ingredient ing = ingredientLookup.get(ingName);
                    if (ing != null) {
                        dish.needs(ing, amount);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Dish> findAll() {
        return findAllWithIngredients(Map.of());
    }

    @Override
    public void update(Dish entity) {
        String sql = "UPDATE dishes SET category = ?, price = ?, image_name = ? WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entity.getCategory());
            pstmt.setDouble(2, entity.getPrice());
            pstmt.setString(3, entity.getImageName());
            pstmt.setString(4, entity.getName());
            pstmt.executeUpdate();

            saveRecipeLines(conn, entity);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String name) {
        String sql = "DELETE FROM dishes WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
