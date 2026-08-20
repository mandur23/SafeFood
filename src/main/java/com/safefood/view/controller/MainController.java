package com.safefood.view.controller;

import com.safefood.network.GroupSession;
import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private ComboBox<String> categoryBox;
    @FXML private ComboBox<String> distanceBox;
    @FXML private FlowPane moodPane;
    @FXML private CheckBox excludeEaten;
    @FXML private VBox cardBox;
    @FXML private Label userLabel;

    private final List<ToggleButton> moodChips = new ArrayList<>();

    @FXML
    private void initialize() {
        // 회원은 로그인 세션의 닉네임을, 게스트는 그룹 참여용 표시 이름을 씁니다.
        if (com.safefood.dto.Session.getCurrentUser() != null) {
            String myNick = com.safefood.dto.Session.getCurrentUser().getNickname();
            userLabel.setText(myNick + "님 로그인 중");
        } else {
            userLabel.setText(GroupSession.get().displayName() + "님 로그인 중");
        }

        categoryBox.getItems().add("전체");
        categoryBox.getItems().addAll(DemoData.CATEGORIES);
        categoryBox.setValue("전체");

        distanceBox.getItems().setAll(DemoData.DISTANCES);
        distanceBox.setValue("1km");

        Widgets.fillChips(moodPane,
                List.of("스트레스 해소", "속풀이·해장", "가벼운 식사", "든든한 한끼"), moodChips);

        fillCards();
    }

    private void fillCards() {
        cardBox.getChildren().clear();
        for (DemoData.Recommendation item : DemoData.RECOMMENDATIONS) {
            cardBox.getChildren().add(card(item));
        }
    }

    private VBox card(DemoData.Recommendation item) {
        Label menu = new Label(item.menu());
        menu.getStyleClass().add("menu-name");

        HBox titleRow = new HBox(10, menu, Widgets.rankBadge(item.rank(), item.score()));
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label restaurant = new Label(item.restaurant());
        restaurant.getStyleClass().add("restaurant-name");

        HBox badgeRow = new HBox(8,
                Widgets.safetyBadge(item.safety()),
                Widgets.sub("매운맛 " + item.spicyLevel() + "단계"));
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        Label reason = new Label(item.reason());
        reason.setWrapText(true);
        VBox reasonPanel = new VBox(reason);
        reasonPanel.getStyleClass().add("reason-panel");

        HBox infoRow = new HBox(16,
                Widgets.sub(item.address()),
                Widgets.sub(item.hours()),
                Widgets.sub("★ " + item.rating()));
        infoRow.setAlignment(Pos.CENTER_LEFT);

        ToggleButton favorite = new ToggleButton("♡ 찜하기");
        favorite.getStyleClass().add("chip");
        favorite.selectedProperty().addListener((observable, before, on) ->
                favorite.setText(on ? "♥ 찜함" : "♡ 찜하기"));

        Button map = new Button("맛집/지도");
        map.setOnAction(event -> AppNav.dialog(
                item.menu() + " - 맛집 상세 및 지도 정보", "restaurant-detail.fxml",
                (RestaurantDetailController controller) -> controller.setItem(item)));

        Button record = new Button("기록하기");
        record.setOnAction(event -> {com.safefood.dto.UserDto me = com.safefood.dto.Session.getCurrentUser();

            if(me == null || me.getId() == -1){
                com.safefood.view.AppNav.warn("게스트는 기록을 저장할 수 없습니다.");
                return;
            }

            com.safefood.service.HistoryService historyService = new com.safefood.service.HistoryService();

            int realMenuIdInMyDb = 1;
            boolean success = historyService.saveEatenHistory(me.getId(), realMenuIdInMyDb);

            if(success){
                com.safefood.view.AppNav.info("기록되었습니다!");
            } else{
                com.safefood.view.AppNav.error("저장에 실패했습니다.");
            }

        }
        );
        record.setDisable(item.blocked());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8, favorite, spacer, map, record);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, titleRow, restaurant, badgeRow, reasonPanel, infoRow, actions);
        card.getStyleClass().add("card");

        if (item.blocked()) {
            card.getStyleClass().add("blocked");
            if (item.alternative() != null) {
                card.getChildren().add(4, Widgets.sub("대체 메뉴 — " + item.alternative()));
            }
        }
        return card;
    }

    @FXML
    private void handleProfile() {
        AppNav.show("프로필 및 설정 관리", "profile.fxml");
    }

    @FXML
    private void handleHistory() {
        AppNav.show("식사 기록 및 즐겨찾기", "history.fxml");
    }

    @FXML
    private void handleLogout() {
        AppNav.show("로그인", "login.fxml");
    }

    @FXML
    private void handleReroll() {

        fillCards();
    }

    @FXML
    private void handleRoulette() {
        AppNav.info("후보 중 무작위로 하나를 고르는 기능입니다. (R-08, 3차 구현)");
    }

    @FXML
    private void handleGroup() {
        AppNav.dialog("같이 먹기 옵션 선택", "group-option.fxml");
    }
}
