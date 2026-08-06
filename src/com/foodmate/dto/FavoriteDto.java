package com.foodmate.dto;

import java.time.LocalDateTime;

public class FavoriteDto {
    private int id;
    private int userId;
    private Integer restaurantId;
    private Integer menuId;
    private LocalDateTime createdAt;

    public FavoriteDto() {
    }

    public static FavoriteDto ofRestaurant(int userId, int restaurantId) {
        FavoriteDto favorite = new FavoriteDto();
        favorite.userId = userId;
        favorite.restaurantId = restaurantId;
        return favorite;
    }

    public static FavoriteDto ofMenu(int userId, int menuId) {
        FavoriteDto favorite = new FavoriteDto();
        favorite.userId = userId;
        favorite.menuId = menuId;
        return favorite;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Integer getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Integer restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Integer getMenuId() {
        return menuId;
    }

    public void setMenuId(Integer menuId) {
        this.menuId = menuId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FavoriteDto{id=" + id + ", userId=" + userId
                + ", restaurantId=" + restaurantId + ", menuId=" + menuId + "}";
    }
}
