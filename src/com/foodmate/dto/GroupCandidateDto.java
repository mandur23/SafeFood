package com.foodmate.dto;

public class GroupCandidateDto {
    private int id;
    private int groupId;
    private int menuId;
    private Integer score;
    private String reason;
    private int voteCount;
    private String menuName;

    public GroupCandidateDto() {
    }

    public GroupCandidateDto(int groupId, int menuId, Integer score, String reason) {
        this.groupId = groupId;
        this.menuId = menuId;
        this.score = score;
        this.reason = reason;
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

    public int getMenuId() {
        return menuId;
    }

    public void setMenuId(int menuId) {
        this.menuId = menuId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    @Override
    public String toString() {
        return "GroupCandidateDto{id=" + id + ", groupId=" + groupId
                + ", menu=" + (menuName != null ? menuName : menuId)
                + ", score=" + score + ", voteCount=" + voteCount + "}";
    }
}
