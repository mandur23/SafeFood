package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class EditInfoController {

    @FXML private VBox root;
    @FXML private PasswordField currentPassword;
    @FXML private TextField nicknameField;
    @FXML private PasswordField newPassword;
    @FXML private Label errorLabel;

    @FXML
    private void handleCheckNickname() {
        String nickname = nicknameField.getText().trim();
        if (nicknameField.getText().isBlank()) {
            AppNav.warn("닉네임을 입력해 주세요.");
            return;
        }

        // 내 원래 닉네임과 똑같이 치고 중복방지 누르는 상황 방지
        if (nickname.equals(com.safefood.dto.Session.getCurrentUser().getNickname())) {
            AppNav.info("현재 사용 중인 닉네임 입니다.");
            return;
        }

        // 중복인지 확인 (서비스에 물어봄)
        com.safefood.service.AuthService authService = new com.safefood.service.AuthService();
        if(authService.isNicknameAvailable(nickname)){
            AppNav.info("사용할 수 있는 닉네임 입니다.");
        }
        else{
            AppNav.warn("이미 누군가 사용 중인 닉네임입니다. 다른 닉네임을 골라주세요.");
        }
    }

    @FXML
    private void handleSave() {
        String currentPw =  currentPassword.getText();
        String newPw = newPassword.getText();
        String newNick = nicknameField.getText().trim();

        if (currentPassword.getText().isBlank()) {
            Widgets.showError(errorLabel, "현재 비밀번호를 입력해 주세요.");
            return;
        }

        if(newNick.isBlank()){
            Widgets.showError(errorLabel, "닉네임을 입력해 주세요.");
            return;
        }

        com.safefood.service.AuthService authService = new com.safefood.service.AuthService();
        com.safefood.dto.UserDto me = com.safefood.dto.Session.getCurrentUser();

        // 비밀번호 확인
        if (!authService.verifyPassword(currentPw, me.getPassword())) {
            Widgets.showError(errorLabel, "현재 비밀번호가 틀렸습니다.");
            return;
        }

        // 닉네임 중복 검사
        if(!newNick.equals(me.getNickname()) && !authService.isNicknameAvailable(newNick)){
            Widgets.showError(errorLabel, "이미 사용중인 닉네임 입니다. 다른 닉네임을 사용해 주세요");
            return;
        }

        // 업데이트
        boolean success = authService.updateProfile(me.getId(), newPw, newNick, me.getPassword());
        if(success){
            Widgets.hideError(errorLabel);
            me.setNickname(newNick);
            if(!newPw.isBlank()){
                me.setPassword(newPw);
            }

            AppNav.close(root);
            AppNav.info("개인정보가 성공적으로 변경되었습니다.");
        }
        else{
            Widgets.showError(errorLabel, "저장 중 오류가 발생했습니다.");
        }
    }

    @FXML
    private void handleCancel() {
        AppNav.close(root);
    }
}
