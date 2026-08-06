package com.foodmate.dao;

import com.foodmate.dto.AllergyDto;
import com.foodmate.dto.UserDto;
import com.foodmate.dto.UserPreferenceDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserDao {
    private static final String COLUMNS = "id, login_id, password, nickname, created_at";

    public UserDto findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM `user` WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public UserDto findByLoginId(String loginId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM `user` WHERE login_id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loginId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsByLoginId(String loginId) throws SQLException {
        String sql = "SELECT 1 FROM `user` WHERE login_id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loginId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(UserDto user) throws SQLException {
        String sql = "INSERT INTO `user` (login_id, password, nickname) VALUES (?, ?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getLoginId());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getNickname());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return user.getId();
        }
    }

    public boolean updateNickname(int userId, String nickname) throws SQLException {
        String sql = "UPDATE `user` SET nickname = ? WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePassword(int userId, String passwordHash) throws SQLException {
        String sql = "UPDATE `user` SET password = ? WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public UserPreferenceDto findPreference(int userId) throws SQLException {
        String sql = "SELECT user_id, spicy_level, price_min, price_max, max_distance"
                + " FROM user_preference WHERE user_id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                UserPreferenceDto preference = new UserPreferenceDto();
                preference.setUserId(rs.getInt("user_id"));
                preference.setSpicyLevel(rs.getObject("spicy_level", Integer.class));
                preference.setPriceMin(rs.getObject("price_min", Integer.class));
                preference.setPriceMax(rs.getObject("price_max", Integer.class));
                preference.setMaxDistance(rs.getObject("max_distance", Integer.class));
                return preference;
            }
        }
    }

    public void savePreference(UserPreferenceDto preference) throws SQLException {
        String sql = "INSERT INTO user_preference (user_id, spicy_level, price_min, price_max, max_distance)"
                + " VALUES (?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE"
                + "   spicy_level  = VALUES(spicy_level),"
                + "   price_min    = VALUES(price_min),"
                + "   price_max    = VALUES(price_max),"
                + "   max_distance = VALUES(max_distance)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, preference.getUserId());
            ps.setObject(2, preference.getSpicyLevel());
            ps.setObject(3, preference.getPriceMin());
            ps.setObject(4, preference.getPriceMax());
            ps.setObject(5, preference.getMaxDistance());
            ps.executeUpdate();
        }
    }

    public List<String> findCategories(int userId) throws SQLException {
        String sql = "SELECT category FROM user_category WHERE user_id = ? ORDER BY category";
        List<String> categories = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.add(rs.getString(1));
                }
            }
        }
        return categories;
    }

    public void replaceCategories(int userId, Collection<String> categories) throws SQLException {
        String delete = "DELETE FROM user_category WHERE user_id = ?";
        String insert = "INSERT INTO user_category (user_id, category) VALUES (?, ?)";
        try (Connection conn = DbConnection.get()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deletePs = conn.prepareStatement(delete);
                 PreparedStatement insertPs = conn.prepareStatement(insert)) {
                deletePs.setInt(1, userId);
                deletePs.executeUpdate();

                if (categories != null) {
                    for (String category : categories) {
                        insertPs.setInt(1, userId);
                        insertPs.setString(2, category);
                        insertPs.addBatch();
                    }
                    insertPs.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<AllergyDto> findAllergies(int userId) throws SQLException {
        String sql = "SELECT a.id, a.name FROM user_allergy ua"
                + " JOIN allergy a ON a.id = ua.allergy_id"
                + " WHERE ua.user_id = ? ORDER BY a.name";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return readAllergies(ps);
        }
    }

    public void replaceAllergies(int userId, Collection<Integer> allergyIds) throws SQLException {
        String delete = "DELETE FROM user_allergy WHERE user_id = ?";
        String insert = "INSERT INTO user_allergy (user_id, allergy_id) VALUES (?, ?)";
        try (Connection conn = DbConnection.get()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deletePs = conn.prepareStatement(delete);
                 PreparedStatement insertPs = conn.prepareStatement(insert)) {
                deletePs.setInt(1, userId);
                deletePs.executeUpdate();

                if (allergyIds != null) {
                    for (Integer allergyId : allergyIds) {
                        insertPs.setInt(1, userId);
                        insertPs.setInt(2, allergyId);
                        insertPs.addBatch();
                    }
                    insertPs.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<AllergyDto> findAllAllergies() throws SQLException {
        String sql = "SELECT id, name FROM allergy ORDER BY id";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return readAllergies(ps);
        }
    }

    public AllergyDto findAllergyByName(String name) throws SQLException {
        String sql = "SELECT id, name FROM allergy WHERE name = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            List<AllergyDto> found = readAllergies(ps);
            return found.isEmpty() ? null : found.get(0);
        }
    }

    private static List<AllergyDto> readAllergies(PreparedStatement ps) throws SQLException {
        List<AllergyDto> allergies = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                allergies.add(new AllergyDto(rs.getInt("id"), rs.getString("name")));
            }
        }
        return allergies;
    }

    private static UserDto map(ResultSet rs) throws SQLException {
        UserDto user = new UserDto();
        user.setId(rs.getInt("id"));
        user.setLoginId(rs.getString("login_id"));
        user.setPassword(rs.getString("password"));
        user.setNickname(rs.getString("nickname"));
        user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return user;
    }
}
