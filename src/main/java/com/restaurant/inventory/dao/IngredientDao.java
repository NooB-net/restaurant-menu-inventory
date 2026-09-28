package com.restaurant.inventory.dao;

import com.restaurant.inventory.model.Ingredient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Manipulation: Full CRUD implementation for Ingredients in SQLite.
 */
public class IngredientDao implements GenericDao<Ingredient, String> {

    @Override
    public void create(Ingredient entity) {
        String sql = "INSERT INTO ingredients (name, unit, quantity, min_level, default_quantity) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(name) DO UPDATE SET unit = excluded.unit, quantity = excluded.quantity, min_level = excluded.min_level, default_quantity = excluded.default_quantity";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getUnit());
            pstmt.setDouble(3, entity.getQuantity());
            pstmt.setDouble(4, entity.getMinLevel());
            pstmt.setDouble(5, entity.getDefaultQuantity());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveAll(List<Ingredient> list) {
        String sql = "INSERT INTO ingredients (name, unit, quantity, min_level, default_quantity) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(name) DO UPDATE SET unit = excluded.unit, quantity = excluded.quantity, min_level = excluded.min_level, default_quantity = excluded.default_quantity";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Ingredient entity : list) {
                    pstmt.setString(1, entity.getName());
                    pstmt.setString(2, entity.getUnit());
                    pstmt.setDouble(3, entity.getQuantity());
                    pstmt.setDouble(4, entity.getMinLevel());
                    pstmt.setDouble(5, entity.getDefaultQuantity());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Ingredient> findById(String name) {
        String sql = "SELECT * FROM ingredients WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Ingredient(
                            rs.getString("name"),
                            rs.getString("unit"),
                            rs.getDouble("quantity"),
                            rs.getDouble("min_level"),
                            rs.getDouble("default_quantity")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Ingredient> findAll() {
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT * FROM ingredients ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Ingredient(
                        rs.getString("name"),
                        rs.getString("unit"),
                        rs.getDouble("quantity"),
                        rs.getDouble("min_level"),
                        rs.getDouble("default_quantity")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void update(Ingredient entity) {
        String sql = "UPDATE ingredients SET quantity = ?, min_level = ?, unit = ? WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, entity.getQuantity());
            pstmt.setDouble(2, entity.getMinLevel());
            pstmt.setString(3, entity.getUnit());
            pstmt.setString(4, entity.getName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String name) {
        String sql = "DELETE FROM ingredients WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
