package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.List;

public class GroupConditionController {

    @FXML private ScrollPane root;
    @FXML private FlowPane allergyPane;
    @FXML private Slider spicySlider;
    @FXML private Label spicyValue;
    @FXML private ComboBox<String> budgetBox;
    @FXML private Label locationLabel;

    private final List<ToggleButton> allergyChips = new ArrayList<>();

    @FXML
    private void initialize() {
        Widgets.fillChips(allergyPane, DemoData.ALLERGIES, allergyChips);

        Widgets.preselect(allergyChips, List.of("새우", "땅콩"));

        spicySlider.valueProperty().addListener((observable, before, after) ->
                spicyValue.setText((int) spicySlider.getValue() + "단계"));

        budgetBox.getItems().setAll(DemoData.BUDGETS);
        budgetBox.setValue("10,000원 이하");
    }

    @FXML
    private void handlePickLocation() {
        locationLabel.setText("중간 지점 계산에 쓸 좌표를 받는 자리입니다. (G-08, 3차 구현)");
    }

    @FXML
    private void handleReady() {

        AppNav.close(root);
        AppNav.show("SafeFood — 그룹 대기실", "waiting-room.fxml");
    }
}
