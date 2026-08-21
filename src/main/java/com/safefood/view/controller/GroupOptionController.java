package com.safefood.view.controller;

import com.safefood.network.GroupClient;
import com.safefood.network.GroupServer;
import com.safefood.network.GroupSession;
import com.safefood.network.InviteCode;
import com.safefood.network.Message;
import com.safefood.network.RoomFinder;
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
import javafx.scene.control.TextFormatter;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * 같이 먹기 — 방 만들기와 참여를 한 창에서 처리합니다 (리디자인 1f).
 *
 * <p>예전에는 group-option → create-room / join-room 으로 화면이 갈라져서, 어느 쪽을 고르든
 * 창이 한 번 더 떴습니다. 지금은 두 길이 같은 창에 나란히 있고, 방 만들기는 누른 자리에서
 * 코드 상태로 바뀝니다.
 *
 * <p><b>서버를 언제 여는가</b> — 목업은 창이 열리자마자 코드가 나와 있지만, 그렇게 하면
 * 참여만 하려고 들어온 사람도 포트를 잡고 탐색 비컨을 띄우게 됩니다. 그래서 방 만들기는
 * 버튼을 눌렀을 때 열고, 참여 쪽은 아무것도 건드리지 않습니다.
 */
public class GroupOptionController {

    @FXML private VBox root;

    // 방 만들기
    @FXML private VBox createIntro;
    @FXML private VBox createdBox;
    @FXML private Label hostTag;
    @FXML private Button createButton;
    @FXML private TextField codeField;
    @FXML private Label addressLabel;
    @FXML private Label discoveryLabel;
    @FXML private Button copyButton;

    // 참여
    @FXML private VBox joinBox;
    @FXML private TextField joinField;
    @FXML private Button joinButton;
    @FXML private Label previewLabel;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator progress;

    private final GroupSession session = GroupSession.get();

    // 접속됐지만 서버가 JOINED/ERROR를 안 주는 경우(엉뚱한 프로그램이 그 포트에 떠 있을 때) 대비.
    // 접속·탐색에 걸린 시간이 섞이지 않도록, 소켓이 붙은 뒤에 시작합니다.
    private final PauseTransition responseTimeout = new PauseTransition(Duration.seconds(5));

    // 접속·탐색은 배경 스레드에서 몇 초씩 도는데 그 사이에 창을 닫을 수 있습니다.
    // 닫힌 화면에 소켓을 새로 물리지 않도록 각 단계에서 이 값을 확인합니다.
    private volatile boolean cancelled;

