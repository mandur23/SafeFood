package com.safefood.dto;

public class PreferenceDto {
    private int spicyLevel;
    private int priceMin;
    private int priceMax;
    private int MaxDistance;

    public PreferenceDto(int spicyLevel, int priceMin, int priceMax, int maxDistance){
        this.spicyLevel = spicyLevel;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.MaxDistance = maxDistance;
    }

    public int getSpicyLevel() {
        return spicyLevel;
    }

    public int getPriceMin() {
        return priceMin;
    }

    public int getPriceMax() {
        return priceMax;
    }

    public int getMaxDistance() {
        return MaxDistance;
    }
}
