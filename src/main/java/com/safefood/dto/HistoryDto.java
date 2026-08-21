package com.safefood.dto;

public class HistoryDto {
    private String date;
    private String type;
    private String menu;
    private String restaurant;
    private String note;
    private int menuId;
    private int restaurantId;
    private int historyId;
    private int feedbackId;

    public HistoryDto(String date, String type, String menu, String restaurant, String note, int menuId, int restaurantId, int historyId, int feedbackId) {
        this.date = date;
        this.type = type;
        this.menu = menu;
        this.restaurant = restaurant;
        this.note = note;
        this.menuId = menuId;
        this.restaurantId = restaurantId;
        this.historyId = historyId;
        this.feedbackId = feedbackId;
    }

    public String getType() { return type; }
    public String getDate() { return date; }
    public String getMenu() { return menu; }
    public String getRestaurant() { return restaurant; }
    public String getNote() { return note; }
    public int getMenuId() { return menuId; }
    public int getRestaurantId() { return restaurantId; }
    public int getHistoryId() { return historyId; }
    public int getFeedbackId() { return feedbackId; }

}
