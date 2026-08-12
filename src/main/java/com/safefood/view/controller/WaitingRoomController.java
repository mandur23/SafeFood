package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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

import java.util.List;

public class WaitingRoomController {

    private static final boolean IS_OWNER = true;

    @FXML private TableView<DemoData.Member> memberTable;
    @FXML private ScrollPane console;
    @FXML private VBox logBox;
    @FXML private TextField chatInput;
    @FXML private VBox mergedBox;
    @FXML private VBox candidateBox;
    @FXML private Button resultButton;
    @FXML private Button closeRoomButton;

    @FXML
    private void initialize() {
        setUpMemberTable();
        fillLog();
        fillMerged();
        fillCandidates();

        resultButton.setDisable(!IS_OWNER);
        closeRoomButton.setDisable(!IS_OWNER);
    }

    private void setUpMemberTable() {
        memberTable.setItems(FXCollections.observableArrayList(DemoData.MEMBERS));
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

    private void fillLog() {
        for (String line : DemoData.SOCKET_LOG) {
            appendLog(line);
        }

        logBox.heightProperty().addListener((observable, before, after) -> console.setVvalue(1.0));
    }

    private void appendLog(String text) {
        Label line = new Label(text);
        line.getStyleClass().addAll("log-line", "log-joined");
        logBox.getChildren().add(line);
    }

    private void fillMerged() {
        for (String line : DemoData.MERGED_CONDITION) {
            mergedBox.getChildren().add(new Label("· " + line));
        }
    }

    private void fillCandidates() {
        List<DemoData.Candidate> list = DemoData.CANDIDATES;
        if (list.isEmpty()) {

            candidateBox.getChildren().add(Widgets.sub("조건을 만족하는 메뉴가 없습니다."));
            candidateBox.getChildren().add(relaxButton());
            return;
        }
        int total = list.stream().mapToInt(DemoData.Candidate::votes).sum();
        for (DemoData.Candidate candidate : list) {
            candidateBox.getChildren().add(candidateCard(candidate, total));
        }
    }

    private VBox candidateCard(DemoData.Candidate candidate, int totalVotes) {
        Label title = new Label(candidate.no() + "번 후보. " + candidate.menu()
                + " (" + candidate.restaurant() + ")");
        title.getStyleClass().add("section-title");

        double ratio = totalVotes == 0 ? 0 : (double) candidate.votes() / totalVotes;
        int percent = (int) Math.round(ratio * 100);

        ProgressBar bar = new ProgressBar(ratio);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add(candidate.no() == 1 ? "leading" : "trailing");
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label count = new Label(candidate.votes() + "표 (" + percent + "%)");
        count.getStyleClass().add("vote-count");
        count.setMinWidth(80);

        Button vote = new Button("투표");

        vote.setOnAction(event -> AppNav.info(candidate.menu() + "에 투표했습니다."));

        HBox voteRow = new HBox(10, bar, count, vote);
        voteRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, title, voteRow);
        card.getStyleClass().add("card");
        return card;
    }

    private Button relaxButton() {
        Button relax = new Button("조건 완화하기");
        relax.getStyleClass().add("outline");
        relax.setOnAction(event -> {
            boolean yes = AppNav.confirm(
                    "조건을 만족하는 메뉴가 없습니다. 조건을 완화할까요?\n\n"
                            + "⚠️ 혼입 가능(POSSIBLE) 등급 메뉴가 후보에 들어갑니다. "
                            + "알레르기가 심한 참여자에게는 위험할 수 있습니다.");
            if (yes) {
                AppNav.warn("POSSIBLE 등급을 허용해 다시 추천합니다. 각 후보의 경고 문구를 꼭 확인하세요.");
            }
        });
        return relax;
    }

    @FXML
    private void handleSend() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        appendLog("[나] " + text);
        chatInput.clear();
    }

    @FXML
    private void handleResult() {

        AppNav.info("최다 득표 후보로 확정하고 전원에게 알립니다.\n확정 메뉴 — 김치찌개 (할머니손맛)");
    }

    @FXML
    private void handleCloseRoom() {
        if (AppNav.confirm("방을 종료할까요? 참여자 전원의 연결이 끊깁니다.")) {

            AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
        }
    }
}
