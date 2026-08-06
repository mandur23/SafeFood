package com.foodmate.dto;

public enum RiskLevel {
    CONTAINS("들어 있음"),
    POSSIBLE("들어 있을 수 있음"),
    UNKNOWN("알 수 없음");

    private final String label;

    RiskLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static RiskLevel from(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
