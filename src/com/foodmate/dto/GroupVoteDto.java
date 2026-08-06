package com.foodmate.dto;

import java.time.LocalDateTime;

public class GroupVoteDto {
    private int candidateId;
    private int memberId;
    private LocalDateTime votedAt;

    public GroupVoteDto() {
    }

    public GroupVoteDto(int candidateId, int memberId) {
        this.candidateId = candidateId;
        this.memberId = memberId;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(LocalDateTime votedAt) {
        this.votedAt = votedAt;
    }

    @Override
    public String toString() {
        return "GroupVoteDto{candidateId=" + candidateId + ", memberId=" + memberId + "}";
    }
}
