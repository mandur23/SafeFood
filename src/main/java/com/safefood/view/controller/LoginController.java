package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class LoginController {

    @FXML private ToggleGroup tabGroup;
    @FXML private ToggleButton memberTab;
    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private TextField nameField;
    @FXML private Button signUpButton;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {

        tabGroup.selectedToggleProperty().addListener((observable, before, after) -> {
            if (after == null && before != null) {
                tabGroup.selectToggle(before);
            }
        });

        memberTab.selectedProperty().addListener((observable, before, isMember) -> {
            idField.setDisable(!isMember);
            passwordField.setDisable(!isMember);
            signUpButton.setDisable(!isMember);
            nameField.setPromptText(isMember ? "표시할 이름을 입력하세요" : "게스트 이름을 입력하세요 (필수)");
            Widgets.hideError(errorLabel);
        });
    }

    @FXML
    private void handleLogin() {
        if (memberTab.isSelected()) {
            if (idField.getText().isBlank() || passwordField.getText().isBlank()) {
                Widgets.showError(errorLabel, "아이디와 비밀번호를 입력해 주세요.");
                return;
            }

            Widgets.hideError(errorLabel);
            AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
        } else {
            if (nameField.getText().isBlank()) {
                Widgets.showError(errorLabel, "이름을 입력해 주세요.");
                return;
            }
            Widgets.hideError(errorLabel);

            AppNav.dialog("같이 먹기 옵션 선택", "group-option.fxml");
        }
    }

    @FXML
    private void handleSignUp() {
        AppNav.show("회원가입", "signup.fxml");
    }
}
