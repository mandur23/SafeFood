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
    @FXML private ListView<com.safefood.dto.FavoriteDto> favoriteList;
    @FXML private Label userLabel;

    private java.util.List<com.safefood.dto.HistoryDto> allHistories = new java.util.ArrayList<>();

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

        allHistories = (me != null && me.getId() != -1) 
                ? new com.safefood.service.HistoryService().getHistories(me.getId())
                : new java.util.ArrayList<>();

        // 초기 화면에 전체 데이터 띄움
        historyList.setItems(FXCollections.observableArrayList(allHistories));
        historyList.setPlaceholder(Widgets.sub("아직 기록이 없습니다."));

        // 콤보박스
        filterBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || "전체 보기".equals(newValue)) {
                historyList.setItems(FXCollections.observableArrayList(allHistories));
                return;
            }

            // 한글 메뉴를 ENUM으로
            String targetType = switch (newValue){
                case "추천받음" -> "RECOMMENDED";
                case "먹음" -> "EATEN";
                case "조회함" -> "VIEWED";
                case "차단됨" -> "BLOCKED";
                default -> "";
            };

            // 보관함에서 타입이 일치하는 것만 필터링
            java.util.List<com.safefood.dto.HistoryDto> filtered = new java.util.ArrayList<>();

            for (com.safefood.dto.HistoryDto dto : allHistories) {
                if (dto.getType().equals(targetType)) {
                    filtered.add(dto);
                }
            }

            historyList.setItems(FXCollections.observableArrayList(filtered));
        });

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
                
                if (row.getFeedbackId() > 0) {
                    rate.setDisable(true);
                    rate.setText("평가완료");
                }

                rate.setOnAction(event -> {
                    java.util.List<Integer> choices = java.util.List.of(5, 4, 3, 2, 1);
                    javafx.scene.control.ChoiceDialog<Integer> dialog = new javafx.scene.control.ChoiceDialog<>(5, choices);
                    dialog.setTitle("식사 평가");
                    dialog.setHeaderText(row.getMenu() + "은(는) 어떠셨나요?");
                    dialog.setContentText("별점을 선택해주세요 (1~5점):");
                    
                    java.util.Optional<Integer> result = dialog.showAndWait();
                    result.ifPresent(rating -> {
                        boolean success = new com.safefood.service.FeedbackService()
                                .saveFeedback(me.getId(), row.getHistoryId(), row.getMenuId(), rating);
                        if (success) {
                            com.safefood.view.AppNav.info("평가가 저장되었습니다!\n(앞으로 추천 알고리즘 가중치에 반영됩니다)");
                            rate.setDisable(true);
                            rate.setText("평가완료");
                        } else {
                            com.safefood.view.AppNav.error("평가 저장에 실패했습니다.");
                        }
                    });
                });

                HBox cell = new HBox(12, Widgets.statusLabel(row.getType()), text, rate);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
            }
        });
    }

    private void setUpFavoriteList() {
        com.safefood.dto.UserDto me = com.safefood.dto.Session.getCurrentUser();
        java.util.List<com.safefood.dto.FavoriteDto> realData = (me != null && me.getId() != -1) 
                ? new com.safefood.service.FavoriteService().getFavorites(me.getId())
                : new java.util.ArrayList<>();

        favoriteList.setItems(FXCollections.observableArrayList(realData));
        favoriteList.setPlaceholder(Widgets.sub("찜한 가게가 없습니다."));
        favoriteList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(com.safefood.dto.FavoriteDto row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                Label name = new Label(row.getRestaurant());
                name.getStyleClass().add("section-title");

                VBox text = new VBox(2, name,
                        Widgets.sub("대표 메뉴 " + row.getMenu()
                                + "  ·  ★ " + row.getRating()
                                + "  ·  리뷰 " + row.getReviewCount()));
                HBox.setHgrow(text, Priority.ALWAYS);

                Button remove = new Button("♥ 찜 취소");
                remove.getStyleClass().add("outline");
                remove.setOnAction(event -> {
                    new com.safefood.service.FavoriteService().removeFavorite(row.getFavoriteId());
                    favoriteList.getItems().remove(row);
                });

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
