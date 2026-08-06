package com.foodmate.dto;

public enum GroupStatus {
    OPEN("모집 중"),
    VOTING("투표 중"),
    CLOSED("종료");

    private final String label;

    GroupStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static GroupStatus from(String value) {
        if (value == null) {
            return OPEN;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OPEN;
        }
    }
}
