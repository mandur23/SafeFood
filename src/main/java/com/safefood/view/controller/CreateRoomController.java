package com.safefood.view.controller;

import com.safefood.view.AppNav;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

public class CreateRoomController {

    @FXML private VBox root;
    @FXML private TextField addressField;
    @FXML private TextField codeField;

    @FXML
    private void initialize() {

        addressField.setText("192.168.0.11:5000");
        codeField.setText("ABC123");
    }

    @FXML
    private void handleCopyAddress() {
        copy(addressField.getText());
    }

    @FXML
    private void handleCopyCode() {
        copy(codeField.getText());
    }

    private void copy(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        AppNav.info("복사되었습니다.");
    }

    @FXML
    private void handleBack() {

        AppNav.close(root);
        AppNav.dialog("같이 먹기 옵션 선택", "group-option.fxml");
    }

    @FXML
    private void handleEnter() {

        AppNav.close(root);
        AppNav.dialog("그룹 조건 입력", "group-condition.fxml");
    }
}
