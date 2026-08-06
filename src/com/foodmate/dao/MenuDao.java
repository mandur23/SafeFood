package com.foodmate.dao;

import com.foodmate.dto.MenuAllergyDto;
import com.foodmate.dto.MenuDto;
import com.foodmate.dto.MoodDto;
import com.foodmate.dto.RiskLevel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MenuDao {
    private static final String COLUMNS = "id, restaurant_id, name, price, category, spicy_level, description";

    private static final String MENU_COLUMNS =
            "m.id, m.restaurant_id, m.name, m.price, m.category, m.spicy_level, m.description";

    private static final String JOIN_COLUMNS = MENU_COLUMNS + ", r.name AS restaurant_name";

    public MenuDto findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM menu WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public MenuDto findDetail(int id) throws SQLException {
        String sql = "SELECT " + JOIN_COLUMNS + " FROM menu m"
                + " JOIN restaurant r ON r.id = m.restaurant_id"
                + " WHERE m.id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapWithRestaurant(rs) : null;
            }
        }
    }

    public List<MenuDto> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM menu ORDER BY id";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return readMenus(ps);
        }
    }

    public List<MenuDto> findByRestaurant(int restaurantId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM menu WHERE restaurant_id = ? ORDER BY price";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            return readMenus(ps);
        }
    }

    public List<MenuDto> findByRestaurantIds(Collection<Integer> restaurantIds) throws SQLException {
        if (restaurantIds == null || restaurantIds.isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "SELECT " + JOIN_COLUMNS + " FROM menu m"
                + " JOIN restaurant r ON r.id = m.restaurant_id"
                + " WHERE m.restaurant_id IN (" + placeholders(restaurantIds.size()) + ")"
                + " ORDER BY m.restaurant_id, m.price";
        List<MenuDto> menus = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (Integer restaurantId : restaurantIds) {
                ps.setInt(index++, restaurantId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    menus.add(mapWithRestaurant(rs));
                }
            }
        }
        return menus;
    }

    public List<MenuDto> search(String category, Integer minPrice, Integer maxPrice, Integer maxSpicyLevel)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM menu WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (minPrice != null) {
            sql.append(" AND price >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price <= ?");
            params.add(maxPrice);
        }
        if (maxSpicyLevel != null) {
            sql.append(" AND (spicy_level IS NULL OR spicy_level <= ?)");
            params.add(maxSpicyLevel);
        }
        sql.append(" ORDER BY price");

        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return readMenus(ps);
        }
    }

    public List<MenuAllergyDto> findAllergies(int menuId) throws SQLException {
        String sql = "SELECT ma.menu_id, ma.allergy_id, ma.risk_level, a.name AS allergy_name"
                + " FROM menu_allergy ma"
                + " JOIN allergy a ON a.id = ma.allergy_id"
                + " WHERE ma.menu_id = ? ORDER BY a.name";
        List<MenuAllergyDto> allergies = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MenuAllergyDto menuAllergy = new MenuAllergyDto();
                    menuAllergy.setMenuId(rs.getInt("menu_id"));
                    menuAllergy.setAllergyId(rs.getInt("allergy_id"));
                    menuAllergy.setAllergyName(rs.getString("allergy_name"));
                    menuAllergy.setRiskLevel(RiskLevel.from(rs.getString("risk_level")));
                    allergies.add(menuAllergy);
                }
            }
        }
        return allergies;
    }

    public Set<Integer> findMenuIdsWithAllergies(Collection<Integer> allergyIds,
                                                 Collection<RiskLevel> riskLevels) throws SQLException {
        Set<Integer> menuIds = new LinkedHashSet<>();
        if (allergyIds == null || allergyIds.isEmpty() || riskLevels == null || riskLevels.isEmpty()) {
            return menuIds;
        }
        String sql = "SELECT DISTINCT menu_id FROM menu_allergy"
                + " WHERE allergy_id IN (" + placeholders(allergyIds.size()) + ")"
                + " AND risk_level IN (" + placeholders(riskLevels.size()) + ")";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (Integer allergyId : allergyIds) {
                ps.setInt(index++, allergyId);
            }
            for (RiskLevel riskLevel : riskLevels) {
                ps.setString(index++, riskLevel.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    menuIds.add(rs.getInt(1));
                }
            }
        }
        return menuIds;
    }

    public void addAllergy(int menuId, int allergyId, RiskLevel riskLevel) throws SQLException {
        String sql = "INSERT INTO menu_allergy (menu_id, allergy_id, risk_level) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE risk_level = VALUES(risk_level)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuId);
            ps.setInt(2, allergyId);
            ps.setString(3, (riskLevel == null ? RiskLevel.UNKNOWN : riskLevel).name());
            ps.executeUpdate();
        }
    }

    public List<MoodDto> findMoods(int menuId) throws SQLException {
        String sql = "SELECT m.id, m.name FROM menu_mood mm"
                + " JOIN mood m ON m.id = mm.mood_id"
                + " WHERE mm.menu_id = ? ORDER BY m.id";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuId);
            return readMoods(ps);
        }
    }

    public List<MenuDto> findByMood(int moodId) throws SQLException {
        String sql = "SELECT " + MENU_COLUMNS + " FROM menu m"
                + " JOIN menu_mood mm ON mm.menu_id = m.id"
                + " WHERE mm.mood_id = ? ORDER BY m.price";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, moodId);
            return readMenus(ps);
        }
    }

    public List<MoodDto> findAllMoods() throws SQLException {
        String sql = "SELECT id, name FROM mood ORDER BY id";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return readMoods(ps);
        }
    }

    public MoodDto findMoodByName(String name) throws SQLException {
        String sql = "SELECT id, name FROM mood WHERE name = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            List<MoodDto> found = readMoods(ps);
            return found.isEmpty() ? null : found.get(0);
        }
    }

    public void addMood(int menuId, int moodId) throws SQLException {
        String sql = "INSERT IGNORE INTO menu_mood (menu_id, mood_id) VALUES (?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuId);
            ps.setInt(2, moodId);
            ps.executeUpdate();
        }
    }

    public int insert(MenuDto menu) throws SQLException {
        String sql = "INSERT INTO menu (restaurant_id, name, price, category, spicy_level, description)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, menu.getRestaurantId());
            ps.setString(2, menu.getName());
            ps.setInt(3, menu.getPrice());
            ps.setString(4, menu.getCategory());
            ps.setObject(5, menu.getSpicyLevel());
            ps.setString(6, menu.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    menu.setId(keys.getInt(1));
                }
            }
            return menu.getId();
        }
    }

    private static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private static List<MenuDto> readMenus(PreparedStatement ps) throws SQLException {
        List<MenuDto> menus = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                menus.add(map(rs));
            }
        }
        return menus;
    }

    private static List<MoodDto> readMoods(PreparedStatement ps) throws SQLException {
        List<MoodDto> moods = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                moods.add(new MoodDto(rs.getInt("id"), rs.getString("name")));
            }
        }
        return moods;
    }

    private static MenuDto map(ResultSet rs) throws SQLException {
        MenuDto menu = new MenuDto();
        menu.setId(rs.getInt("id"));
        menu.setRestaurantId(rs.getInt("restaurant_id"));
        menu.setName(rs.getString("name"));
        menu.setPrice(rs.getInt("price"));
        menu.setCategory(rs.getString("category"));
        menu.setSpicyLevel(rs.getObject("spicy_level", Integer.class));
        menu.setDescription(rs.getString("description"));
        return menu;
    }

    private static MenuDto mapWithRestaurant(ResultSet rs) throws SQLException {
        MenuDto menu = map(rs);
        menu.setRestaurantName(rs.getString("restaurant_name"));
        return menu;
    }
}
