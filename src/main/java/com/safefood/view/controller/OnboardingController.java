package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class OnboardingController {

    @FXML private Slider spicySlider;
    @FXML private Label spicyValue;
    @FXML private FlowPane categoryPane;
    @FXML private ComboBox<String> budgetBox;
    @FXML private ComboBox<String> distanceBox;
    @FXML private FlowPane allergyPane;
    @FXML private VBox severityBox;

    private final List<ToggleButton> categoryChips = new ArrayList<>();
    private final List<ToggleButton> allergyChips = new ArrayList<>();

    @FXML
    private void initialize() {
        spicySlider.valueProperty().addListener((observable, before, after) ->
                spicyValue.setText((int) spicySlider.getValue() + "단계"));

        budgetBox.getItems().setAll(DemoData.BUDGETS);
        budgetBox.setValue("12,000원 이하");
        distanceBox.getItems().setAll(DemoData.DISTANCES);
        distanceBox.setValue("1km");

        Widgets.fillChips(categoryPane, DemoData.CATEGORIES, categoryChips);
        Widgets.fillChips(allergyPane, DemoData.ALLERGIES, allergyChips);

        for (ToggleButton chip : allergyChips) {
            chip.selectedProperty().addListener((observable, before, after) -> rebuildSeverity());
        }
        rebuildSeverity();
    }

    private void rebuildSeverity() {
        severityBox.getChildren().clear();

        List<String> picked = Widgets.selected(allergyChips);
        if (picked.isEmpty()) {
            severityBox.getChildren().add(Widgets.sub("알레르기를 선택하면 심각도를 지정할 수 있습니다."));
            return;
        }

        Label title = new Label("심각도 (" + picked.size() + "건)");
        title.getStyleClass().add("section-title");
        severityBox.getChildren().add(title);

        for (String name : picked) {
            Label label = new Label(name);
            label.setMinWidth(90);

            ChoiceBox<String> severity = new ChoiceBox<>();
            severity.getItems().setAll(DemoData.SEVERITIES);
            severity.setValue(DemoData.SEVERITIES.get(2));

            HBox row = new HBox(10, label, severity);
            row.setAlignment(Pos.CENTER_LEFT);
            severityBox.getChildren().add(row);
        }
    }

    @FXML
    private void handleStart() {

        AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
    }
}
