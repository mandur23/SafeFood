package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

public class RestaurantDetailController {

    @FXML private VBox root;
    @FXML private Label restaurantName;
    @FXML private Label menuName;
    @FXML private TextArea reasonArea;
    @FXML private Label addressLabel;
    @FXML private Label hoursLabel;
    @FXML private Label ratingLabel;
    @FXML private ToggleButton favoriteButton;

    @FXML
    private void initialize() {
        favoriteButton.selectedProperty().addListener((observable, before, on) ->
                favoriteButton.setText(on ? "♥ 찜함" : "♡ 찜하기"));
    }

    public void setItem(DemoData.Recommendation item) {
        restaurantName.setText(item.restaurant());
        menuName.setText(item.menu());
        reasonArea.setText(item.reason());
        addressLabel.setText(item.address());
        hoursLabel.setText(item.hours());
        ratingLabel.setText("★ " + item.rating() + "  (리뷰 312)");
    }

    @FXML
    private void handleRoute() {

        AppNav.info("외부 지도 앱으로 연결하는 기능입니다. (3차 구현 예정)");
    }

    @FXML
    private void handleClose() {
        AppNav.close(root);
    }
}
