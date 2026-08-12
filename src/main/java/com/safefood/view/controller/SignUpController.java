package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignUpController {

    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private TextField nicknameField;
    @FXML private Label checkResult;
    @FXML private Label errorLabel;

    private boolean idChecked;

    @FXML
    private void initialize() {
        idField.textProperty().addListener((observable, before, after) -> {
            idChecked = false;
            checkResult.setText("아이디 중복 확인을 해 주세요.");
        });
    }

    @FXML
    private void handleCheckId() {
        if (idField.getText().isBlank()) {
            Widgets.showError(errorLabel, "아이디를 입력해 주세요.");
            return;
        }
        Widgets.hideError(errorLabel);

        idChecked = true;
        checkResult.setText("사용할 수 있는 아이디입니다.");
    }

    @FXML
    private void handleSubmit() {
        if (idField.getText().isBlank() || passwordField.getText().isBlank()
                || nicknameField.getText().isBlank()) {
            Widgets.showError(errorLabel, "빈 칸을 모두 채워 주세요.");
            return;
        }
        if (!idChecked) {
            Widgets.showError(errorLabel, "아이디 중복 확인을 먼저 해 주세요.");
            return;
        }
        if (!passwordField.getText().equals(confirmField.getText())) {
            Widgets.showError(errorLabel, "비밀번호가 일치하지 않습니다.");
            return;
        }
        Widgets.hideError(errorLabel);

        AppNav.show("온보딩 설문", "onboarding.fxml");
    }

    @FXML
    private void handleCancel() {
        AppNav.show("로그인", "login.fxml");
    }
}
