package com.safefood.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class FeedbackDao {
    // 반환값을 boolean에서 int(feedback_id)로 변경 (실패시 -1)
    public int insertFeedback(int userId, int menuId, boolean liked, int rating) {
        String sql = "INSERT INTO feedback (user_id, menu_id, liked, rating) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, menuId);
            pstmt.setBoolean(3, liked);
            pstmt.setInt(4, rating);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (java.sql.ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1); // 생성된 feedback_id 반환
                    }
                }
            }
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
