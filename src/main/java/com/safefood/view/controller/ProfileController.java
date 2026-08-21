package com.safefood.view.controller;

import com.safefood.dto.PreferenceDto;
import com.safefood.dto.Session;
import com.safefood.dto.UserDto;
import com.safefood.service.ProfileService;
import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 내 프로필 — 2열 배치, 심각도는 칩 안에, 저장 버튼은 없앴습니다 (리디자인 1h).
 *
 * <p>바뀐 값은 곧바로 DB로 갑니다. 슬라이더처럼 값이 연달아 바뀌는 입력은
 * {@link PauseTransition} 으로 잠깐 멎기를 기다렸다가 한 번만 저장합니다 — 안 그러면
 * 손잡이를 끄는 동안 수십 번 쓰게 됩니다.
 */
public class ProfileController {

    /** 값이 멎었다고 볼 시간 — 이만큼 조용하면 저장합니다. */
    private static final Duration SETTLE = Duration.millis(500);

    @FXML private FlowPane ownedPane;
    @FXML private FlowPane categoryPane;
    @FXML private TextField searchField;
    @FXML private HBox distanceSeg;
    @FXML private Slider priceMax;
    @FXML private Label priceLabel;
    @FXML private Slider spicySlider;
    @FXML private Label spicyLabel;
    @FXML private Label userLabel;
    @FXML private Label autoSaveTag;
    @FXML private Label allergyHint;

    private final List<ToggleButton> categoryChips = new ArrayList<>();
    private final ToggleGroup distanceGroup = new ToggleGroup();

    /** 화면에 붙어 있는 알레르기 → 심각도. 저장할 때 이걸 그대로 넘깁니다. */
    private final Map<String, Integer> owned = new LinkedHashMap<>();

    private final PauseTransition preferenceSettle = new PauseTransition(SETTLE);

    /** 초기 로딩 중에는 저장하지 않습니다 — 값을 채우는 것도 '변경'으로 잡히기 때문입니다. */
    private boolean loading = true;

    @FXML
    private void initialize() {
        fillDistanceSeg();
        Widgets.fillChips(categoryPane, DemoData.CATEGORIES, categoryChips);
        preferenceSettle.setOnFinished(event -> savePreferences());

        UserDto me = Session.getCurrentUser();
        if (me == null) {
            // 게스트는 저장할 계정이 없습니다 — 화면은 보여 주되 자동 저장은 끕니다
            userLabel.setText(com.safefood.network.GroupSession.get().displayName() + "님");
            autoSaveTag.setText("게스트 — 저장되지 않아요");
            autoSaveTag.getStyleClass().remove("accent2");
            autoSaveTag.getStyleClass().add("neutral");
            renderPriceLabel();
            renderSpicyLabel();
            renderOwned();
            wireAutoSave();
            loading = false;
            return;
        }

        userLabel.setText(me.getNickname() + "님");
        ProfileService profileService = new ProfileService();

        Map<String, Integer> myAllergies = profileService.getMyAllergies(me.getId());
        if (myAllergies != null) {
            owned.putAll(myAllergies);
        }

        List<String> myCategories = profileService.getMyCategories(me.getId());
        if (myCategories != null) {
            Widgets.preselect(categoryChips, myCategories);
        }

        PreferenceDto pref = profileService.getMyPreference(me.getId());
        if (pref != null) {
            priceMax.setValue(pref.getPriceMax());
            spicySlider.setValue(pref.getSpicyLevel());
            selectDistance(pref.getMaxDistance());
        }

        renderPriceLabel();
        renderSpicyLabel();
        renderOwned();
        wireAutoSave();
        loading = false;
    }

    /** 값이 바뀌면 저장으로 이어지도록 묶습니다 — 로딩이 끝난 뒤에 붙입니다. */
    private void wireAutoSave() {
        priceMax.valueProperty().addListener((observable, before, after) -> {
            // 1000원 단위로 스냅 — 라벨과 저장값이 같은 눈금을 쓰게
            int rounded = (int) Math.round(after.doubleValue() / 1000.0) * 1000;
            if ((int) priceMax.getValue() != rounded) {
                priceMax.setValue(rounded);
                return;   // 되돌아온 변경에서 이어서 처리됩니다
            }
            renderPriceLabel();
            schedulePreferenceSave();
        });

        spicySlider.valueProperty().addListener((observable, before, after) -> {
            renderSpicyLabel();
            schedulePreferenceSave();
        });

        for (ToggleButton chip : categoryChips) {
            chip.selectedProperty().addListener((observable, before, on) -> schedulePreferenceSave());
        }

        distanceGroup.selectedToggleProperty().addListener((observable, before, after) -> {
            if (after == null && before != null) {
                distanceGroup.selectToggle(before);
                return;
            }
            schedulePreferenceSave();
        });
    }

    private void fillDistanceSeg() {
        distanceSeg.getChildren().clear();
        List<String> options = DemoData.DISTANCES;
        for (int i = 0; i < options.size(); i++) {
            ToggleButton option =
                    Widgets.segOption(options.get(i), i == 0, i == options.size() - 1);
            option.setToggleGroup(distanceGroup);
            if ("1km".equals(options.get(i))) {
                option.setSelected(true);
            }
            distanceSeg.getChildren().add(option);
        }
    }

