package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

/**
 * 맛집 상세 (리디자인 1j) — 지도가 맨 위 전체 폭을 차지하고, 나머지 정보는 한 줄 메타로 접힙니다.
 */
public class RestaurantDetailController {

    @FXML private VBox root;
    @FXML private Label restaurantName;
    @FXML private Label menuName;
    @FXML private Label safetyTag;
    @FXML private Label metaLabel;
    @FXML private Label reasonLabel;
    @FXML private ToggleButton favoriteButton;

    private DemoData.Recommendation item;

    @FXML
    private void initialize() {
        favoriteButton.selectedProperty().addListener((observable, before, on) ->
                favoriteButton.setText(on ? "♥ 찜함" : "♡ 찜"));
    }

    public void setItem(DemoData.Recommendation item) {
        this.item = item;
        restaurantName.setText(item.restaurant());
        menuName.setText(item.menu());
        reasonLabel.setText(item.reason());

        safetyTag.setText(item.safety().shortLabel);
        safetyTag.getStyleClass().removeAll("safe", "possible", "contains");
        safetyTag.getStyleClass().add(item.safety().styleClass);

        // 주소 · 영업시간 · 평점 · 거리를 한 줄로
        metaLabel.setText(item.address()
                + " · 영업 " + item.hours()
                + " · ★ " + item.rating() + " (312)"
                + " · " + item.distance());
    }

    public void setItem(com.safefood.dto.RecommendationDto item) {
        restaurantName.setText(item.getRestaurant());
        menuName.setText(item.getMenu());
        reasonLabel.setText(item.getReason());

        if (item.getSafety() != null) {
            safetyTag.setText(item.getSafety().label);
            safetyTag.getStyleClass().removeAll("safe", "possible", "contains");
            safetyTag.getStyleClass().add(item.getSafety().styleClass);
        }

        String hours = item.getHours() != null ? item.getHours() : "";
        String address = item.getAddress() != null ? item.getAddress() : "";
        metaLabel.setText(address + " · 영업 " + hours + " · ★ " + item.getRating());
    }

    @FXML
    private void handleRoute() {
        AppNav.info("외부 지도 앱으로 연결하는 기능입니다. (3차 구현 예정)");
    }

    @FXML
    private void handleRecord() {
        String menu = item == null ? "이 메뉴" : item.menu();
        AppNav.success(menu + "을(를) 먹은 기록으로 남겼어요");
    }

    @FXML
    private void handleClose() {
        AppNav.close(root);
    }
}
