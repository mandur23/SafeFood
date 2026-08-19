package com.safefood.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OnboardingDao {
    public boolean insertAllergy(int userId, int allergyId, int severity) {
        String sql = "INSERT INTO user_allergy (user_id, allergy_id, severity) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, allergyId);
            pstmt.setInt(3, severity);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertPreference(int userId, int spicyLevel, int priceMax, int maxDistance){
        String sql = "INSERT INTO user_preference (user_id, spicy_level, price_min, price_max, max_distance) VALUES (?, ?, 0, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, spicyLevel);
            pstmt.setInt(3, priceMax);
            pstmt.setInt(4, maxDistance);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertCategory(int userId, String category) {
        String sql = "INSERT INTO user_category (user_id, category) VALUES (?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, category);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}