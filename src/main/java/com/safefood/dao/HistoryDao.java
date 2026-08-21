package com.safefood.dao;

import com.safefood.dto.HistoryDto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class HistoryDao {
    public boolean insertHistory(int userId, int menuId, Integer groupId, String type){
        String sql = "INSERT INTO history (user_id, menu_id, group_id, type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, menuId);
            if (groupId != null) {
                pstmt.setInt(3, groupId);
            } else{
                pstmt.setNull(3, Types.INTEGER);
            }
            pstmt.setString(4, type);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateFeedbackId(int historyId, int feedbackId) {
        String sql = "UPDATE history SET feedback_id = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, feedbackId);
            pstmt.setInt(2, historyId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<HistoryDto> findHistoriesByUserId(int userId) {
        String sql = "SELECT DATE_FORMAT(h.created_at, '%Y-%m-%d') as date, " +
                "h.type, m.name as menu_name, r.name as restaurant_name, " +
                "m.id as menu_id, r.id as restaurant_id, h.id as history_id, h.feedback_id " +
                "FROM history h " +
                "JOIN menu m ON h.menu_id = m.id " +
                "JOIN restaurant r ON m.restaurant_id = r.id " +
                "WHERE h.user_id = ? " +
                "ORDER BY h.created_at DESC";

        List<HistoryDto> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getString("date");
                    String type = rs.getString("type");
                    String menu = rs.getString("menu_name");
                    String restaurant = rs.getString("restaurant_name");
                    int menuId = rs.getInt("menu_id");
                    int restaurantId = rs.getInt("restaurant_id");
                    int historyId = rs.getInt("history_id");
                    int feedbackId = rs.getInt("feedback_id"); // NULL이면 0이 반환됨
                    list.add(new HistoryDto(date, type, menu, restaurant, "", menuId, restaurantId, historyId, feedbackId));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
