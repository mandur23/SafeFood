package com.safefood.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * [ SafeFood - 메뉴 DAO ]
 * DB의 menu 테이블 조회 담당
 */
public class MenuDao {

    /**
     * 추천 후보 메뉴 DTO
     */
    public static class MenuDto {
        private int id;
        private int restaurantId;
        private String name;
        private int price;
        private String category;
        private int spicyLevel;

        public MenuDto(int id, int restaurantId, String name, int price, String category, int spicyLevel) {
            this.id = id;
            this.restaurantId = restaurantId;
            this.name = name;
            this.price = price;
            this.category = category;
            this.spicyLevel = spicyLevel;
        }

        public int getId() { return id; }
        public int getRestaurantId() { return restaurantId; }
        public String getName() { return name; }
        public int getPrice() { return price; }
        public String getCategory() { return category; }
        public int getSpicyLevel() { return spicyLevel; }
    }

    /**
     * 전체 추천 후보 메뉴 목록 조회
     */
    public List<MenuDto> getAllCandidateMenus() {
        List<MenuDto> menuList = new ArrayList<>();
        String sql = "SELECT id, restaurant_id, name, price, category, spicy_level FROM menu";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                menuList.add(new MenuDto(
                        rs.getInt("id"),
                        rs.getInt("restaurant_id"),
                        rs.getString("name"),
                        rs.getInt("price"),
                        rs.getString("category"),
                        rs.getInt("spicy_level")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return menuList;
    }
}