package com.safefood.view.controller;

import com.safefood.network.GroupClient;
import com.safefood.network.GroupServer;
import com.safefood.network.GroupSession;
import com.safefood.network.InviteCode;
import com.safefood.network.Message;
import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * 그룹 대기실 — {@link GroupSession}의 스냅샷을 그리고, 수신 알림이 올 때마다 다시 그립니다.
 *
 * <p>리디자인(1b)에서 화면이 둘로 갈렸습니다. 왼쪽은 대화 흐름 — 참여·준비·조건 병합·최종 확정이
 * 전부 채팅과 같은 줄에 시간 순으로 흐릅니다({@link GroupSession#events()}). 오른쪽은 사람과
 * 투표입니다. 프로토콜 원문은 접힌 '소켓 로그'에 그대로 남겨 뒀습니다.
 *
 * <p>수신은 소켓 수신 스레드에서 오므로 화면 갱신은 전부 {@code Platform.runLater}로 감쌉니다.
 */
public class WaitingRoomController implements GroupClient.Listener {

    @FXML private Label memberTag;
    @FXML private Label codeLabel;
    @FXML private Button copyButton;
    @FXML private Button closeRoomButton;

    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatBox;
    @FXML private TextField chatInput;

    @FXML private FlowPane avatarRow;
    @FXML private Label voteTitle;
    @FXML private VBox candidateBox;
    @FXML private Button resultButton;

    @FXML private ScrollPane console;
    @FXML private VBox logBox;

    private final GroupSession session = GroupSession.get();

    private int renderedLogCount;
    private int renderedEventCount;
    private boolean leaving;

    @FXML
    private void initialize() {
        // 새 줄이 붙을 때마다 맨 아래로 — 대화도, 로그도
        chatBox.heightProperty().addListener((observable, before, after) -> chatScroll.setVvalue(1.0));
        logBox.heightProperty().addListener((observable, before, after) -> console.setVvalue(1.0));

        if (!session.isOwner()) {
            closeRoomButton.setText("나가기");
        }

        String code = session.inviteCode();
        codeLabel.setText(code.isEmpty() ? "코드 없음" : InviteCode.format(code));
        copyButton.setDisable(code.isEmpty());

        renderAll();
        session.setUiListener(this);   // 이 화면이 열려 있는 동안 수신 알림을 받습니다

        // 화면 전환 사이에 CLOSED·끊김이 도착해 알림을 놓쳤을 수 있어 입장 시점에 한 번 확인합니다.
        // (initialize는 화면 로드 중이라, 로드가 끝난 다음 틱으로 미룹니다)
        Platform.runLater(() -> {
            if (session.isDead()) {
                leaveTo(session.isRemoteClosed()
                        ? "방장이 방을 종료했습니다." : "서버와의 연결이 끊겼습니다.");
            }
        });
    }

    // ---- GroupClient.Listener (수신 스레드에서 호출됩니다) ----

    @Override
    public void onMessage(Message message) {
        Platform.runLater(() -> {
            renderLogs();
            renderEvents();
            // 후보 카드(투표 버튼)는 채팅 등 무관한 메시지에 다시 만들지 않습니다 — 클릭 유실 방지
            switch (message.type()) {
                case JOINED, LEFT, READY -> renderMembers();
                case CANDIDATES, VOTE_STATUS -> renderCandidates();
                case RESULT -> {
                    renderCandidates();
                    AppNav.success("최종 메뉴가 확정되었어요 — " + session.result());
                }
                case CLOSED -> {
                    if (!session.isOwner()) {
                        leaveTo(message.part(0).isEmpty() ? "방장이 방을 종료했습니다." : message.part(0));
                    }
                }
                default -> { }
            }
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> leaveTo("서버와의 연결이 끊겼습니다."));
    }

    private void leaveTo(String reason) {
        if (leaving) {
            return;
        }
        leaving = true;
        session.shutdown();
        AppNav.warn(reason);
        goHome();
    }

    /** 방을 나간 뒤 돌아갈 화면 — 게스트는 회원용 메인이 아니라 로그인 화면으로. */
    private void goHome() {
        if (session.isGuest()) {
            AppNav.show("로그인", "login.fxml");
        } else {
            AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
        }
    }

    // ---- 그리기 (세션 스냅샷 → 화면) ----

    private void renderAll() {
        renderMembers();
        renderEvents();
        renderLogs();
        renderCandidates();
    }

    /** 머리의 인원 표시 + 오른쪽 아바타 줄. */
    private void renderMembers() {
        List<GroupSession.MemberView> members = session.members();
        int ready = 0;
        for (GroupSession.MemberView member : members) {
            if (member.ready()) {
                ready++;
            }
        }
        memberTag.setText(members.size() + "명 · " + ready + "명 준비됨");

        avatarRow.getChildren().clear();
        if (members.isEmpty()) {
            avatarRow.getChildren().add(Widgets.sub("아직 아무도 없습니다."));
            return;
        }
        for (GroupSession.MemberView member : members) {
            String variant = member.owner() ? "owner" : member.ready() ? "ready" : "waiting";
            VBox cell = new VBox(3,
                    Widgets.avatar(member.name(), variant),
                    Widgets.micro(member.owner() ? "방장 ✓" : member.ready() ? "준비 ✓" : "대기 중"));
            cell.setAlignment(Pos.CENTER);
            avatarRow.getChildren().add(cell);
        }
    }

    /** 대화 흐름 — 새로 생긴 사건만 이어 붙입니다 (통째로 다시 그리면 스크롤이 튑니다). */
    private void renderEvents() {
        List<GroupSession.Event> events = session.events();
        for (int i = renderedEventCount; i < events.size(); i++) {
            appendEvent(events.get(i));
        }
        renderedEventCount = events.size();
    }

    private void appendEvent(GroupSession.Event event) {
        switch (event.kind()) {
            case CHAT -> chatBox.getChildren().add(bubble(event.sender(), event.text()));
            case SYSTEM -> chatBox.getChildren().add(centered(Widgets.systemPill(event.text())));
            case SYSTEM_GOOD ->
                    chatBox.getChildren().add(centered(Widgets.systemPill(event.text(), "good")));
            case SYSTEM_BAD ->
                    chatBox.getChildren().add(centered(Widgets.systemPill(event.text(), "bad")));
            case MERGED -> {
                chatBox.getChildren().add(centered(Widgets.systemPill(event.text(), "good")));
                chatBox.getChildren().add(mergedCard());
            }
        }
    }

    /** 내가 보낸 말은 오른쪽 테라코타, 남의 말은 왼쪽에 이름과 함께. */
    private HBox bubble(String sender, String text) {
        boolean mine = sender.equals(session.displayName());

        Label body = new Label(text);
        body.setWrapText(true);
        body.getStyleClass().add("bubble");
        body.setMaxWidth(360);

        HBox row = new HBox(8);
        if (mine) {
            body.getStyleClass().add("mine");
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().add(body);
        } else {
            VBox stack = new VBox(0, Widgets.label(sender, "bubble-name"), body);
            row.setAlignment(Pos.BOTTOM_LEFT);
            row.getChildren().addAll(Widgets.avatar(sender, "small"), stack);
        }
        return row;
    }

    /** 병합된 조건 — 대화 흐름 한가운데 카드로 펼칩니다. */
    private HBox mergedCard() {
        VBox card = new VBox(5);
        card.getStyleClass().addAll("card", "accent2", "elev");
        card.getChildren().add(Widgets.label("병합된 조건", "kicker"));

        List<String> lines = session.mergedLines();
        if (lines.isEmpty()) {
            card.getChildren().add(Widgets.sub("병합 결과가 비어 있습니다."));
        } else {
            for (String line : lines) {
                Label item = new Label("· " + line);
                item.setWrapText(true);
                card.getChildren().add(item);
            }
        }
        card.setMaxWidth(440);

        HBox row = new HBox(card);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private static HBox centered(Label pill) {
        HBox row = new HBox(pill);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private void renderLogs() {
        List<String> lines = session.logLines();
        for (int i = renderedLogCount; i < lines.size(); i++) {
            Label line = new Label(lines.get(i));
            line.getStyleClass().addAll("log-line", "log-joined");
            logBox.getChildren().add(line);
        }
        renderedLogCount = lines.size();
    }

    // ---- 투표 패널 ----

    private void renderCandidates() {
        boolean finished = session.result() != null;
        // 확정 후에는 재확정(RESULT 재발송)을 막습니다
        resultButton.setDisable(!session.isOwner() || finished);

        candidateBox.getChildren().clear();
        List<GroupSession.CandidateView> list = session.candidates();
        int total = session.totalVotes();

        if (list.isEmpty()) {
            voteTitle.setText("투표");
            candidateBox.getChildren().add(Widgets.sub("전원 준비가 끝나면 추천 후보가 도착합니다."));
            resultButton.setText("최종 확정");
            return;
        }

        voteTitle.setText("투표 — " + session.members().size() + "표 중 " + total + "표");

        int leadingNo = leadingNo(list);
        for (GroupSession.CandidateView candidate : list) {
            candidateBox.getChildren().add(candidateCard(candidate, total, leadingNo, finished));
        }

        // 버튼이 무엇을 확정하는지 이름으로 말해 줍니다 (목업의 "최종 확정 — 김치찌개")
        String leadingMenu = menuOf(list, leadingNo);
        if (finished) {
            resultButton.setText("확정됨 — " + session.result());
        } else {
            resultButton.setText(total == 0 ? "최종 확정" : "최종 확정 — " + leadingMenu);
        }
    }

    private int leadingNo(List<GroupSession.CandidateView> list) {
        int best = list.get(0).no();
        for (GroupSession.CandidateView candidate : list) {
            if (session.votesFor(candidate.no()) > session.votesFor(best)) {
                best = candidate.no();
            }
        }
        return best;
    }

    private static String menuOf(List<GroupSession.CandidateView> list, int no) {
        for (GroupSession.CandidateView candidate : list) {
            if (candidate.no() == no) {
                return candidate.menu();
            }
        }
        return "";
    }

    private VBox candidateCard(GroupSession.CandidateView candidate, int totalVotes, int leadingNo,
                               boolean finished) {
        int votes = session.votesFor(candidate.no());
        boolean leading = candidate.no() == leadingNo && votes > 0;

        Label title = Widgets.label(candidate.menu(), "card-title");
        Label where = Widgets.sub(candidate.restaurant());

        Label count = new Label(votes + "표");
        count.getStyleClass().add("vote-count");
        if (votes == 0) {
            count.getStyleClass().add("zero");
        } else if (!leading) {
            count.getStyleClass().add("trailing");
        }

        HBox head = new HBox(8, title, where, Widgets.hSpacer(), count);
        head.setAlignment(Pos.CENTER_LEFT);

        double ratio = totalVotes == 0 ? 0 : (double) votes / totalVotes;
        ProgressBar bar = new ProgressBar(ratio);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add(leading ? "leading" : "trailing");

        Button vote = new Button("이 후보에 투표");
        vote.getStyleClass().add("ghost");
        vote.setDisable(finished);   // 확정 후에는 투표를 받지 않습니다 (서버도 무시)
        vote.setOnAction(event -> {
            GroupClient client = session.client();
            if (client != null) {
                client.sendVote(candidate.no());   // 득표 현황은 VOTE_STATUS 수신으로 갱신됩니다
            }
        });

        VBox card = new VBox(8, head, bar, vote);
        card.getStyleClass().addAll("card", "plain");
        if (leading) {
            card.getStyleClass().add("outlined");   // 1위만 테두리로 도드라지게
        }
        return card;
    }

    // ---- 입력 ----

    @FXML
    private void handleSend() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        GroupClient client = session.client();
        if (client != null) {
            client.sendChat(text);   // 서버가 나를 포함한 전원에게 중계 → 수신될 때 말풍선이 붙습니다
        }
        chatInput.clear();
    }

    @FXML
    private void handleCopyCode() {
        String code = session.inviteCode();
        if (code.isEmpty()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(InviteCode.format(code));
        Clipboard.getSystemClipboard().setContent(content);
        AppNav.success("초대 코드를 복사했어요");
    }

    @FXML
    private void handleResult() {
        GroupServer server = session.server();
        if (server == null) {
            return;
        }
        if (!server.room().finishResult()) {
            AppNav.warn("아직 아무도 투표하지 않았습니다.");
        }
    }

    @FXML
    private void handleCloseRoom() {
        if (session.isOwner()
                && !AppNav.confirm("방을 종료할까요? 참여자 전원의 연결이 끊깁니다.")) {
            return;
        }
        leaving = true;
        session.shutdown();
        goHome();
    }
}
