package com.safefood.service;

import com.safefood.dao.UserDao;
import com.safefood.dto.UserDto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthService {

    private final UserDao userDao;

    public AuthService() {
        this.userDao = new UserDao();
    }

    /**
     * 아이디 중복 확인
     * @return true: 사용 가능 (중복 없음), false: 이미 사용 중
     */
    public boolean isLoginIdAvailable(String loginId) {
        return userDao.findByLoginId(loginId) == null;
    }

    /**
     * 회원 가입 처리
     * 비밀번호를 암호화하여 DB에 저장합니다.
     */
    public boolean register(String loginId, String password, String nickname) {
        // 한 번 더 중복 검증
        if (!isLoginIdAvailable(loginId)) {
            return false;
        }

        String hashedPassword = hashPassword(password);
        UserDto newUser = new UserDto(loginId, hashedPassword, nickname);

        return userDao.insertUser(newUser);
    }

    /**
     * 로그인 처리
     * @return 성공 시 UserDto 반환, 실패 시 null 반환
     */
    public UserDto login(String loginId, String password) {
        UserDto user = userDao.findByLoginId(loginId);

        // 아이디가 존재하고, 암호화된 비밀번호가 일치하는지 확인
        if (user != null) {
            String hashedPassword = hashPassword(password);
            if (user.getPassword().equals(hashedPassword)) {
                return user; // 인증 성공
            }
        }
        return null; // 인증 실패 (아이디 없음 또는 비밀번호 불일치)
    }

    /**
     * 비밀번호 암호화 (SHA-256)
     */
    private String hashPassword(String plainPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(plainPassword.getBytes());
            byte[] byteData = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : byteData) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("비밀번호 암호화 중 시스템 오류가 발생했습니다.", e);
        }
    }
}