    private void selectDistance(int meters) {
        String label = switch (meters) {
            case 500 -> "500m";
            case 1000 -> "1km";
            case 2000 -> "2km";
            case 3000 -> "3km";
            default -> "제한없음";
        };
        for (javafx.scene.Node node : distanceSeg.getChildren()) {
            ToggleButton option = (ToggleButton) node;
            option.setSelected(option.getText().equals(label));
        }
    }

    // ── 알레르기 칩 ─────────────────────────────────────────────

    /** 칩 하나가 이름 + 심각도 선택 + 삭제를 다 갖습니다 — 따로 심각도 칸을 두지 않습니다. */
    private void renderOwned() {
        ownedPane.getChildren().clear();
        if (owned.isEmpty()) {
            ownedPane.getChildren().add(Widgets.sub("등록한 알레르기가 없습니다."));
            return;
        }
        for (Map.Entry<String, Integer> entry : owned.entrySet()) {
            ownedPane.getChildren().add(allergyChip(entry.getKey(), entry.getValue()));
        }
    }

    private HBox allergyChip(String name, int severity) {
        Label label = new Label(name + " · 심각");

        ChoiceBox<Integer> level = new ChoiceBox<>();
        level.getItems().setAll(1, 2, 3, 4, 5);
        level.setValue(severity);
        level.valueProperty().addListener((observable, before, after) -> {
            if (after == null) {
                return;
            }
            owned.put(name, after);
            saveAllergies();
        });

        Button remove = new Button("✕");
        remove.getStyleClass().addAll("ghost-quiet");
        remove.setOnAction(event -> {
            owned.remove(name);
            renderOwned();
            saveAllergies();
        });

        HBox chip = new HBox(6, label, level, remove);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().addAll("tag", "accent");
        return chip;
    }

    @FXML
    private void handleAddAllergy() {
        String name = searchField.getText().trim();
        if (name.isEmpty()) {
            hint("추가할 알레르기 이름을 입력해 주세요.");
            return;
        }
        if (!DemoData.ALLERGIES.contains(name)) {
            hint("알레르기 마스터에 없는 항목입니다. 식약처 고시 19종 중에서 골라 주세요.");
            return;
        }
        if (owned.containsKey(name)) {
            hint("이미 등록한 알레르기입니다.");
            return;
        }

        owned.put(name, 3);   // 기본 '보통' — 칩에서 바로 바꿀 수 있습니다
        searchField.clear();
        Widgets.hideError(allergyHint);
        renderOwned();
        saveAllergies();
    }

    private void hint(String message) {
        Widgets.showError(allergyHint, message);
    }

    // ── 저장 ────────────────────────────────────────────────────

    private void schedulePreferenceSave() {
        if (loading) {
            return;
        }
        preferenceSettle.playFromStart();
    }

    private void saveAllergies() {
        if (loading) {
            return;
        }
        UserDto me = Session.getCurrentUser();
        if (me == null) {
            return;   // 게스트 — 저장할 계정이 없습니다
        }
        new ProfileService().updateAllergiesOnly(me.getId(), new java.util.HashMap<>(owned));
    }

    private void savePreferences() {
        UserDto me = Session.getCurrentUser();
        if (me == null) {
            return;
        }
        new ProfileService().updatePreferencesOnly(me.getId(),
                (int) spicySlider.getValue(),
                (int) priceMax.getValue(),
                parseDistance(selectedDistance()),
                Widgets.selected(categoryChips));
    }

    private String selectedDistance() {
        ToggleButton picked = (ToggleButton) distanceGroup.getSelectedToggle();
        return picked == null ? "1km" : picked.getText();
    }

    private static int parseDistance(String label) {
        return switch (label) {
            case "500m" -> 500;
            case "1km" -> 1000;
            case "2km" -> 2000;
            case "3km" -> 3000;
            default -> 0;
        };
    }

    // ── 라벨 ────────────────────────────────────────────────────

    private void renderPriceLabel() {
        priceLabel.setText(String.format("한 끼 예산 상한 — %,d원", (int) priceMax.getValue()));
    }

    private void renderSpicyLabel() {
        spicyLabel.setText("선호 맵기 — " + (int) spicySlider.getValue() + "단계");
    }

    // ── 내비게이션 ──────────────────────────────────────────────

    @FXML
    private void handleEditInfo() {
        AppNav.dialog("정보 변경", "edit-info.fxml");
        // 정보 변경이 끝나면(팝업창이 닫히면) 즉시 라벨 텍스트 갱신
        UserDto me = Session.getCurrentUser();
        if (me != null) {
            userLabel.setText(me.getNickname() + "님");
        }
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
    private void handleGroup() {
        AppNav.dialog("같이 먹기", "group-option.fxml");
    }

    @FXML
    private void handleLogout() {
        AppNav.show("로그인", "login.fxml");
    }
}
