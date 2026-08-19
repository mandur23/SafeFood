package com.safefood.view.controller;

import com.safefood.dto.UserDto;
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
    public static String newUserId;
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
        UserDto me = com.safefood.dto.Session.getCurrentUser();
        // 세션의 유저 아이디가 -1이면 게스트
        boolean isGuest = (me != null && me.getId() == -1);

        int currentUserId = -1;
        if (!isGuest) {
            // 정식 회원가입 직후 넘어온 경우 (DB에서 진짜 번호를 찾아와야 함)
            com.safefood.service.AuthService authService = new com.safefood.service.AuthService();
            me = authService.getUserInfo(newUserId);
            com.safefood.dto.Session.setCurrentUser(me);
            currentUserId = me.getId();
        }


        // 2. 취향 데이터 가져오기 및 파싱(숫자로 변환)
        int spicyLevel = (int) spicySlider.getValue(); // 이미 숫자(0~5)

        // 예산: "12,000원 이하" -> 12000 변환, "제한없음" -> 0
        String budgetStr = budgetBox.getValue();
        int priceMax = 0;
        if (!"제한없음".equals(budgetStr)) {
            priceMax = Integer.parseInt(budgetStr.replaceAll("[^0-9]", ""));
        }

        // 거리: "1km" -> 1000(미터) 변환, "제한없음" -> 0
        String distanceStr = distanceBox.getValue();
        int maxDistance = 0;
        if ("500m".equals(distanceStr)) maxDistance = 500;
        else if ("1km".equals(distanceStr)) maxDistance = 1000;
        else if ("2km".equals(distanceStr)) maxDistance = 2000;
        else if ("3km".equals(distanceStr)) maxDistance = 3000;

        // 카테고리 (문자열 리스트)
        List<String> preferredCategories = Widgets.selected(categoryChips);

        if (isGuest) {
            // DB에 안 넣고 세션에만 취향정보를 묶어서 저장
            com.safefood.dto.PreferenceDto guestPref = new com.safefood.dto.PreferenceDto(spicyLevel, 0, priceMax, maxDistance);
            guestPref.setSpicyLevel(spicyLevel);
            guestPref.setPriceMax(priceMax);
            guestPref.setMaxDistance(maxDistance);
            com.safefood.dto.Session.setGuestPreference(guestPref);

            // 알레르기 리스트도 나중에 필요하면 Session에 담을 수 있습니다.
            // 저장이 끝났으니 메인 화면이 아닌  '그룹 옵션 창'을 띄워줌!
            AppNav.dialog("같이 먹기 옵션 선택","group-option.fxml");
            return;
        }

        // --------------------------------------------------------------------- //

        // 💡 정식 회원 처리 (기존 로직)
        com.safefood.service.OnboardingService onboardingService = new com.safefood.service.
                OnboardingService();

        for (javafx.scene.Node node : severityBox.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                Label label = (Label) row.getChildren().get(0);
                @SuppressWarnings("unchecked")
                ChoiceBox<String> choice = (ChoiceBox<String>) row.getChildren().get(1);

                String allergyName = label.getText();
                String severityStr = choice.getValue();

                int allergyId = DemoData.ALLERGIES.indexOf(allergyName) + 1;
                int severity = DemoData.SEVERITIES.indexOf(severityStr) + 1;

                onboardingService.saveAllergy(currentUserId, allergyId, severity);
            }
        }

        onboardingService.savePreferences(currentUserId, spicyLevel, priceMax, maxDistance,
                preferredCategories);
        AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
    }
}
