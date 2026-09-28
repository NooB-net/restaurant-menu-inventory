package com.restaurant.inventory.dao;

import com.restaurant.inventory.model.Role;
import com.restaurant.inventory.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Manipulation: Full CRUD implementation for Users in SQLite.
 */
public class UserDao implements GenericDao<User, String> {

    @Override
    public void create(User entity) {
        String sql = "INSERT OR REPLACE INTO users (username, password, full_name, role, email, phone, country, dob, gender, bio, avatar_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entity.getUsername());
            pstmt.setString(2, entity.getPassword());
            pstmt.setString(3, entity.getFullName());
            pstmt.setString(4, entity.getRole().name());
            pstmt.setString(5, entity.getEmail());
            pstmt.setString(6, entity.getPhone());
            pstmt.setString(7, entity.getCountry());
            pstmt.setString(8, entity.getDateOfBirth() != null ? entity.getDateOfBirth().toString() : null);
            pstmt.setString(9, entity.getGender());
            pstmt.setString(10, entity.getBio());
            pstmt.setString(11, entity.getAvatarPath());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<User> findById(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    LocalDate dob = rs.getString("dob") != null ? LocalDate.parse(rs.getString("dob")) : null;
                    Role role = Role.valueOf(rs.getString("role"));
                    return Optional.of(new User(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("full_name"),
                            role,
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("country"),
                            dob,
                            rs.getString("gender"),
                            rs.getString("bio"),
                            rs.getString("avatar_path")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String dobStr = rs.getString("dob");
                LocalDate dob = (dobStr != null && !dobStr.isBlank()) ? LocalDate.parse(dobStr) : null;
                Role role = Role.valueOf(rs.getString("role"));
                list.add(new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name"),
                        role,
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("country"),
                        dob,
                        rs.getString("gender"),
                        rs.getString("bio"),
                        rs.getString("avatar_path")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void update(User entity) {
        String sql = "UPDATE users SET password = ?, full_name = ?, role = ?, email = ?, phone = ?, country = ?, dob = ?, gender = ?, bio = ?, avatar_path = ? WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entity.getPassword());
            pstmt.setString(2, entity.getFullName());
            pstmt.setString(3, entity.getRole().name());
            pstmt.setString(4, entity.getEmail());
            pstmt.setString(5, entity.getPhone());
            pstmt.setString(7, entity.getCountry());
            pstmt.setString(8, entity.getDateOfBirth() != null ? entity.getDateOfBirth().toString() : null);
            pstmt.setString(9, entity.getGender());
            pstmt.setString(10, entity.getBio());
            pstmt.setString(11, entity.getAvatarPath());
            pstmt.setString(12, entity.getUsername());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
