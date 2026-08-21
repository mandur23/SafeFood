package com.safefood.dto;

public class PreferenceDto {
    private int spicyLevel;
    private int priceMax;
    private int MaxDistance;

    public PreferenceDto(int spicyLevel, int priceMin, int priceMax, int maxDistance){
        this.spicyLevel = spicyLevel;
        this.priceMax = priceMax;
        this.MaxDistance = maxDistance;
    }

    public int getSpicyLevel() {
        return spicyLevel;
    }
    public int getPriceMax() {
        return priceMax;
    }
    public int getMaxDistance() {
        return MaxDistance;
    }

    public void setMaxDistance(int maxDistance) { MaxDistance = maxDistance; }
    public void setPriceMax(int priceMax) { this.priceMax = priceMax; }
    public void setSpicyLevel(int spicyLevel) { this.spicyLevel = spicyLevel; }
}
