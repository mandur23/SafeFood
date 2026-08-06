package com.foodmate.dao;

import com.foodmate.dto.RestaurantDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDao {
    private static final String COLUMNS =
            "id, name, category, address, phone, open_time, close_time, latitude, longitude";

    public RestaurantDto findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM restaurant WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs, false) : null;
            }
        }
    }

    public List<RestaurantDto> findAll() throws SQLException {
        return search(null, null, null, null);
    }

    public List<RestaurantDto> findByCategory(String category) throws SQLException {
        return search(category, null, null, null);
    }

    public List<RestaurantDto> findNearby(double latitude, double longitude, int radiusMeters)
            throws SQLException {
        return search(null, latitude, longitude, radiusMeters);
    }

    public List<RestaurantDto> search(String category, Double latitude, Double longitude, Integer radiusMeters)
            throws SQLException {
        boolean hasLocation = latitude != null && longitude != null;
        String distance = "ST_Distance_Sphere(POINT(longitude, latitude), POINT(?, ?))";

        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS);
        List<Object> params = new ArrayList<>();

        if (hasLocation) {
            sql.append(", ").append(distance).append(" AS distance_m");
            params.add(longitude);
            params.add(latitude);
        }
        sql.append(" FROM restaurant WHERE 1 = 1");

        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (hasLocation) {
            sql.append(" AND latitude IS NOT NULL AND longitude IS NOT NULL");
            if (radiusMeters != null) {
                sql.append(" AND ").append(distance).append(" <= ?");
                params.add(longitude);
                params.add(latitude);
                params.add(radiusMeters);
            }
        }
        sql.append(hasLocation ? " ORDER BY distance_m" : " ORDER BY name");

        List<RestaurantDto> restaurants = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    restaurants.add(map(rs, hasLocation));
                }
            }
        }
        return restaurants;
    }

    public List<String> findCategories() throws SQLException {
        String sql = "SELECT DISTINCT category FROM restaurant ORDER BY category";
        List<String> categories = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString(1));
            }
        }
        return categories;
    }

    public int insert(RestaurantDto restaurant) throws SQLException {
        String sql = "INSERT INTO restaurant"
                + " (name, category, address, phone, open_time, close_time, latitude, longitude)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, restaurant.getName());
            ps.setString(2, restaurant.getCategory());
            ps.setString(3, restaurant.getAddress());
            ps.setString(4, restaurant.getPhone());
            ps.setObject(5, restaurant.getOpenTime());
            ps.setObject(6, restaurant.getCloseTime());
            ps.setObject(7, restaurant.getLatitude());
            ps.setObject(8, restaurant.getLongitude());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    restaurant.setId(keys.getInt(1));
                }
            }
            return restaurant.getId();
        }
    }

    private static RestaurantDto map(ResultSet rs, boolean withDistance) throws SQLException {
        RestaurantDto restaurant = new RestaurantDto();
        restaurant.setId(rs.getInt("id"));
        restaurant.setName(rs.getString("name"));
        restaurant.setCategory(rs.getString("category"));
        restaurant.setAddress(rs.getString("address"));
        restaurant.setPhone(rs.getString("phone"));
        restaurant.setOpenTime(rs.getObject("open_time", LocalTime.class));
        restaurant.setCloseTime(rs.getObject("close_time", LocalTime.class));
        restaurant.setLatitude(rs.getObject("latitude", Double.class));
        restaurant.setLongitude(rs.getObject("longitude", Double.class));
        if (withDistance) {
            restaurant.setDistanceMeters(rs.getObject("distance_m", Double.class));
        }
        return restaurant;
    }
}
