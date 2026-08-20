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
    /**
     * 식사 및 추천 이력을 DB에 저장
     * @param userId 회원 식별자
     * @param menuId 대상 메뉴 식별자
     * @param groupId 그룹 ID (혼자일 때는 null을 받아야 하므로 int가 아닌 Integer 클래스 사용!)
     * @param type 상태값 ("RECOMMENDED", "EATEN", "VIEWED", "BLOCKED")
     */

    public boolean insertHistory(int userId, int menuId, Integer groupId, String type){
        String sql = "INSERT INTO history (user_id, menu_id, group_id, type) VALUES (?, ?, ?, ?)";

        // 자원 정리는 try-with-resources로 닫기
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // SQl 인젝션 방지 ? 바인딩 사용
            pstmt.setInt(1, userId);
            pstmt.setInt(2, menuId);

            // group_id가 null인지 검사
            if (groupId != null) {
                pstmt.setInt(3, groupId);
            } else{
                // 혼자 먹었을 때 DB에 null
                pstmt.setNull(3, Types.INTEGER);
            }

            pstmt.setString(4, type);

            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // 회원 히스토리 꺼내기
    public List<HistoryDto> findHistoriesByUserId(int userId) {
        String sql = "SELECT DATE_FORMAT(h.created_at, '%Y-%m-%d') as date, " +
                "h.type, m.name as menu_name, r.name as restaurant_name " +
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

                    // DTO 담아서 리스트 추가
                    list.add(new HistoryDto(date, type, menu, restaurant, ""));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            return list;
        }
    }
}
