package com.foodmate.dto;

public class MenuAllergyDto {
    private int menuId;
    private int allergyId;
    private String allergyName;
    private RiskLevel riskLevel;

    public MenuAllergyDto() {
    }

    public MenuAllergyDto(int menuId, int allergyId, RiskLevel riskLevel) {
        this.menuId = menuId;
        this.allergyId = allergyId;
        this.riskLevel = riskLevel;
    }

    public int getMenuId() {
        return menuId;
    }

    public void setMenuId(int menuId) {
        this.menuId = menuId;
    }

    public int getAllergyId() {
        return allergyId;
    }

    public void setAllergyId(int allergyId) {
        this.allergyId = allergyId;
    }

    public String getAllergyName() {
        return allergyName;
    }

    public void setAllergyName(String allergyName) {
        this.allergyName = allergyName;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    @Override
    public String toString() {
        return "MenuAllergyDto{menuId=" + menuId
                + ", allergy=" + (allergyName != null ? allergyName : allergyId)
                + ", riskLevel=" + riskLevel + "}";
    }
}
