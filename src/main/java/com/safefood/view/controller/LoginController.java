package com.safefood.view.controller;

import com.safefood.dto.UserDto;
import com.safefood.network.GroupSession;
import com.safefood.service.AuthService;
import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class LoginController {

    @FXML private ToggleGroup tabGroup;
    @FXML private ToggleButton memberTab;
    @FXML private VBox idBox;
    @FXML private TextField idField;
    @FXML private VBox passwordBox;
    @FXML private PasswordField passwordField;
    @FXML private VBox nameBox;
    @FXML private TextField nameField;
    @FXML private Label guestHintLabel;
    @FXML private Button signUpButton;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        tabGroup.selectedToggleProperty().addListener((observable, before, after) -> {
            if (after == null && before != null) {
                tabGroup.selectToggle(before);
            }
        });

        memberTab.selectedProperty().addListener((observable, before, isMember) -> {
            applyTabMode(isMember);
            Widgets.hideError(errorLabel);
        });
        applyTabMode(memberTab.isSelected());
    }

    private void applyTabMode(boolean isMember) {
        idBox.setVisible(isMember);
        idBox.setManaged(isMember);
        passwordBox.setVisible(isMember);
        passwordBox.setManaged(isMember);

        nameBox.setVisible(!isMember);
        nameBox.setManaged(!isMember);
        guestHintLabel.setVisible(!isMember);
        guestHintLabel.setManaged(!isMember);

        signUpButton.setDisable(!isMember);
    }

    @FXML
    private void handleLogin() {
        if (memberTab.isSelected()) {
            String loginId = idField.getText().trim();
            String password = passwordField.getText();

            if (loginId.isBlank() || password.isBlank()) {
                Widgets.showError(errorLabel, "아이디와 비밀번호를 입력해 주세요.");
                return;
            }

            UserDto user = authService.login(loginId, password);
            if (user == null) {
                Widgets.showError(errorLabel, "아이디 또는 비밀번호가 올바르지 않습니다.");
                return;
            }

            // Session에 정보 보관
            com.safefood.dto.Session.setCurrentUser(user);

            Widgets.hideError(errorLabel);
            // 그룹 참여(소켓 JOIN) 때 쓸 표시 이름
            GroupSession.get().setDisplayName(
                    user.getNickname() == null || user.getNickname().isBlank()
                            ? loginId : user.getNickname());
            GroupSession.get().setGuest(false);
            AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
        } else {
            if (nameField.getText().isBlank()) {
                Widgets.showError(errorLabel, "이름을 입력해 주세요.");
                return;
            }
            Widgets.hideError(errorLabel);
            GroupSession.get().setDisplayName(nameField.getText());
            GroupSession.get().setGuest(true);
            AppNav.dialog("같이 먹기 옵션 선택", "group-option.fxml");
        }
    }

    @FXML
    private void handleSignUp() {
        AppNav.show("회원가입", "signup.fxml");
    }
}
