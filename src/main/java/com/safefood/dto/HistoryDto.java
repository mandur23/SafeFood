package com.safefood.dto;

public class HistoryDto {
    private String date;
    private String type;
    private String menu;
    private String restaurant;
    private String note;

    public HistoryDto(String date, String type, String menu, String restaurant, String note) {
        this.date = date;
        this.type = type;
        this.menu = menu;
        this.restaurant = restaurant;
        this.note = note;
    }

    public String getType() { return type; }
    public String getDate() { return date; }
    public String getMenu() { return menu; }
    public String getRestaurant() { return restaurant; }
    public String getNote() { return note; }

}
