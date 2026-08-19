package com.safefood.dao;

import com.safefood.dto.PreferenceDto;
import com.safefood.view.DemoData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ProfileDao {

    // 내 취향 정보 가져오기
    public PreferenceDto getPreference(int userId){
        // 유저 맵기/최소가격/최대가격/최대거리 가져옴
        String sql = "SELECT spicy_level, price_min, price_max, max_distance FROM user_preference WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new PreferenceDto(
                            rs.getInt("spicy_level"), rs.getInt("price_min"),
                            rs.getInt("price_max"), rs.getInt("max_distance")
                    );
                }
            }
        }catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // 내 선호 음식 종류 가져오기(List 사용)
    public List<String> getCategories(int userId){
        List<String> list = new ArrayList<>();
        // 선택 카테고리 유저 카테고리에서 유저아이디에 맞는것.
        String sql = "SELECT category from user_category WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("category"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // 내 알레르기와 심각도 가져오기 (매칭을 위해 MAP 사용)
    public Map<String, Integer> getAllergies(int userId) {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT allergy_id, severity FROM user_allergy WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int allergyId = rs.getInt("allergy_id");
                    int  severity = rs.getInt("severity");

                    // DB의 allergy_id(int)를 텍스트로 변환
                    if(allergyId > 0 && allergyId <= DemoData.ALLERGIES.size()){
                        String allergyName = DemoData.ALLERGIES.get(allergyId - 1);
                        map.put(allergyName, severity);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }


    // 단일 취향 업데이트 (데이터가 없으면 새로 넣고, 있으면 덮어쓰기)
    public void updatePreference(int userId, int spicy_level, int priceMax, int maxDistance){
        String sql = "INSERT INTO user_preference (user_id, spicy_level, price_min, price_max, max_distance) " +
                     "VALUES (?, ?, 0, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE spicy_level = ?, price_max = ?, max_distance = ?";
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            // INSERT 용
            pstmt.setInt(1, userId);
            pstmt.setInt(2, spicy_level);
            pstmt.setInt(3, priceMax);
            pstmt.setInt(4, maxDistance);
            
            // UPDATE 용
            pstmt.setInt(5, spicy_level);
            pstmt.setInt(6, priceMax);
            pstmt.setInt(7, maxDistance);
            
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 카테고리 / 알레르기 초기화 ( DELETE )
    public void deleteCategory(int userId){
        String sql = "DELETE FROM user_category WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, userId); pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void deleteAllergies(int userId){
        String sql = "DELETE FROM user_allergy WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, userId); pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }


    // 카테고리 / 알레르기 다시 추가 ( INSERT )

    // 카테고리 추가
    public void insertCategory(int userId, String category){
        String  sql = "INSERT INTO user_category (user_id, category) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, userId); pstmt.setString(2, category);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 알레르기 추가
    public void insertAllergy(int userId, int allergyId, int severity){
        String sql = "INSERT INTO user_allergy (user_id, allergy_id, severity) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, userId); pstmt.setInt(2, allergyId);
            pstmt.setInt(3, severity); pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }



}
