package com.safefood.dto;

public class FavoriteDto {
    private int favoriteId; // 찜 취소할때 사용
    private String restaurant;
    private String menu;
    private double rating;
    private int reviewCount;

    public FavoriteDto(int favoriteId, String restaurant, String menu, double rating, int reviewCount) {
        this.favoriteId = favoriteId;
        this.restaurant = restaurant;
        this.menu = menu;
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

    // GETTER
    public int getFavoriteId() { return favoriteId; }
    public String getRestaurant() { return restaurant == null ? "" : restaurant; }
    public String getMenu() { return menu == null ? "" : menu; }
    public double getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
}
