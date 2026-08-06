package com.foodmate.dto;

import java.time.LocalDateTime;

public class HistoryDto {
    private int id;
    private int userId;
    private int menuId;
    private Integer groupId;
    private HistoryType type;
    private LocalDateTime createdAt;
    private String menuName;

    public HistoryDto() {
    }

    public HistoryDto(int userId, int menuId, HistoryType type) {
        this.userId = userId;
        this.menuId = menuId;
        this.type = type;
    }

    public HistoryDto(int userId, int menuId, Integer groupId, HistoryType type) {
        this.userId = userId;
        this.menuId = menuId;
        this.groupId = groupId;
        this.type = type;
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

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public HistoryType getType() {
        return type;
    }

    public void setType(HistoryType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    @Override
    public String toString() {
        return "HistoryDto{id=" + id + ", userId=" + userId
                + ", menu=" + (menuName != null ? menuName : menuId)
                + ", type=" + type + ", createdAt=" + createdAt + "}";
    }
}
