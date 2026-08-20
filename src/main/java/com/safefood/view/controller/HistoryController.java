package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

public class HistoryController {

    @FXML private ToggleGroup tabGroup;
    @FXML private ToggleButton historyTab;
    @FXML private ComboBox<String> filterBox;
    @FXML private ListView<com.safefood.dto.HistoryDto> historyList;
    @FXML private ListView<DemoData.FavoriteRow> favoriteList;
    @FXML private Label userLabel;

    @FXML
    private void initialize() {
        if (com.safefood.dto.Session.getCurrentUser() != null) {
            String myNick = com.safefood.dto.Session.getCurrentUser().getNickname();
            userLabel.setText(myNick + "님 로그인 중");
        }

        tabGroup.selectedToggleProperty().addListener((observable, before, after) -> {
            if (after == null && before != null) {
                tabGroup.selectToggle(before);
            }
        });

        filterBox.getItems().setAll("전체 보기", "추천받음", "먹음", "조회함", "차단됨");
        filterBox.setValue("전체 보기");

        setUpHistoryList();
        setUpFavoriteList();

        historyTab.selectedProperty().addListener((observable, before, isHistory) -> {
            show(historyList, isHistory);
            show(favoriteList, !isHistory);
            filterBox.setDisable(!isHistory);
        });
    }

    private static void show(ListView<?> list, boolean visible) {
        list.setVisible(visible);
        list.setManaged(visible);
    }

    private void setUpHistoryList() {
        com.safefood.dto.UserDto me = com.safefood.dto.Session.getCurrentUser();
        java.util.List<com.safefood.dto.HistoryDto> realData = (me != null && me.getId() != -1) 
                ? new com.safefood.service.HistoryService().getHistories(me.getId())
                : new java.util.ArrayList<>();

        historyList.setItems(FXCollections.observableArrayList(realData));
        historyList.setPlaceholder(Widgets.sub("아직 기록이 없습니다."));
        historyList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(com.safefood.dto.HistoryDto row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                Label menu = new Label(row.getMenu() + "  ·  " + row.getRestaurant());
                menu.getStyleClass().add("section-title");

                VBox text = new VBox(2, menu, Widgets.sub(row.getDate() + "  ·  " + row.getNote()));
                HBox.setHgrow(text, Priority.ALWAYS);

                Button rate = new Button("평가하기");
                rate.getStyleClass().add("outline");
                boolean eaten = "EATEN".equals(row.getType());
                rate.setVisible(eaten);
                rate.setManaged(eaten);
                rate.setOnAction(event ->
                        AppNav.info("좋아요 / 만족도 1~5점을 입력받는 자리입니다. (H-04)"));

                HBox cell = new HBox(12, Widgets.statusLabel(row.getType()), text, rate);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
            }
        });
    }

    private void setUpFavoriteList() {
        favoriteList.setItems(FXCollections.observableArrayList(DemoData.FAVORITES));
        favoriteList.setPlaceholder(Widgets.sub("찜한 가게가 없습니다."));
        favoriteList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(DemoData.FavoriteRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                Label name = new Label(row.restaurant());
                name.getStyleClass().add("section-title");

                VBox text = new VBox(2, name,
                        Widgets.sub("대표 메뉴 " + row.menu()
                                + "  ·  ★ " + row.rating()
                                + "  ·  리뷰 " + row.reviewCount()));
                HBox.setHgrow(text, Priority.ALWAYS);

                Button remove = new Button("♥ 찜 취소");
                remove.getStyleClass().add("outline");
                remove.setOnAction(event -> favoriteList.getItems().remove(row));

                HBox cell = new HBox(12, text, remove);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
            }
        });
    }

    @FXML
    private void handleMain() {
        AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
    }

    @FXML
    private void handleProfile() {
        AppNav.show("프로필 및 설정 관리", "profile.fxml");
    }

    @FXML
    private void handleLogout() {
        AppNav.show("로그인", "login.fxml");
    }
}
