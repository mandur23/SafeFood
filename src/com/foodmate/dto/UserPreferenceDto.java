package com.foodmate.dto;

public class UserPreferenceDto {
    private int userId;
    private Integer spicyLevel;
    private Integer priceMin;
    private Integer priceMax;
    private Integer maxDistance;

    public UserPreferenceDto() {
    }

    public UserPreferenceDto(int userId) {
        this.userId = userId;
    }

    public UserPreferenceDto(int userId, Integer spicyLevel, Integer priceMin, Integer priceMax, Integer maxDistance) {
        this.userId = userId;
        this.spicyLevel = spicyLevel;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.maxDistance = maxDistance;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Integer getSpicyLevel() {
        return spicyLevel;
    }

    public void setSpicyLevel(Integer spicyLevel) {
        this.spicyLevel = spicyLevel;
    }

    public Integer getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(Integer priceMin) {
        this.priceMin = priceMin;
    }

    public Integer getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(Integer priceMax) {
        this.priceMax = priceMax;
    }

    public Integer getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(Integer maxDistance) {
        this.maxDistance = maxDistance;
    }

    @Override
    public String toString() {
        return "UserPreferenceDto{userId=" + userId
                + ", spicyLevel=" + spicyLevel
                + ", price=" + priceMin + "~" + priceMax
                + ", maxDistance=" + maxDistance + "}";
    }
}
