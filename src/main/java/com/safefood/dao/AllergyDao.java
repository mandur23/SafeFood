package com.safefood.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [ SafeFood - 알레르기 DAO ]
 * DB의 user_allergy, menu_allergy 테이블 조회 담당
 */
public class AllergyDao {

    /**
     * 특정 회원의 알레르기 ID 목록 조회
     */
    public List<Integer> getUserAllergyIds(int userId) {
        List<Integer> allergyIds = new ArrayList<>();
        String sql = "SELECT allergy_id FROM user_allergy WHERE user_id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    allergyIds.add(rs.getInt("allergy_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allergyIds;
    }

    /**
     * 특정 회원의 알레르기 ID 및 심각도(severity) Map 조회
     */
    public Map<Integer, Integer> getUserSeverityMap(int userId) {
        Map<Integer, Integer> severityMap = new HashMap<>();
        String sql = "SELECT allergy_id, severity FROM user_allergy WHERE user_id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    severityMap.put(rs.getInt("allergy_id"), rs.getInt("severity"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return severityMap;
    }

    /**
     * 특정 메뉴의 특정 위험도(risk_level: CONTAINS / POSSIBLE) 알레르기 ID 목록 조회
     */
    public List<Integer> getMenuAllergiesByRiskLevel(int menuId, String riskLevel) {
        List<Integer> allergyIds = new ArrayList<>();
        String sql = "SELECT allergy_id FROM menu_allergy WHERE menu_id = ? AND risk_level = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, menuId);
            pstmt.setString(2, riskLevel);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    allergyIds.add(rs.getInt("allergy_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allergyIds;
    }
}