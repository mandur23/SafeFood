package com.foodmate.dao;

import com.foodmate.dto.AllergyDto;
import com.foodmate.dto.DiningGroupDto;
import com.foodmate.dto.GroupCandidateDto;
import com.foodmate.dto.GroupMemberDto;
import com.foodmate.dto.GroupStatus;
import com.foodmate.dto.GroupVoteDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GroupDao {
    private static final String GROUP_COLUMNS = "id, name, owner_id, invite_code, status, created_at";
    private static final String MEMBER_COLUMNS =
            "id, group_id, user_id, guest_name, latitude, longitude, joined_at";

    public int insertGroup(DiningGroupDto group) throws SQLException {
        String sql = "INSERT INTO dining_group (name, owner_id, invite_code, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, group.getName());
            ps.setInt(2, group.getOwnerId());
            ps.setString(3, group.getInviteCode());
            ps.setString(4, (group.getStatus() == null ? GroupStatus.OPEN : group.getStatus()).name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    group.setId(keys.getInt(1));
                }
            }
            return group.getId();
        }
    }

    public DiningGroupDto findGroupById(int groupId) throws SQLException {
        String sql = "SELECT " + GROUP_COLUMNS + " FROM dining_group WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapGroup(rs) : null;
            }
        }
    }

    public DiningGroupDto findGroupByInviteCode(String inviteCode) throws SQLException {
        String sql = "SELECT " + GROUP_COLUMNS + " FROM dining_group WHERE invite_code = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inviteCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapGroup(rs) : null;
            }
        }
    }

    public boolean existsInviteCode(String inviteCode) throws SQLException {
        String sql = "SELECT 1 FROM dining_group WHERE invite_code = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inviteCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<DiningGroupDto> findGroupsByOwner(int ownerId) throws SQLException {
        String sql = "SELECT " + GROUP_COLUMNS + " FROM dining_group WHERE owner_id = ?"
                + " ORDER BY created_at DESC";
        List<DiningGroupDto> groups = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapGroup(rs));
                }
            }
        }
        return groups;
    }

    public boolean updateStatus(int groupId, GroupStatus status) throws SQLException {
        String sql = "UPDATE dining_group SET status = ? WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, groupId);
            return ps.executeUpdate() > 0;
        }
    }

    public int insertMember(GroupMemberDto member) throws SQLException {
        String sql = "INSERT INTO group_member (group_id, user_id, guest_name, latitude, longitude)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, member.getGroupId());
            ps.setObject(2, member.getUserId());
            ps.setString(3, member.getGuestName());
            ps.setObject(4, member.getLatitude());
            ps.setObject(5, member.getLongitude());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    member.setId(keys.getInt(1));
                }
            }
            return member.getId();
        }
    }

    public List<GroupMemberDto> findMembers(int groupId) throws SQLException {
        String sql = "SELECT gm.id, gm.group_id, gm.user_id, gm.guest_name,"
                + " gm.latitude, gm.longitude, gm.joined_at, u.nickname"
                + " FROM group_member gm"
                + " LEFT JOIN `user` u ON u.id = gm.user_id"
                + " WHERE gm.group_id = ? ORDER BY gm.joined_at, gm.id";
        List<GroupMemberDto> members = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GroupMemberDto member = mapMember(rs);
                    member.setNickname(rs.getString("nickname"));
                    members.add(member);
                }
            }
        }
        return members;
    }

    public GroupMemberDto findMemberById(int memberId) throws SQLException {
        String sql = "SELECT " + MEMBER_COLUMNS + " FROM group_member WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapMember(rs) : null;
            }
        }
    }

    public boolean updateMemberLocation(int memberId, double latitude, double longitude) throws SQLException {
        String sql = "UPDATE group_member SET latitude = ?, longitude = ? WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, latitude);
            ps.setDouble(2, longitude);
            ps.setInt(3, memberId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteMember(int memberId) throws SQLException {
        try (Connection conn = DbConnection.get()) {
            conn.setAutoCommit(false);
            try (PreparedStatement votes = conn.prepareStatement(
                         "DELETE FROM group_vote WHERE member_id = ?");
                 PreparedStatement allergies = conn.prepareStatement(
                         "DELETE FROM group_member_allergy WHERE member_id = ?");
                 PreparedStatement member = conn.prepareStatement(
                         "DELETE FROM group_member WHERE id = ?")) {
                votes.setInt(1, memberId);
                votes.executeUpdate();
                allergies.setInt(1, memberId);
                allergies.executeUpdate();
                member.setInt(1, memberId);
                int deleted = member.executeUpdate();

                conn.commit();
                return deleted > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void replaceMemberAllergies(int memberId, Collection<Integer> allergyIds) throws SQLException {
        String delete = "DELETE FROM group_member_allergy WHERE member_id = ?";
        String insert = "INSERT INTO group_member_allergy (member_id, allergy_id) VALUES (?, ?)";
        try (Connection conn = DbConnection.get()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deletePs = conn.prepareStatement(delete);
                 PreparedStatement insertPs = conn.prepareStatement(insert)) {
                deletePs.setInt(1, memberId);
                deletePs.executeUpdate();

                if (allergyIds != null) {
                    for (Integer allergyId : allergyIds) {
                        insertPs.setInt(1, memberId);
                        insertPs.setInt(2, allergyId);
                        insertPs.addBatch();
                    }
                    insertPs.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<AllergyDto> findMemberAllergies(int memberId) throws SQLException {
        String sql = "SELECT a.id, a.name FROM group_member_allergy gma"
                + " JOIN allergy a ON a.id = gma.allergy_id"
                + " WHERE gma.member_id = ? ORDER BY a.name";
        List<AllergyDto> allergies = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    allergies.add(new AllergyDto(rs.getInt("id"), rs.getString("name")));
                }
            }
        }
        return allergies;
    }

    public Set<Integer> findGroupAllergyIds(int groupId) throws SQLException {
        String sql = "SELECT gma.allergy_id FROM group_member_allergy gma"
                + " JOIN group_member gm ON gm.id = gma.member_id"
                + " WHERE gm.group_id = ?"
                + " UNION"
                + " SELECT ua.allergy_id FROM user_allergy ua"
                + " JOIN group_member gm ON gm.user_id = ua.user_id"
                + " WHERE gm.group_id = ?";
        Set<Integer> allergyIds = new LinkedHashSet<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    allergyIds.add(rs.getInt(1));
                }
            }
        }
        return allergyIds;
    }

    public int insertCandidate(GroupCandidateDto candidate) throws SQLException {
        String sql = "INSERT INTO group_candidate (group_id, menu_id, score, reason) VALUES (?, ?, ?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, candidate.getGroupId());
            ps.setInt(2, candidate.getMenuId());
            ps.setObject(3, candidate.getScore());
            ps.setString(4, candidate.getReason());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    candidate.setId(keys.getInt(1));
                }
            }
            return candidate.getId();
        }
    }

    public List<GroupCandidateDto> findCandidates(int groupId) throws SQLException {
        String sql = "SELECT c.id, c.group_id, c.menu_id, c.score, c.reason,"
                + " m.name AS menu_name, COUNT(v.member_id) AS vote_count"
                + " FROM group_candidate c"
                + " JOIN menu m ON m.id = c.menu_id"
                + " LEFT JOIN group_vote v ON v.candidate_id = c.id"
                + " WHERE c.group_id = ?"
                + " GROUP BY c.id, c.group_id, c.menu_id, c.score, c.reason, m.name"
                + " ORDER BY vote_count DESC, c.score DESC";
        List<GroupCandidateDto> candidates = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GroupCandidateDto candidate = new GroupCandidateDto();
                    candidate.setId(rs.getInt("id"));
                    candidate.setGroupId(rs.getInt("group_id"));
                    candidate.setMenuId(rs.getInt("menu_id"));
                    candidate.setScore(rs.getObject("score", Integer.class));
                    candidate.setReason(rs.getString("reason"));
                    candidate.setMenuName(rs.getString("menu_name"));
                    candidate.setVoteCount(rs.getInt("vote_count"));
                    candidates.add(candidate);
                }
            }
        }
        return candidates;
    }

    public void deleteCandidates(int groupId) throws SQLException {
        String deleteVotes = "DELETE v FROM group_vote v"
                + " JOIN group_candidate c ON c.id = v.candidate_id"
                + " WHERE c.group_id = ?";
        String deleteCandidates = "DELETE FROM group_candidate WHERE group_id = ?";
        try (Connection conn = DbConnection.get()) {
            conn.setAutoCommit(false);
            try (PreparedStatement votesPs = conn.prepareStatement(deleteVotes);
                 PreparedStatement candidatesPs = conn.prepareStatement(deleteCandidates)) {
                votesPs.setInt(1, groupId);
                votesPs.executeUpdate();
                candidatesPs.setInt(1, groupId);
                candidatesPs.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public boolean insertVote(int candidateId, int memberId) throws SQLException {
        String sql = "INSERT IGNORE INTO group_vote (candidate_id, member_id) VALUES (?, ?)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            ps.setInt(2, memberId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteVote(int candidateId, int memberId) throws SQLException {
        String sql = "DELETE FROM group_vote WHERE candidate_id = ? AND member_id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            ps.setInt(2, memberId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<GroupVoteDto> findVotes(int groupId) throws SQLException {
        String sql = "SELECT v.candidate_id, v.member_id, v.voted_at FROM group_vote v"
                + " JOIN group_candidate c ON c.id = v.candidate_id"
                + " WHERE c.group_id = ? ORDER BY v.voted_at";
        List<GroupVoteDto> votes = new ArrayList<>();
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GroupVoteDto vote = new GroupVoteDto();
                    vote.setCandidateId(rs.getInt("candidate_id"));
                    vote.setMemberId(rs.getInt("member_id"));
                    vote.setVotedAt(rs.getObject("voted_at", LocalDateTime.class));
                    votes.add(vote);
                }
            }
        }
        return votes;
    }

    public int countVotedMembers(int groupId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT v.member_id) FROM group_vote v"
                + " JOIN group_candidate c ON c.id = v.candidate_id"
                + " WHERE c.group_id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static DiningGroupDto mapGroup(ResultSet rs) throws SQLException {
        DiningGroupDto group = new DiningGroupDto();
        group.setId(rs.getInt("id"));
        group.setName(rs.getString("name"));
        group.setOwnerId(rs.getInt("owner_id"));
        group.setInviteCode(rs.getString("invite_code"));
        group.setStatus(GroupStatus.from(rs.getString("status")));
        group.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return group;
    }

    private static GroupMemberDto mapMember(ResultSet rs) throws SQLException {
        GroupMemberDto member = new GroupMemberDto();
        member.setId(rs.getInt("id"));
        member.setGroupId(rs.getInt("group_id"));
        member.setUserId(rs.getObject("user_id", Integer.class));
        member.setGuestName(rs.getString("guest_name"));
        member.setLatitude(rs.getObject("latitude", Double.class));
        member.setLongitude(rs.getObject("longitude", Double.class));
        member.setJoinedAt(rs.getObject("joined_at", LocalDateTime.class));
        return member;
    }
}
