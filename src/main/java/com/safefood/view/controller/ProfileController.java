package com.safefood.view.controller;

import com.safefood.service.ProfileService;
import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileController {

    @FXML private FlowPane ownedPane;
    @FXML private FlowPane severityPane;
    @FXML private FlowPane categoryPane;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> distanceBox;
    @FXML private Slider priceMin;
    @FXML private Slider priceMax;
    @FXML private Label priceLabel;
    @FXML private Slider spicySlider;
    @FXML private Label spicyValue;

    private final List<ToggleButton> categoryChips = new ArrayList<>();
    private final ToggleGroup severityGroup = new ToggleGroup();

    @FXML
    private void initialize() {
        // 로그인이 안되면 띄우지 않음
        if(com.safefood.dto.Session.getCurrentUser() == null){
            return;
        }

        int myId = com.safefood.dto.Session.getCurrentUser().getId();
        com.safefood.service.ProfileService profileService = new com.safefood.service.ProfileService();
        
        // 내 알레르기 불러와서 화면에 추가
        Map<String, Integer> myAllergies = profileService.getMyAllergies(myId);
        if(myAllergies != null){
            for (Map.Entry<String, Integer> entry : myAllergies.entrySet()) {
                addOwned(entry.getKey(), entry.getValue());
            }
        }

        // 심각도
        for (int i = 1; i <= 5; i++){
            ToggleButton level = Widgets.chip("Class " + i);
            level.setToggleGroup(severityGroup);
            level.setUserData(i);
            if( i == 3) level.setSelected(true);
            severityPane.getChildren().add(level);
        }

        // 내 카테고리(선호 음식) 불러오기, 체크
        Widgets.fillChips(categoryPane, DemoData.CATEGORIES, categoryChips);
        List<String> myCategories = profileService.getMyCategories(myId);
        if(myCategories != null){
            for (ToggleButton chip : categoryChips) {
                if(myCategories.contains(chip.getText())){
                    chip.setSelected(true);
                }
            }
        }

        // 내 취향(에산, 매운맛, 거리) 불러오기
        distanceBox.getItems().setAll(DemoData.DISTANCES); // 콤보박스 아이템 세팅
        com.safefood.dto.PreferenceDto pref = profileService.getMyPreference(myId);
        if(pref != null){
            priceMax.setValue(pref.getPriceMax());
            spicySlider.setValue(pref.getSpicyLevel());
            
            // 거리 콤보박스 세팅(DB 숫자를 텍스트로 변환
            switch(pref.getMaxDistance()){
                case 3000: distanceBox.getSelectionModel().select("3km"); break;
                case 2000: distanceBox.getSelectionModel().select("2km"); break;
                case 1000: distanceBox.getSelectionModel().select("1km"); break;
                case 500: distanceBox.getSelectionModel().select("500m"); break;
                default: distanceBox.getSelectionModel().select("제한없음"); break;
            }

            priceMin.valueProperty().addListener((observable, before, after) -> syncPrice());
            priceMax.valueProperty().addListener((observable, before, after) -> syncPrice());
            syncPrice();
            spicySlider.valueProperty().addListener((observable, before, after) -> spicyValue.setText((int) spicySlider.getValue() + "단계"));
            spicyValue.setText((int) spicySlider.getValue() + "단계");

        }

    }

    private void addOwned(String name, int severity) {
        Label label = new Label(name + "  Class " + severity);

        Button remove = new Button("✕");
        remove.getStyleClass().add("icon");

        HBox chip = new HBox(6, label, remove);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("chip");
        remove.setOnAction(event -> ownedPane.getChildren().remove(chip));

        ownedPane.getChildren().add(chip);
    }

    @FXML
    private void handleSearch() {
        AppNav.info("allergy 마스터 19종에서 이름으로 찾는 자리입니다.");
    }

    @FXML
    private void handleAddAllergy() {
        String name = searchField.getText().trim();
        if (name.isEmpty()) {
            AppNav.warn("추가할 알레르기 이름을 입력해 주세요.");
            return;
        }
        if (!DemoData.ALLERGIES.contains(name)) {
            AppNav.warn("알레르기 마스터에 없는 항목입니다. 19종 중에서 골라 주세요.");
            return;
        }
        Object picked = severityGroup.getSelectedToggle() == null
                ? 3 : severityGroup.getSelectedToggle().getUserData();

        addOwned(name, (int) picked);
        searchField.clear();
    }

    @FXML
    private void handleSave() {

        AppNav.info("저장되었습니다. 다음 추천부터 반영됩니다.");
    }

    @FXML
    private void handleEditInfo() {
        AppNav.dialog("정보 변경", "edit-info.fxml");
    }

    @FXML
    private void handleMain() {
        AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
    }

    @FXML
    private void handleHistory() {
        AppNav.show("식사 기록 및 즐겨찾기", "history.fxml");
    }

    @FXML
    private void handleLogout() {
        AppNav.show("로그인", "login.fxml");
    }

    private void syncPrice() {
        if (priceMin.getValue() > priceMax.getValue()) {
            priceMin.setValue(priceMax.getValue());
        }
        priceLabel.setText(String.format("%,d원 ~ %,d원",
                (int) priceMin.getValue(), (int) priceMax.getValue()));
    }
}
