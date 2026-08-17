package com.safefood.view.controller;

import com.safefood.network.GroupClient;
import com.safefood.network.GroupServer;
import com.safefood.network.GroupSession;
import com.safefood.network.Message;
import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * 그룹 대기실 — {@link GroupSession}의 스냅샷을 그리고, 수신 알림이 올 때마다 다시 그립니다.
 *
 * <p>수신은 소켓 수신 스레드에서 오므로 화면 갱신은 전부 {@code Platform.runLater}로 감쌉니다.
 * 참여 알림(JOINED/LEFT), READY 현황, 조건 병합 결과, 후보·실시간 득표, 채팅이 모두 소켓으로 옵니다.
 */
public class WaitingRoomController implements GroupClient.Listener {

    @FXML private TableView<DemoData.Member> memberTable;
    @FXML private ScrollPane console;
    @FXML private VBox logBox;
    @FXML private TextField chatInput;
    @FXML private VBox mergedBox;
    @FXML private VBox candidateBox;
    @FXML private Button resultButton;
    @FXML private Button closeRoomButton;

    private final GroupSession session = GroupSession.get();
    private final ObservableList<DemoData.Member> members = FXCollections.observableArrayList();

    private int renderedLogCount;
    private boolean leaving;

    @FXML
    private void initialize() {
        setUpMemberTable();
        logBox.heightProperty().addListener((observable, before, after) -> console.setVvalue(1.0));

        if (!session.isOwner()) {
            closeRoomButton.setText("나가기");
        }

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

    private void setUpMemberTable() {
        memberTable.setItems(members);
        memberTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<DemoData.Member, String> nameColumn = new TableColumn<>("이름");
        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().name() + " (" + data.getValue().role() + ")"));

        TableColumn<DemoData.Member, String> stateColumn = new TableColumn<>("상태");
        stateColumn.setPrefWidth(110);
        stateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().state()));
        stateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String state, boolean empty) {
                super.updateItem(state, empty);
                setText(null);
                setGraphic(empty || state == null ? null : Widgets.memberState(state));
            }
        });

        memberTable.getColumns().add(nameColumn);
        memberTable.getColumns().add(stateColumn);
    }

    // ---- GroupClient.Listener (수신 스레드에서 호출됩니다) ----

    @Override
    public void onMessage(Message message) {
        Platform.runLater(() -> {
            renderLogs();
            // 후보 카드(투표 버튼)는 채팅 등 무관한 메시지에 다시 만들지 않습니다 — 클릭 유실 방지
            switch (message.type()) {
                case JOINED, LEFT, READY -> refreshMembers();
                case MERGED -> renderMerged();
                case CANDIDATES, VOTE_STATUS -> renderCandidates();
                case RESULT -> {
                    renderCandidates();
                    AppNav.info("최종 메뉴가 확정되었습니다!\n" + session.result());
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
        refreshMembers();
        renderLogs();
        renderMerged();
        renderCandidates();
    }

    private void refreshMembers() {
        List<DemoData.Member> rows = new ArrayList<>();
        for (GroupSession.MemberView member : session.members()) {
            rows.add(new DemoData.Member(member.name(),
                    member.owner() ? "방장" : "참여자",
                    member.ready() ? "READY" : "WAITING"));
        }
        members.setAll(rows);
    }

    private void renderLogs() {
        List<String> lines = session.logLines();
        for (int i = renderedLogCount; i < lines.size(); i++) {
            appendLog(lines.get(i));
        }
        renderedLogCount = lines.size();
    }

    private void appendLog(String text) {
        Label line = new Label(text);
        line.getStyleClass().addAll("log-line", "log-joined");
        logBox.getChildren().add(line);
    }

    private void renderMerged() {
        // 첫 자식은 FXML의 제목 라벨 — 그 아래만 다시 채웁니다
        mergedBox.getChildren().remove(1, mergedBox.getChildren().size());
        List<String> lines = session.mergedLines();
        if (lines.isEmpty()) {
            mergedBox.getChildren().add(Widgets.sub("참여자 전원이 READY가 되면 병합 결과가 표시됩니다."));
            return;
        }
        for (String line : lines) {
            mergedBox.getChildren().add(new Label("· " + line));
        }
    }

    private void renderCandidates() {
        boolean finished = session.result() != null;
        // 확정 후에는 재확정(RESULT 재발송)을 막습니다
        resultButton.setDisable(!session.isOwner() || finished);

        candidateBox.getChildren().clear();
        List<GroupSession.CandidateView> list = session.candidates();
        if (list.isEmpty()) {
            candidateBox.getChildren().add(Widgets.sub("전원 준비가 끝나면 추천 후보가 도착합니다."));
            return;
        }
        if (finished) {
            candidateBox.getChildren().add(Widgets.sub("확정된 메뉴 — " + session.result()));
        }
        int total = session.totalVotes();
        int leadingNo = leadingNo(list);
        for (GroupSession.CandidateView candidate : list) {
            candidateBox.getChildren().add(candidateCard(candidate, total, leadingNo, finished));
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

    private VBox candidateCard(GroupSession.CandidateView candidate, int totalVotes, int leadingNo,
                               boolean finished) {
        Label title = new Label(candidate.no() + "번 후보. " + candidate.menu()
                + (candidate.restaurant().isEmpty() ? "" : " (" + candidate.restaurant() + ")"));
        title.getStyleClass().add("section-title");

        int votes = session.votesFor(candidate.no());
        double ratio = totalVotes == 0 ? 0 : (double) votes / totalVotes;
        int percent = (int) Math.round(ratio * 100);

        ProgressBar bar = new ProgressBar(ratio);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add(candidate.no() == leadingNo && votes > 0 ? "leading" : "trailing");
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label count = new Label(votes + "표 (" + percent + "%)");
        count.getStyleClass().add("vote-count");
        count.setMinWidth(80);

        Button vote = new Button("투표");
        vote.setDisable(finished);   // 확정 후에는 투표를 받지 않습니다 (서버도 무시)
        vote.setOnAction(event -> {
            GroupClient client = session.client();
            if (client != null) {
                client.sendVote(candidate.no());   // 득표 현황은 VOTE_STATUS 수신으로 갱신됩니다
            }
        });

        HBox voteRow = new HBox(10, bar, count, vote);
        voteRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, title, voteRow);
        card.getStyleClass().add("card");
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
            client.sendChat(text);   // 서버가 나를 포함한 전원에게 중계 → 수신될 때 로그에 찍힙니다
        }
        chatInput.clear();
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
