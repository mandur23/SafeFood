package com.safefood.dao;

import com.safefood.dto.UserDto;
import java.sql.*;

public class UserDao {

    /**
     * 신규 회원 저장 (회원가입)
     * 비밀번호는 반드시 해시값으로 저장되어야 합니다.
     */
    public boolean insertUser(UserDto user) {
        String sql = "INSERT INTO `user` (login_id, password, nickname) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getLoginId());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getNickname());

            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 아이디로 회원 조회 (로그인 및 아이디 중복 확인용)
     */
    public UserDto findByLoginId(String loginId) {
        String sql = "SELECT id, login_id, password, nickname, created_at FROM `user` WHERE login_id = ?";
        UserDto user = null;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, loginId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = new UserDto();
                    user.setId(rs.getInt("id"));
                    user.setLoginId(rs.getString("login_id"));
                    user.setPassword(rs.getString("password"));
                    user.setNickname(rs.getString("nickname"));
                    user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }
}