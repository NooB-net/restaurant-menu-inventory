package com.restaurant.inventory.dao;

import com.restaurant.inventory.model.Person;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Manipulation: Full CRUD implementation for Staff Directory in SQLite.
 */
public class StaffDao implements GenericDao<Person, String> {

    @Override
    public void create(Person entity) {
        String sql = "INSERT INTO staff (name, gender, skill_level, country, dob, photo_file) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getGender());
            pstmt.setString(3, entity.getSkillLevel());
            pstmt.setString(4, entity.getCountry());
            pstmt.setString(5, entity.getDateOfBirth() != null ? entity.getDateOfBirth().toString() : null);
            pstmt.setString(6, entity.getPhotoFile());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Person> findById(String name) {
        String sql = "SELECT * FROM staff WHERE name = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    LocalDate dob = rs.getString("dob") != null ? LocalDate.parse(rs.getString("dob")) : null;
                    return Optional.of(new Person(
                            rs.getString("name"),
                            rs.getString("gender"),
                            rs.getString("skill_level"),
                            rs.getString("country"),
                            dob,
                            "",
                            rs.getString("photo_file")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Person> findAll() {
        List<Person> list = new ArrayList<>();
        String sql = "SELECT * FROM staff ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String dobStr = rs.getString("dob");
                LocalDate dob = (dobStr != null && !dobStr.isBlank()) ? LocalDate.parse(dobStr) : null;
                list.add(new Person(
                        rs.getString("name"),
                        rs.getString("gender"),
                        rs.getString("skill_level"),
                        rs.getString("country"),
                        dob,
                        "",
                        rs.getString("photo_file")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void update(Person entity) {
        String sql = "UPDATE staff SET gender = ?, skill_level = ?, country = ?, dob = ?, photo_file = ? WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entity.getGender());
            pstmt.setString(2, entity.getSkillLevel());
            pstmt.setString(3, entity.getCountry());
            pstmt.setString(4, entity.getDateOfBirth() != null ? entity.getDateOfBirth().toString() : null);
            pstmt.setString(5, entity.getPhotoFile());
            pstmt.setString(6, entity.getName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String name) {
        String sql = "DELETE FROM staff WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
