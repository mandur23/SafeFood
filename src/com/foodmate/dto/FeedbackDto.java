package com.foodmate.dto;

import java.time.LocalDateTime;

public class FeedbackDto {
    private int id;
    private int userId;
    private int menuId;
    private Boolean liked;
    private Integer rating;
    private LocalDateTime createdAt;

    public FeedbackDto() {
    }

    public FeedbackDto(int userId, int menuId, Boolean liked, Integer rating) {
        this.userId = userId;
        this.menuId = menuId;
        this.liked = liked;
        this.rating = rating;
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

    public int getMenuId() {
        return menuId;
    }

    public void setMenuId(int menuId) {
        this.menuId = menuId;
    }

    public Boolean getLiked() {
        return liked;
    }

    public void setLiked(Boolean liked) {
        this.liked = liked;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FeedbackDto{id=" + id + ", userId=" + userId + ", menuId=" + menuId
                + ", liked=" + liked + ", rating=" + rating + "}";
    }
}
