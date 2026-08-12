package com.safefood.view.controller;

import com.safefood.view.AppNav;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class GroupOptionController {

    @FXML private VBox root;

    @FXML
    private void handleCreate() {

        AppNav.close(root);
        AppNav.dialog("방 만들기 - 접속 주소 생성", "create-room.fxml");
    }

    @FXML
    private void handleJoin() {
        AppNav.close(root);
        AppNav.dialog("방 참여하기", "join-room.fxml");
    }
}
