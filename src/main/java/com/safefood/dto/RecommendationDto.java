package com.safefood.dto;

public class RecommendationDto {
    public enum Safety {
        SAFE("SAFE (알레르기 안전)", "safe"),
        POSSIBLE("POSSIBLE (혼입 가능)", "possible"),
        CONTAINS("CONTAINS (알레르기 원료 포함)", "contains");

        public final String label;
        public final String styleClass;

        Safety(String label, String styleClass) {
            this.label = label;
            this.styleClass = styleClass;
        }
    }

    private int menuId;
    private int restaurantId;
    private int rank;
    private int score;
    private String menu;
    private String restaurant;
    private Safety safety;
    private int spicyLevel;
    private String reason;
    private String address;
    private String hours;
    private double rating;
    private boolean blocked;
    private String alternative;

    public RecommendationDto(int menuId, int restaurantId, int rank, int score, String menu, String restaurant, Safety safety, int spicyLevel, String reason, String address, String hours, double rating, boolean blocked, String alternative) {
        this.menuId = menuId;
        this.restaurantId = restaurantId;
        this.rank = rank;
        this.score = score;
        this.menu = menu;
        this.restaurant = restaurant;
        this.safety = safety;
        this.spicyLevel = spicyLevel;
        this.reason = reason;
        this.address = address;
        this.hours = hours;
        this.rating = rating;
        this.blocked = blocked;
        this.alternative = alternative;
    }

    public int getMenuId() { return menuId; }
    public int getRestaurantId() { return restaurantId; }
    public int getRank() { return rank; }
    public int getScore() { return score; }
    public String getMenu() { return menu; }
    public String getRestaurant() { return restaurant; }
    public Safety getSafety() { return safety; }
    public int getSpicyLevel() { return spicyLevel; }
    public String getReason() { return reason; }
    public String getAddress() { return address; }
    public String getHours() { return hours; }
    public double getRating() { return rating; }
    public boolean isBlocked() { return blocked; }
    public String getAlternative() { return alternative; }
}
