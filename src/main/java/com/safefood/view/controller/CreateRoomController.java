package com.safefood.view.controller;

import com.safefood.network.GroupServer;
import com.safefood.network.GroupSession;
import com.safefood.network.InviteCode;
import com.safefood.network.SocketConfig;
import com.safefood.view.AppNav;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.InetAddress;

/**
 * 방 만들기 — 이 창이 열리는 순간 방장 앱이 곧 서버가 됩니다 (README 소켓 통신 설계).
 *
 * <p>일행에게 전달할 값은 <b>초대 코드 하나뿐</b>입니다. 접속 주소는 코드 안에 들어 있어서
 * ({@link InviteCode}) 따로 알려 주지 않아도 참여자가 찾아옵니다.
 */
public class CreateRoomController {

    @FXML private VBox root;
    @FXML private TextField codeField;
    @FXML private Label addressLabel;
    @FXML private Label discoveryLabel;
    @FXML private Button copyButton;

    private final GroupSession session = GroupSession.get();

    @FXML
    private void initialize() {
        // X로 창을 닫아도 서버가 몰래 남지 않게 — 버튼(뒤로/입장)으로 닫을 때는 실행되지 않습니다
        AppNav.onDialogClosed(root, session::shutdown);

        InetAddress host = GroupServer.lanAddress();
        int port = SocketConfig.port();

        // 주소를 코드에 담습니다 — 서버를 열기 전에 먼저 확인해, 실패하면 포트를 잡지 않습니다
        String inviteCode;
        try {
            inviteCode = InviteCode.issue(host, port);
        } catch (IllegalArgumentException e) {
            fail("초대 코드를 만들지 못했습니다.\n\n원인: " + e.getMessage());
            return;
        }

        try {
            session.hostRoom(port, inviteCode);
        } catch (IOException e) {
            fail("방(서버)을 열지 못했습니다.\n"
                    + "포트 " + port + "이(가) 이미 사용 중이라면 config.properties의 "
                    + "socket.port 값을 바꾼 뒤 다시 시도하세요.\n\n원인: " + e.getMessage());
            return;
        }
        // 방장도 자기 서버에 클라이언트로 접속 — 참여자와 화면 로직이 같아집니다
        session.client().join(inviteCode, session.displayName());

        codeField.setText(InviteCode.format(inviteCode));
        addressLabel.setText("이 코드에는 접속 주소 " + host.getHostAddress() + ":" + port
                + " 이(가) 들어 있습니다.");

        // 코드에 굳어 있는 주소는 '지금'의 값입니다. 자동 탐색이 켜져 있으면 IP가 바뀌어도
        // 참여자가 방을 다시 찾아내므로, 코드를 새로 전달할 필요가 있는지가 달라집니다.
        discoveryLabel.setText(session.server().discoverable()
                ? "와이파이를 다시 잡아 이 PC의 주소가 바뀌어도, 같은 네트워크 안이라면"
                        + " 참여자가 이 코드로 방을 다시 찾아냅니다."
                : "자동 탐색을 켜지 못했습니다. 이 PC의 주소가 바뀌면 코드가 무효가 되니,"
                        + " 그때는 방을 다시 만들어 새 코드를 전달하세요.");
    }

    /** 코드 발급·서버 시작 실패 — 화면을 '못 쓰는 상태'로 명확히 표시합니다. */
    private void fail(String message) {
        codeField.setText("발급 실패");
        addressLabel.setText("일행이 참여할 수 없습니다. 뒤로 간 뒤 다시 시도해 주세요.");
        copyButton.setDisable(true);
        AppNav.error(message);
    }

    @FXML
    private void handleCopyCode() {
        ClipboardContent content = new ClipboardContent();
        content.putString(codeField.getText());
        Clipboard.getSystemClipboard().setContent(content);
        AppNav.info("초대 코드가 복사되었습니다.\n일행에게 그대로 전달하세요.");
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
