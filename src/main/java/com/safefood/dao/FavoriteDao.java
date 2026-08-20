package com.safefood.dao;

import com.safefood.dto.FavoriteDto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class FavoriteDao {

    // 1. 찜 목록 꺼내오기 (SELECT)
    // 식당만 찜할 수도 있고, 메뉴만 찜할 수도 있어서 LEFT JOIN
    public List<FavoriteDto> findFavoritesByUserId(int userId) {
        String sql = "SELECT f.id, r.name as restaurant_name, m.name as menu_name, " +
                "r.rating, r.review_count " +
                "FROM favorite f " +
                "LEFT JOIN restaurant r ON f.restaurant_id = r.id " +
                "LEFT JOIN menu m ON f.menu_id = m.id " +
                "WHERE f.user_id = ? " +
                "ORDER BY f.created_at DESC";

        List<FavoriteDto> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new FavoriteDto(
                            rs.getInt("id"),
                            rs.getString("restaurant_name"),
                            rs.getString("menu_name"),
                            rs.getDouble("rating"),
                            rs.getInt("review_count")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. 찜하기 (INSERT)
    public boolean insertFavorite(int userId, Integer restaurantId, Integer
            menuId) {
        String sql = "INSERT INTO favorite (user_id, restaurant_id, menu_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);

            if (restaurantId != null) pstmt.setInt(2, restaurantId);
            else pstmt.setNull(2, Types.INTEGER);

            if (menuId != null) pstmt.setInt(3, menuId);
            else pstmt.setNull(3, Types.INTEGER);

            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 3. 찜 취소하기 (DELETE)
    public boolean deleteFavorite(int favoriteId) {
        String sql = "DELETE FROM favorite WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, favoriteId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. 메인 화면용 찜 취소하기 (유저, 식당, 메뉴 ID로 삭제)
    public boolean deleteFavoriteByMenu(int userId, Integer restaurantId, Integer menuId) {
        String sql = "DELETE FROM favorite WHERE user_id = ? AND restaurant_id = ? AND menu_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            
            if (restaurantId != null) pstmt.setInt(2, restaurantId);
            else pstmt.setNull(2, Types.INTEGER);
            
            if (menuId != null) pstmt.setInt(3, menuId);
            else pstmt.setNull(3, Types.INTEGER);

            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}