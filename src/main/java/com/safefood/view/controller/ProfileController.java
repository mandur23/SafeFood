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

            // 화면 켜지자마자 글씨 띄우기
            int initialPrice = (int) Math.round(priceMax.getValue() / 1000.0) * 1000;
            priceLabel.setText(String.format("최대 %,d원 이하", initialPrice));

            // 가격 설정
            priceMax.valueProperty().addListener((observable, before, after) -> {
                // 1000원 단위 반올림
                int roundedPrice = (int) Math.round(after.doubleValue() / 1000.0) * 1000;
                
                // 무한 루프 방지
                if (priceMax.getValue() != roundedPrice) {
                    priceMax.setValue(roundedPrice);
                }

                // 라벨 텍스트 업데이트
                priceLabel.setText(String.format("최대 %,d원 이하", roundedPrice));
            });

            // 맵기 설정
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
        remove.setOnAction(event -> {
            ownedPane.getChildren().remove(chip);
            saveAllergies(); // 지울 때도 알레르기 즉시 저장
        });

        ownedPane.getChildren().add(chip);
    }

    @FXML
    private void handleSearch() {
        String allList = String.join(", ", DemoData.ALLERGIES);
        AppNav.info("[지원되는 알레르기 목록 19종]\n\n" + allList);
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

        // 방금 추가된 것을 포함하여 전체 알레르기 다시 저장
        saveAllergies();
        AppNav.info("알레르기 정보가 저장되었습니다.");
    }

    private void saveAllergies() {
        int myId = com.safefood.dto.Session.getCurrentUser().getId();
        java.util.Map<String, Integer> myAllergies = new java.util.HashMap<>();
        for (javafx.scene.Node node : ownedPane.getChildren()) {
            if(node instanceof javafx.scene.layout.HBox){
                javafx.scene.layout.HBox chip = (javafx.scene.layout.HBox) node;
                javafx.scene.control.Label label = (javafx.scene.control.Label) chip.getChildren().get(0);
                String[] parts = label.getText().split("  Class ");
                if(parts.length == 2){
                    myAllergies.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        }
        ProfileService profileService = new ProfileService();
        profileService.updateAllergiesOnly(myId, myAllergies);
    }

    @FXML
    private void handleSave() {
        int myId =  com.safefood.dto.Session.getCurrentUser().getId();

        // 1. 매운맛, 예산, 거리 파싱
        int spicyLevel = (int) spicySlider.getValue();
        int priceLimit = (int) priceMax.getValue();

        int maxDistance = 0;
        String distStr = distanceBox.getValue();
        if("500m".equals(distStr)){ maxDistance = 500;}
        else if("1km".equals(distStr)){ maxDistance = 1000;}
        else if("2km".equals(distStr)){ maxDistance = 2000;}
        else if("3km".equals(distStr)){ maxDistance = 3000;}

        // 2. 카테고리 파싱
        List<String> myCategories = Widgets.selected(categoryChips);

        // 3. 취향/조건 서비스 호출
        ProfileService profileService = new ProfileService();
        profileService.updatePreferencesOnly(myId, spicyLevel, priceLimit, maxDistance, myCategories);
        AppNav.info("취향 및 조건이 저장되었습니다. 다음 추천부터 반영됩니다.");
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

}
