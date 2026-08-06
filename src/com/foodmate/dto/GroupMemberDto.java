package com.foodmate.dto;

import java.time.LocalDateTime;

public class GroupMemberDto {
    private int id;
    private int groupId;
    private Integer userId;
    private String guestName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime joinedAt;
    private String nickname;

    public GroupMemberDto() {
    }

    public static GroupMemberDto ofUser(int groupId, int userId) {
        GroupMemberDto member = new GroupMemberDto();
        member.groupId = groupId;
        member.userId = userId;
        return member;
    }

    public static GroupMemberDto ofGuest(int groupId, String guestName) {
        GroupMemberDto member = new GroupMemberDto();
        member.groupId = groupId;
        member.guestName = guestName;
        return member;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return "GroupMemberDto{id=" + id + ", groupId=" + groupId
                + ", userId=" + userId + ", guestName='" + guestName + "'}";
    }
}
