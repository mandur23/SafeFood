package com.foodmate.dto;

public enum HistoryType {
    RECOMMENDED("추천받음"),
    EATEN("먹음"),
    VIEWED("조회함");

    private final String label;

    HistoryType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static HistoryType from(String value) {
        if (value == null) {
            return VIEWED;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return VIEWED;
        }
    }
}
