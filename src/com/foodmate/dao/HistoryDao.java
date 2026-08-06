package com.foodmate.dao;

import com.foodmate.dto.FavoriteDto;
import com.foodmate.dto.FeedbackDto;
import com.foodmate.dto.HistoryDto;
import com.foodmate.dto.HistoryType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HistoryDao {
    public int insertHistory(HistoryDto history) throws SQLException {
        String sql = "INSERT INTO history (user_id, menu_id, group_id, type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, history.getUserId());
            ps.setInt(2, history.getMenuId());
            ps.setObject(3, history.getGroupId());
            ps.setString(4, history.getType().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    history.setId(keys.getInt(1));
                }
            }
            return history.getId();
        }
    }

    public List<HistoryDto> findHistory(int userId, HistoryType type, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT h.id, h.user_id, h.menu_id, h.group_id, h.type, h.created_at, m.name AS menu_name"
                        + " FROM history h"
                        + " JOIN menu m ON m.id = h.menu_id"
                        + " WHERE h.user_id = ?");
        if (type != null) {
            sql.append(" AND h.type = ?");
        }
        sql.append(" ORDER BY h.created_at DESC, h.id DESC LIMIT ?");

        List<HistoryDto> histories = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;
            ps.setInt(index++, userId);
            if (type != null) {
                ps.setString(index++, type.name());
            }
            ps.setInt(index, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    histories.add(mapHistory(rs));
                }
            }
        }
        return histories;
    }

    public Set<Integer> findMenuIdsOn(int userId, LocalDate date, HistoryType type) throws SQLException {
        String sql = "SELECT DISTINCT menu_id FROM history"
                + " WHERE user_id = ? AND DATE(created_at) = ? AND type = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setObject(2, date);
            ps.setString(3, type.name());
            return readMenuIds(ps);
        }
    }

    public Set<Integer> findRecentMenuIds(int userId, int days, HistoryType type) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT menu_id FROM history"
                + " WHERE user_id = ? AND created_at >= NOW() - INTERVAL ? DAY");
        if (type != null) {
            sql.append(" AND type = ?");
        }
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, userId);
            ps.setInt(2, days);
            if (type != null) {
                ps.setString(3, type.name());
            }
            return readMenuIds(ps);
        }
    }

    public int insertFavorite(FavoriteDto favorite) throws SQLException {
        String sql = "INSERT INTO favorite (user_id, restaurant_id, menu_id) VALUES (?, ?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, favorite.getUserId());
            ps.setObject(2, favorite.getRestaurantId());
            ps.setObject(3, favorite.getMenuId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    favorite.setId(keys.getInt(1));
                }
            }
            return favorite.getId();
        }
    }

    public List<FavoriteDto> findFavorites(int userId) throws SQLException {
        String sql = "SELECT id, user_id, restaurant_id, menu_id, created_at FROM favorite"
                + " WHERE user_id = ? ORDER BY created_at DESC, id DESC";
        List<FavoriteDto> favorites = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FavoriteDto favorite = new FavoriteDto();
                    favorite.setId(rs.getInt("id"));
                    favorite.setUserId(rs.getInt("user_id"));
                    favorite.setRestaurantId(rs.getObject("restaurant_id", Integer.class));
                    favorite.setMenuId(rs.getObject("menu_id", Integer.class));
                    favorite.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                    favorites.add(favorite);
                }
            }
        }
        return favorites;
    }

    public boolean deleteFavorite(int favoriteId, int userId) throws SQLException {
        String sql = "DELETE FROM favorite WHERE id = ? AND user_id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, favoriteId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean existsFavoriteMenu(int userId, int menuId) throws SQLException {
        String sql = "SELECT 1 FROM favorite WHERE user_id = ? AND menu_id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, menuId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertFeedback(FeedbackDto feedback) throws SQLException {
        String sql = "INSERT INTO feedback (user_id, menu_id, liked, rating) VALUES (?, ?, ?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, feedback.getUserId());
            ps.setInt(2, feedback.getMenuId());
            ps.setObject(3, feedback.getLiked());
            ps.setObject(4, feedback.getRating());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    feedback.setId(keys.getInt(1));
                }
            }
            return feedback.getId();
        }
    }

    public List<FeedbackDto> findFeedbacks(int userId) throws SQLException {
        String sql = "SELECT " + FEEDBACK_COLUMNS + " FROM feedback"
                + " WHERE user_id = ? ORDER BY created_at DESC, id DESC";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return readFeedbacks(ps);
        }
    }

    public FeedbackDto findLatestFeedback(int userId, int menuId) throws SQLException {
        String sql = "SELECT " + FEEDBACK_COLUMNS + " FROM feedback"
                + " WHERE user_id = ? AND menu_id = ?"
                + " ORDER BY created_at DESC, id DESC LIMIT 1";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, menuId);
            List<FeedbackDto> found = readFeedbacks(ps);
            return found.isEmpty() ? null : found.get(0);
        }
    }

    public Set<Integer> findLikedMenuIds(int userId) throws SQLException {
        return findMenuIdsByLiked(userId, true);
    }

    public Set<Integer> findDislikedMenuIds(int userId) throws SQLException {
        return findMenuIdsByLiked(userId, false);
    }

    private static final String FEEDBACK_COLUMNS = "id, user_id, menu_id, liked, rating, created_at";

    private Set<Integer> findMenuIdsByLiked(int userId, boolean liked) throws SQLException {
        String sql = "SELECT DISTINCT menu_id FROM feedback WHERE user_id = ? AND liked = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setBoolean(2, liked);
            return readMenuIds(ps);
        }
    }

    private static Set<Integer> readMenuIds(PreparedStatement ps) throws SQLException {
        Set<Integer> menuIds = new LinkedHashSet<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                menuIds.add(rs.getInt(1));
            }
        }
        return menuIds;
    }

    private static List<FeedbackDto> readFeedbacks(PreparedStatement ps) throws SQLException {
        List<FeedbackDto> feedbacks = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FeedbackDto feedback = new FeedbackDto();
                feedback.setId(rs.getInt("id"));
                feedback.setUserId(rs.getInt("user_id"));
                feedback.setMenuId(rs.getInt("menu_id"));
                feedback.setLiked(rs.getObject("liked", Boolean.class));
                feedback.setRating(rs.getObject("rating", Integer.class));
                feedback.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                feedbacks.add(feedback);
            }
        }
        return feedbacks;
    }

    private static HistoryDto mapHistory(ResultSet rs) throws SQLException {
        HistoryDto history = new HistoryDto();
        history.setId(rs.getInt("id"));
        history.setUserId(rs.getInt("user_id"));
        history.setMenuId(rs.getInt("menu_id"));
        history.setGroupId(rs.getObject("group_id", Integer.class));
        history.setType(HistoryType.from(rs.getString("type")));
        history.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        history.setMenuName(rs.getString("menu_name"));
        return history;
    }
}
