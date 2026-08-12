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
        if (nicknameField.getText().isBlank()) {
            AppNav.warn("닉네임을 입력해 주세요.");
            return;
        }

        AppNav.info("사용할 수 있는 닉네임입니다.");
    }

    @FXML
    private void handleSave() {
        if (currentPassword.getText().isBlank()) {
            Widgets.showError(errorLabel, "현재 비밀번호를 입력해 주세요.");
            return;
        }

        Widgets.hideError(errorLabel);
        AppNav.close(root);
        AppNav.info("변경되었습니다.");
    }

    @FXML
    private void handleCancel() {
        AppNav.close(root);
    }
}
