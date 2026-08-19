package com.safefood.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * [ SafeFood - 그룹 DAO ]
 * DB의 dining_group, group_member, group_member_allergy, user_preference 테이블 데이터 조회 담당
 */
public class GroupDao {

    /**
     * 特定 그룹(groupId)에 속한 모든 참여자의 member_id 목록 조회
     */
    public List<Integer> getGroupMemberIds(int groupId) {
        List<Integer> memberIds = new ArrayList<>();
        String sql = "SELECT id FROM group_member WHERE group_id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    memberIds.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return memberIds;
    }

    /**
     * 特定 그룹(groupId) 참여자들의 알레르기 ID 스냅샷 리스트 목록 조회 (합집합 병합용)
     */
    public List<List<Integer>> getGroupMembersAllergies(int groupId) {
        List<List<Integer>> membersAllergies = new ArrayList<>();
        List<Integer> memberIds = getGroupMemberIds(groupId);

        String sql = "SELECT allergy_id FROM group_member_allergy WHERE member_id = ?";

        for (int memberId : memberIds) {
            List<Integer> singleMemberAllergies = new ArrayList<>();
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, memberId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        singleMemberAllergies.add(rs.getInt("allergy_id"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            membersAllergies.add(singleMemberAllergies);
        }
        return membersAllergies;
    }

    /**
     * 特定 그룹(groupId) 회원 참여자들의 매운맛 선호도(spicy_level) 목록 조회 (최솟값 병합용)
     */
    public List<Integer> getGroupMembersSpicyLevels(int groupId) {
        List<Integer> spicyLevels = new ArrayList<>();
        String sql = "SELECT up.spicy_level " +
                "FROM group_member gm " +
                "JOIN user_preference up ON gm.user_id = up.user_id " +
                "WHERE gm.group_id = ? AND gm.user_id IS NOT NULL";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    spicyLevels.add(rs.getInt("spicy_level"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return spicyLevels;
    }
}