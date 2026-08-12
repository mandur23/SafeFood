package com.safefood.view;

import com.safefood.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SignupController {

    @FXML private TextField loginIdField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField passwordConfirmField;
    @FXML private TextField nicknameField;

    private AuthService authService;
    private boolean isIdChecked = false; // 중복 확인 통과 여부

    @FXML
    public void initialize() {
        authService = new AuthService();

        // 사용자가 아이디를 다시 수정하면 중복 확인을 처음부터 다시 받도록 설정
        loginIdField.textProperty().addListener((observable, oldValue, newValue) -> {
            isIdChecked = false;
        });
    }

    /**
     * 중복 확인 버튼 클릭 시 실행
     */
    @FXML
    public void handleCheckDuplicateId() {
        String loginId = loginIdField.getText().trim();
        if (loginId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "입력 오류", "아이디를 입력해 주세요.");
            return;
        }

        if (authService.isLoginIdAvailable(loginId)) {
            showAlert(Alert.AlertType.INFORMATION, "확인 완료", "사용할 수 있는 아이디입니다.");
            isIdChecked = true;
        } else {
            showAlert(Alert.AlertType.WARNING, "중복 오류", "이미 사용 중인 아이디입니다.");
            isIdChecked = false;
        }
    }

    /**
     * 회원가입 완료 버튼 클릭 시 실행
     */
    @FXML
    public void handleSignup() {
        String loginId = loginIdField.getText().trim();
        String password = passwordField.getText();
        String passwordConfirm = passwordConfirmField.getText();
        String nickname = nicknameField.getText().trim();

        // 1. 필수 입력 검증
        if (loginId.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty() || nickname.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "입력 누락", "모든 항목을 입력해 주세요.");
            return;
        }

        // 2. 중복 확인 여부 검증
        if (!isIdChecked) {
            showAlert(Alert.AlertType.WARNING, "확인 필요", "아이디 중복 확인을 먼저 해 주세요.");
            return;
        }

        // 3. 비밀번호 일치 검증
        if (!password.equals(passwordConfirm)) {
            showAlert(Alert.AlertType.WARNING, "입력 오류", "비밀번호가 일치하지 않습니다.");
            return;
        }

        // 4. 회원가입 처리
        boolean isSuccess = authService.register(loginId, password, nickname);
        if (isSuccess) {
            showAlert(Alert.AlertType.INFORMATION, "가입 성공", "회원가입이 완료되었습니다.");
            closeWindow(); // 가입 완료 후 창 닫기
        } else {
            showAlert(Alert.AlertType.ERROR, "시스템 오류", "회원가입 처리 중 문제가 발생했습니다.");
        }
    }

    /**
     * 화면에 알림창(Alert)을 띄우는 공통 메서드
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 현재 창 닫기
     */
    private void closeWindow() {
        Stage stage = (Stage) loginIdField.getScene().getWindow();
        stage.close();
    }
}