package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class JoinRoomController {

    @FXML private VBox root;
    @FXML private TextField addressField;
    @FXML private TextField codeField;
    @FXML private Label errorLabel;

    @FXML
    private void handleJoin() {
        if (addressField.getText().isBlank() || codeField.getText().isBlank()) {
            Widgets.showError(errorLabel, "접속 주소와 초대 코드를 모두 입력해 주세요.");
            return;
        }
        Widgets.hideError(errorLabel);

        AppNav.close(root);

        AppNav.info("그룹 방에 정상적으로 연결되었습니다!");
        AppNav.dialog("그룹 조건 입력", "group-condition.fxml");
    }
}
