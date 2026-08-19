package com.safefood.view.controller;

import com.safefood.network.GroupServer;
import com.safefood.network.GroupSession;
import com.safefood.network.SocketConfig;
import com.safefood.view.AppNav;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * 방 만들기 — 이 창이 열리는 순간 방장 앱이 곧 서버가 됩니다 (README 소켓 통신 설계).
 * 화면에는 일행에게 전달할 '접속 주소 + 초대 코드'를 표시합니다.
 */
public class CreateRoomController {

    @FXML private VBox root;
    @FXML private TextField addressField;
    @FXML private TextField codeField;

    private final GroupSession session = GroupSession.get();

    @FXML
    private void initialize() {
        // X로 창을 닫아도 서버가 몰래 남지 않게 — 버튼(뒤로/입장)으로 닫을 때는 실행되지 않습니다
        AppNav.onDialogClosed(root, session::shutdown);

        String inviteCode = GroupServer.newInviteCode();
        int port = SocketConfig.port();
        try {
            session.hostRoom(port, inviteCode);
        } catch (IOException e) {
            addressField.setText("서버 시작 실패");
            codeField.setText("-");
            AppNav.error("방(서버)을 열지 못했습니다.\n"
                    + "포트 " + port + "이(가) 이미 사용 중이라면 config.properties의 "
                    + "socket.port 값을 바꾼 뒤 다시 시도하세요.\n\n원인: " + e.getMessage());
            return;
        }
        // 방장도 자기 서버에 클라이언트로 접속 — 참여자와 화면 로직이 같아집니다
        session.client().join(inviteCode, session.displayName());

        addressField.setText(GroupServer.hostAddress() + ":" + port);
        codeField.setText(inviteCode);
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
        session.shutdown();   // 방장이 뒤로 가면 방을 정리 — 이미 들어온 참여자에게 CLOSED가 갑니다
        AppNav.close(root);
        AppNav.dialog("같이 먹기 옵션 선택", "group-option.fxml");
    }

    @FXML
    private void handleEnter() {
        if (!session.isConnected()) {
            AppNav.warn("서버가 열려 있지 않습니다. 뒤로 가서 다시 시도해 주세요.");
            return;
        }
        AppNav.close(root);
        AppNav.dialog("그룹 조건 입력", "group-condition.fxml");
    }
}
