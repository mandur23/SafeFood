package com.safefood.dao;

import com.safefood.dto.RecommendationDto;
import com.safefood.dto.RecommendationDto.Safety;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RecommendationDao {
    public List<RecommendationDto> getRecommendationsFromDb() {
        String sql = "SELECT m.id as menu_id, r.id as restaurant_id, m.name as menu_name, r.name as restaurant_name, " +
                     "m.spicy_level, r.address, r.open_time, r.close_time, r.rating " +
                     "FROM menu m JOIN restaurant r ON m.restaurant_id = r.id LIMIT 5";
        
        List<RecommendationDto> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            int rank = 1;
            while (rs.next()) {
                String openTime = rs.getString("open_time") != null ? rs.getString("open_time").substring(0, 5) : "";
                String closeTime = rs.getString("close_time") != null ? rs.getString("close_time").substring(0, 5) : "";
                String hours = openTime + " ~ " + closeTime;
                
                list.add(new RecommendationDto(
                    rs.getInt("menu_id"), rs.getInt("restaurant_id"), rank, 100 - (rank * 2),
                    rs.getString("menu_name"), rs.getString("restaurant_name"),
                    Safety.SAFE, rs.getInt("spicy_level"),
                    "회원님의 취향과 일치해요!",
                    rs.getString("address"), hours, rs.getDouble("rating"), false, null
                ));
                rank++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