    @FXML
    private void initialize() {
        // X로 창을 닫아도 서버·소켓이 몰래 남지 않게 — 버튼으로 닫을 때는 실행되지 않습니다
        AppNav.onDialogClosed(root, () -> {
            cancelled = true;
            session.shutdown();
        });

        responseTimeout.setOnFinished(event -> {
            session.shutdown();
            fail("방장이 응답하지 않습니다. 코드가 방장 화면의 값과 같은지 확인해 주세요.");
        });

        // 소문자·구분선·O/I/L 혼동을 입력하는 즉시 표준형으로 교정하고, 14자를 넘기지 못하게 합니다
        joinField.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }
            change.setText(InviteCode.normalize(change.getText()));
            return change.getControlNewText().length() > InviteCode.LENGTH ? null : change;
        }));
        joinField.textProperty().addListener((observable, old, text) -> updatePreview(text));
        updatePreview("");
    }

    /**
     * 로그인 화면에서 이미 받아 둔 초대 코드를 채워 둡니다 — 게스트가 코드를 두 번 적지 않게.
     * 빈 값이면 아무것도 하지 않습니다.
     */
    public void prefillCode(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        joinField.setText(code);   // 포매터가 표준형으로 교정하고, 리스너가 미리보기를 채웁니다
        joinField.requestFocus();
    }

    // ══ 방 만들기 ═══════════════════════════════════════════════

    /**
     * 이 순간부터 방장 앱이 서버가 됩니다 (README 소켓 통신 설계).
     *
     * <p>일행에게 전달할 값은 <b>초대 코드 하나뿐</b>입니다. 접속 주소는 코드 안에 들어 있어서
     * ({@link InviteCode}) 따로 알려 주지 않아도 참여자가 찾아옵니다.
     */
    @FXML
    private void handleCreate() {
        InetAddress host = GroupServer.lanAddress();
        int port = SocketConfig.port();

        // 주소를 코드에 담습니다 — 서버를 열기 전에 먼저 확인해, 실패하면 포트를 잡지 않습니다
        String inviteCode;
        try {
            inviteCode = InviteCode.issue(host, port);
        } catch (IllegalArgumentException e) {
            AppNav.error("초대 코드를 만들지 못했습니다.\n\n원인: " + e.getMessage());
            return;
        }

        try {
            session.hostRoom(port, inviteCode);
        } catch (IOException e) {
            AppNav.error("방(서버)을 열지 못했습니다.\n"
                    + "포트 " + port + "이(가) 이미 사용 중이라면 config.properties의 "
                    + "socket.port 값을 바꾼 뒤 다시 시도하세요.\n\n원인: " + e.getMessage());
            return;
        }
        // 방장도 자기 서버에 클라이언트로 접속 — 참여자와 화면 로직이 같아집니다
        session.client().join(inviteCode, session.displayName());

        codeField.setText(InviteCode.format(inviteCode));
        addressLabel.setText("같은 와이파이에서만 접속돼요 · 주소 "
                + host.getHostAddress() + ":" + port + " (코드에 포함)");

        // 코드에 굳어 있는 주소는 '지금'의 값입니다. 자동 탐색이 켜져 있으면 IP가 바뀌어도
        // 참여자가 방을 다시 찾아내므로, 코드를 새로 전달할 필요가 있는지가 달라집니다.
        discoveryLabel.setText(session.server().discoverable()
                ? "이 PC의 주소가 바뀌어도 같은 네트워크라면 참여자가 이 코드로 방을 다시 찾아냅니다."
                : "자동 탐색을 켜지 못했습니다. 주소가 바뀌면 방을 다시 만들어 새 코드를 전달하세요.");

        showCreated();
    }

    /** 방을 연 뒤 — 카드를 코드 상태로 바꾸고, 이제 필요 없는 참여 쪽을 잠급니다. */
    private void showCreated() {
        show(createIntro, false);
        show(createdBox, true);
        show(hostTag, true);
        joinBox.setDisable(true);
        previewLabel.setText("이미 방장입니다. 참여하려면 창을 닫고 다시 여세요.");
    }

    @FXML
    private void handleCopyCode() {
        ClipboardContent content = new ClipboardContent();
        content.putString(codeField.getText());
        Clipboard.getSystemClipboard().setContent(content);
        AppNav.success("초대 코드를 복사했어요 — 일행에게 그대로 전달하세요");
    }

    @FXML
    private void handleEnter() {
        if (!session.isConnected()) {
            AppNav.warn("서버가 열려 있지 않습니다. 창을 닫고 다시 시도해 주세요.");
            return;
        }
        AppNav.close(root);
        AppNav.dialog("그룹 조건 확인", "group-condition.fxml");
    }

    // ══ 참여 ════════════════════════════════════════════════════

    /** 입력 중 실시간 안내 — 다 입력하면 코드에서 꺼낸 방장 주소를 보여 줍니다. */
    private void updatePreview(String text) {
        String code = InviteCode.normalize(text);
        if (code.length() < InviteCode.LENGTH) {
            previewLabel.setText(code.isEmpty()
                    ? "전달받은 초대 코드를 입력하세요."
                    : code.length() + " / " + InviteCode.LENGTH + "자");
            previewLabel.getStyleClass().remove("error-text");
            return;
        }
        try {
            previewLabel.setText("✓ 코드 확인 — 방장 주소 " + InviteCode.parse(code).address()
                    + " 을(를) 찾았어요");
            previewLabel.getStyleClass().remove("error-text");
        } catch (IllegalArgumentException e) {
            previewLabel.setText(e.getMessage());
            if (!previewLabel.getStyleClass().contains("error-text")) {
                previewLabel.getStyleClass().add("error-text");
            }
        }
    }

    @FXML
    private void handleJoin() {
        InviteCode invite;
        try {
            invite = InviteCode.parse(joinField.getText());   // 여기서 주소가 나옵니다
        } catch (IllegalArgumentException e) {
            Widgets.showError(errorLabel, e.getMessage());
            return;
        }

        Widgets.hideError(errorLabel);
        cancelled = false;
        setBusy(true);

        // 접속도 탐색도 몇 초씩 걸릴 수 있어 UI 스레드 밖에서 시도합니다
        Thread connector = new Thread(() -> connect(invite), "join-room-connect");
        connector.setDaemon(true);
        connector.start();
    }

    /**
     * 접속 두 단계 — 코드의 주소로 바로 붙어 보고, 안 되면 같은 네트워크에서 방을 찾습니다.
     *
     * <p>순서가 이렇게 된 이유: 코드의 주소는 대부분 그대로 맞아서 즉시 붙고, 네트워크에 아무것도
     * 뿌리지 않습니다. 탐색을 먼저 하면 <b>모든 참여자가</b> 매번 몇 초씩 기다리고 방화벽 확인 창까지
     * 보게 됩니다. 그래서 탐색은 실제로 필요할 때만 켭니다.
     *
     * <p>접속 전용 스레드에서 돕니다 — 화면을 건드릴 때만 {@link Platform#runLater}로 넘깁니다.
     */
    private void connect(InviteCode invite) {
        try {
            open(invite.host(), invite.port(), invite.code());
            return;   // 1차 성공 — 이후는 JOINED/ERROR 응답이 판정합니다
        } catch (IOException direct) {
            if (cancelled) {
                return;
            }
            Platform.runLater(() -> status("코드에 담긴 주소(" + invite.address() + ")에 닿지 않았습니다."
                    + " 같은 네트워크에서 방을 찾는 중…"));
        }

        // 2차 — 방장의 IP가 바뀌었어도 코드는 그대로 쓸 수 있게 해 주는 폴백
        Optional<InetSocketAddress> found = RoomFinder.find(invite.code());
        if (cancelled) {
            return;   // 탐색하는 사이에 창을 닫았습니다
        }
        if (found.isEmpty()) {
            Platform.runLater(() -> fail("방을 찾지 못했습니다.\n"
                    + "방이 아직 열려 있는지, 방장과 같은 와이파이에 있는지 확인해 주세요.\n"
                    + "(코드에 담긴 주소 " + invite.address() + " 에도 닿지 않았습니다.)"));
            return;
        }

        InetSocketAddress host = found.get();
        String moved = host.getAddress().getHostAddress() + ":" + host.getPort();
        Platform.runLater(() -> status("방을 찾았습니다 — " + moved + " 로 접속합니다."));
        try {
            open(host.getAddress().getHostAddress(), host.getPort(), invite.code());
        } catch (IOException e) {
            Platform.runLater(() -> fail(moved + " 에서 방을 찾았지만 접속하지 못했습니다.\n("
                    + e.getMessage() + ")"));
        }
    }

    /** 소켓 접속 → 리스너 부착 → JOIN 전송. 여기까지가 배경 스레드의 몫입니다. */
    private void open(String host, int port, String code) throws IOException {
        session.joinRoom(host, port);
        if (cancelled) {
            session.shutdown();   // 붙는 사이에 창이 닫혔습니다 — 방금 연 소켓을 도로 정리합니다
            return;
        }
        session.setInviteCode(code);   // 대기실 머리에 다시 보여 주기 위해
        session.setUiListener(joinResultListener());
        // 응답 대기는 소켓이 붙은 지금부터 셉니다. JOIN을 보내기 전에 걸어 두므로,
        // 응답이 아무리 빨라도 UI 스레드에서 playFromStart → succeed 순서가 지켜집니다.
        Platform.runLater(responseTimeout::playFromStart);
        session.client().join(code, session.displayName());
    }

    private GroupClient.Listener joinResultListener() {
        return new GroupClient.Listener() {
            @Override
            public void onMessage(Message message) {
                if (message.type() == Message.Type.JOINED
                        && message.part(0).equals(session.displayName())) {
                    Platform.runLater(GroupOptionController.this::succeed);
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
        session.setUiListener(null);   // 다음 화면(조건 확인)이 자기 리스너를 답니다
        AppNav.close(root);
        AppNav.dialog("그룹 조건 확인", "group-condition.fxml");
    }

    /** 진행 상황 — 코드 아래 줄(입력 중에는 자릿수·주소 안내가 나오던 자리)에 그대로 씁니다. */
    private void status(String text) {
        previewLabel.getStyleClass().remove("error-text");
        previewLabel.setText(text);
    }

    private void fail(String reason) {
        responseTimeout.stop();
        setBusy(false);
        updatePreview(joinField.getText());   // 진행 상황 자리를 원래 안내로 되돌립니다
        Widgets.showError(errorLabel, reason);
    }

    private void setBusy(boolean busy) {
        joinButton.setDisable(busy);
        joinField.setDisable(busy);
        createButton.setDisable(busy);   // 접속 중에 방을 열면 세션이 엎어집니다
        show(progress, busy);
    }

    private static void show(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
