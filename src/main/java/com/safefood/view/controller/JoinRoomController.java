package com.safefood.view.controller;

import com.safefood.network.GroupClient;
import com.safefood.network.GroupSession;
import com.safefood.network.Message;
import com.safefood.network.SocketConfig;
import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

/**
 * 방 참여하기 — 전달받은 '접속 주소 + 초대 코드'로 방장 서버에 붙습니다.
 * 입장 성공 여부는 서버의 JOINED / ERROR 응답으로 판정합니다.
 */
public class JoinRoomController {

    @FXML private VBox root;
    @FXML private TextField addressField;
    @FXML private TextField codeField;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator progress;
    @FXML private Button joinButton;

    private final GroupSession session = GroupSession.get();

    // 접속됐지만 서버가 JOINED/ERROR를 안 주는 경우(엉뚱한 프로그램이 그 포트에 떠 있을 때) 대비
    private final PauseTransition responseTimeout = new PauseTransition(Duration.seconds(5));

    @FXML
    private void initialize() {
        // 연결 시도 중 X로 닫으면 붙어 있던 소켓을 정리합니다
        AppNav.onDialogClosed(root, session::shutdown);

        responseTimeout.setOnFinished(event -> {
            session.shutdown();
            fail("서버가 응답하지 않습니다. 주소와 포트가 방장 화면의 값과 같은지 확인해 주세요.");
        });
    }

    @FXML
    private void handleJoin() {
        String address = addressField.getText().trim();
        String inviteCode = codeField.getText().trim();
        if (address.isBlank() || inviteCode.isBlank()) {
            Widgets.showError(errorLabel, "접속 주소와 초대 코드를 모두 입력해 주세요.");
            return;
        }

        String host;
        int port;
        try {
            String[] parts = address.split(":");   // 포트를 생략하면 기본 포트
            host = parts[0].trim();
            port = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : SocketConfig.DEFAULT_PORT;
            if (host.isEmpty()) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Widgets.showError(errorLabel, "접속 주소는 '주소:포트' 형식으로 입력해 주세요. 예) 192.168.0.11:5000");
            return;
        }

        Widgets.hideError(errorLabel);
        setBusy(true);
        responseTimeout.playFromStart();

        // 접속은 몇 초씩 걸릴 수 있어 UI 스레드 밖에서 시도합니다
        Thread connector = new Thread(() -> {
            try {
                session.joinRoom(host, port);
                session.setUiListener(joinResultListener());
                session.client().join(inviteCode, session.displayName());
            } catch (IOException e) {
                Platform.runLater(() -> fail(
                        "접속하지 못했습니다. 주소가 맞는지, 방장과 같은 네트워크인지 확인해 주세요.\n("
                                + e.getMessage() + ")"));
            }
        }, "join-room-connect");
        connector.setDaemon(true);
        connector.start();
    }

    private GroupClient.Listener joinResultListener() {
        return new GroupClient.Listener() {
            @Override
            public void onMessage(Message message) {
                if (message.type() == Message.Type.JOINED
                        && message.part(0).equals(session.displayName())) {
                    Platform.runLater(JoinRoomController.this::succeed);
                } else if (message.type() == Message.Type.ERROR) {
                    Platform.runLater(() -> {
                        session.shutdown();
                        fail(message.part(0));
                    });
                }
            }

            @Override
            public void onDisconnected() {
                Platform.runLater(() -> {
                    session.shutdown();
                    fail("서버와의 연결이 끊겼습니다.");
                });
            }
        };
    }

    private void succeed() {
        responseTimeout.stop();
        if (!session.isConnected()) {
            return;   // 응답 직전에 X로 취소한 경우 — 죽은 세션으로 진행하지 않습니다
        }
        setBusy(false);
        session.setUiListener(null);   // 다음 화면(대기실)이 자기 리스너를 답니다
        AppNav.close(root);
        AppNav.dialog("그룹 조건 입력", "group-condition.fxml");
    }

    private void fail(String reason) {
        responseTimeout.stop();
        setBusy(false);
        Widgets.showError(errorLabel, reason);
    }

    private void setBusy(boolean busy) {
        joinButton.setDisable(busy);
        progress.setVisible(busy);
    }
}
