package com.safefood.view.controller;

import com.safefood.network.GroupClient;
import com.safefood.network.GroupSession;
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
        // 조건 입력 중 X로 닫으면 방에서 나갑니다 — 준비 안 된 유령 참여자로 남아 전원 READY를 막지 않게
        AppNav.onDialogClosed(root, () -> GroupSession.get().shutdown());

        Widgets.fillChips(allergyPane, DemoData.ALLERGIES, allergyChips);

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
        GroupClient client = GroupSession.get().client();
        if (client != null) {
            // INFO|알레르기,목록|매운맛|예산 → READY (전원 완료 시 서버가 병합·추천 시작)
            client.sendInfo(Widgets.selected(allergyChips),
                    (int) spicySlider.getValue(), parseBudget(budgetBox.getValue()));
            client.sendReady();
        }
        AppNav.close(root);
        AppNav.show("SafeFood — 그룹 대기실", "waiting-room.fxml");
    }

    /** "10,000원 이하" → 10000, "제한없음" → 0 */
    private static int parseBudget(String label) {
        if (label == null) {
            return 0;
        }
        String digits = label.replaceAll("\\D", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }
}
