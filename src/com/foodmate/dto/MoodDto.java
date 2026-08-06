package com.foodmate.dto;

public class MoodDto {
    private int id;
    private String name;

    public MoodDto() {
    }

    public MoodDto(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MoodDto{id=" + id + ", name='" + name + "'}";
    }
}
