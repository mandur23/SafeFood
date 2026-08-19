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
     * 로그인 아이디로 회원정보 조회
     * >> view가 DB를 알고싶을 때 service에게 물어보도록 함
     */

    public UserDto getUserInfo(String loginId){
        return userDao.findByLoginId(loginId);
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

    // 닉네임 사용 가능 여부 확인
    public boolean isNicknameAvailable(String nickname) {
        // Dao에서 찾아왔는데 null(아무도 안씀) 이면 true 반환
        return userDao.findByNickname(nickname) == null;
    }

    // 비밀번호 검증
    public boolean verifyPassword(String inputPassword, String savedHashedPassword) {
        // 사용자가 입력한 비밀번호를 암호화한 값과 DB에 있는 암호화 값이 같은지 비교
        return hashPassword(inputPassword).equals(savedHashedPassword);
    }

    // 업데이트
    public boolean updateProfile(int userId, String plainNewPassword, String newNickname, String existingHashedPassword) {
        String passwordToSave;

        // 사용자가 새 비밀번호 칸을 비웠다면 기존 비밀번호 유지
        if(existingHashedPassword == null || plainNewPassword == null || plainNewPassword.isBlank()) {
            passwordToSave = existingHashedPassword;
        }
        else{
            passwordToSave = hashPassword(plainNewPassword);
        }

        return userDao.updateUserInfo(userId, passwordToSave, newNickname);
    }
}