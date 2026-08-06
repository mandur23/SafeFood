package com.foodmate.dto;

import java.time.LocalDateTime;

public class DiningGroupDto {
    private int id;
    private String name;
    private int ownerId;
    private String inviteCode;
    private GroupStatus status = GroupStatus.OPEN;
    private LocalDateTime createdAt;

    public DiningGroupDto() {
    }

    public DiningGroupDto(String name, int ownerId, String inviteCode) {
        this.name = name;
        this.ownerId = ownerId;
        this.inviteCode = inviteCode;
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

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public void setStatus(GroupStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "DiningGroupDto{id=" + id + ", name='" + name + "', inviteCode='" + inviteCode
                + "', status=" + status + "}";
    }
}
