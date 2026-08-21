package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 온보딩 — 3단계 스텝 (리디자인 1d).
 *
 * <p>단계마다 기본값이 들어 있어서 '다음'만 눌러도 끝나고, '나중에 할게요'는 지금 화면까지의
 * 값을 그대로 저장하고 메인으로 보냅니다 — 어느 쪽이든 빈 프로필이 만들어지지 않습니다.
 */
public class OnboardingController {

    public static String newUserId;

    private static final int LAST_STEP = 3;

    /** 단계별 제목·부제 — 인덱스 0이 1단계. */
    private static final String[][] HEADINGS = {
            {"무슨 음식을 좋아하세요?", "고른 종류가 추천 순위에서 먼저 올라와요."},
            {"못 드시는 재료가 있나요?",
                    "식약처 고시 19종 · 한 명이라도 못 먹는 메뉴는 추천에서 빠져요."},
            {"예산과 거리는요?", "여기까지만 정하면 바로 추천을 시작할 수 있어요."}
    };

    @FXML private Label dot1;
    @FXML private Label dot2;
    @FXML private Label dot3;
    @FXML private Region line1;
    @FXML private Region line2;
    @FXML private Label name1;
    @FXML private Label name2;
    @FXML private Label name3;

    @FXML private Label stepTitle;
    @FXML private Label stepSubtitle;

    @FXML private VBox step1;
    @FXML private VBox step2;
    @FXML private VBox step3;

    @FXML private Slider spicySlider;
    @FXML private Label spicyValue;
    @FXML private FlowPane categoryPane;

    @FXML private FlowPane allergyPane;
    @FXML private VBox severityBox;

    @FXML private ComboBox<String> budgetBox;
    @FXML private HBox distanceSeg;

    @FXML private Button prevButton;
    @FXML private Button nextButton;

    private final List<ToggleButton> categoryChips = new ArrayList<>();
    private final List<ToggleButton> allergyChips = new ArrayList<>();
    private final ToggleGroup distanceGroup = new ToggleGroup();

    /** 고른 알레르기 → 심각도(1~5). 칩을 껐다 켜도 값이 살아 있게 여기에 들고 있습니다. */
    private final Map<String, Integer> severityByAllergy = new LinkedHashMap<>();

    private int step = 1;

    @FXML
    private void initialize() {
        spicySlider.valueProperty().addListener((observable, before, after) ->
                spicyValue.setText((int) spicySlider.getValue() + "단계"));

        budgetBox.getItems().setAll(DemoData.BUDGETS);
        budgetBox.setValue("12,000원 이하");

        fillDistanceSeg();

        Widgets.fillChips(categoryPane, DemoData.CATEGORIES, categoryChips);
        Widgets.fillChips(allergyPane, DemoData.ALLERGIES, allergyChips);

        for (ToggleButton chip : allergyChips) {
            chip.selectedProperty().addListener((observable, before, on) -> {
                if (!on) {
                    severityByAllergy.remove(chip.getText());
                } else {
                    severityByAllergy.putIfAbsent(chip.getText(), 3);   // 기본 '보통'
                }
                rebuildSeverity();
            });
        }
        rebuildSeverity();

        renderStep();
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
        // 세그먼티드 컨트롤은 '선택 없음'이 없습니다 — 다시 누르면 그대로 둡니다
        distanceGroup.selectedToggleProperty().addListener((observable, before, after) -> {
            if (after == null && before != null) {
                distanceGroup.selectToggle(before);
            }
        });
    }

    /** 고른 알레르기마다 심각도 카드 한 장 — 목업의 "새우 — 얼마나 심한가요?" 블록. */
    private void rebuildSeverity() {
        severityBox.getChildren().clear();

        if (severityByAllergy.isEmpty()) {
            severityBox.getChildren().add(
                    Widgets.sub("해당하는 재료를 고르면 심각도를 지정할 수 있어요. 없으면 그냥 넘어가세요."));
            return;
        }

        for (Map.Entry<String, Integer> entry : severityByAllergy.entrySet()) {
            severityBox.getChildren().add(severityCard(entry.getKey(), entry.getValue()));
        }
        severityBox.getChildren().add(
                Widgets.micro("심각할수록 '혼입 가능' 메뉴의 순위를 크게 낮춰요."));
    }

    private VBox severityCard(String allergy, int current) {
        Label title = Widgets.label(allergy + " — 얼마나 심한가요?", "section-title");

        ToggleGroup group = new ToggleGroup();
        HBox seg = new HBox();
        seg.setAlignment(Pos.CENTER_LEFT);
        for (int level = 1; level <= DemoData.SEVERITIES.size(); level++) {
            // "Class 3 보통" → "3 보통" (세그먼트 칸이 좁아 접두어를 뗍니다)
            String label = DemoData.SEVERITIES.get(level - 1).replace("Class ", "");
            ToggleButton option =
                    Widgets.segOption(label, level == 1, level == DemoData.SEVERITIES.size());
            option.setToggleGroup(group);
            option.setUserData(level);
            option.setSelected(level == current);
            seg.getChildren().add(option);
        }
        group.selectedToggleProperty().addListener((observable, before, after) -> {
            if (after == null) {
                group.selectToggle(before);      // 한 칸은 늘 켜져 있어야 합니다
                return;
            }
            severityByAllergy.put(allergy, (int) after.getUserData());
        });

        VBox card = new VBox(10, title, seg);
        card.getStyleClass().addAll("card", "plain");
        return card;
    }

    // ── 단계 이동 ───────────────────────────────────────────────

    @FXML
    private void handleNext() {
        if (step < LAST_STEP) {
            step++;
            renderStep();
            return;
        }
        save();
        AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
    }

    @FXML
    private void handlePrev() {
        if (step > 1) {
            step--;
            renderStep();
        }
    }

    /** 건너뛰기 — 지금까지 고른 값(대부분 기본값)을 그대로 저장하고 넘어갑니다. */
    @FXML
    private void handleSkip() {
        save();
        AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
    }

    private void renderStep() {
        show(step1, step == 1);
        show(step2, step == 2);
        show(step3, step == 3);

        stepTitle.setText(HEADINGS[step - 1][0]);
        stepSubtitle.setText(HEADINGS[step - 1][1]);

        paintDot(dot1, 1);
        paintDot(dot2, 2);
        paintDot(dot3, 3);
        paintLine(line1, step > 1);
        paintLine(line2, step > 2);
        paintName(name1, 1);
        paintName(name2, 2);
        paintName(name3, 3);

        prevButton.setDisable(step == 1);
        nextButton.setText(step == LAST_STEP ? "추천 시작하기 →" : "다음 →");
    }

    private void paintDot(Label dot, int index) {
        dot.getStyleClass().removeAll("done", "now");
        if (index < step) {
            dot.getStyleClass().add("done");
            dot.setText("✓");
        } else {
            dot.setText(String.valueOf(index));
            if (index == step) {
                dot.getStyleClass().add("now");
            }
        }
    }

    private static void paintLine(Region line, boolean done) {
        line.getStyleClass().remove("done");
        if (done) {
            line.getStyleClass().add("done");
        }
    }

    private void paintName(Label name, int index) {
        name.getStyleClass().removeAll("sub", "accent-text");
        name.getStyleClass().add(index == step ? "accent-text" : "sub");
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // ── 저장 ────────────────────────────────────────────────────

    /**
     * 화면의 값을 DB로 넘깁니다.
     *
     * <p>단계를 끝까지 밟지 않고 건너뛰어도 호출되므로, 손대지 않은 칸은 화면의 기본값
     * (맵기 2단계 · 12,000원 · 1km) 이 그대로 저장됩니다.
     */
    private void save() {
        // 1. 방금 가입한 아이디로 실제 회원 번호(user_id)를 DB에서 찾아오기
        com.safefood.service.AuthService authService = new com.safefood.service.AuthService();
        com.safefood.dto.UserDto user = authService.getUserInfo(newUserId);

        // 세션 로그인 상태 등록
        com.safefood.dto.Session.setCurrentUser(user);
        int currentUserId = user.getId();

        // 2. 취향 데이터 파싱
        int spicyLevel = (int) spicySlider.getValue();

        // 예산: "12,000원 이하" -> 12000, "제한없음" -> 0
        String budgetStr = budgetBox.getValue();
        int priceMax = 0;
        if (!"제한없음".equals(budgetStr)) {
            priceMax = Integer.parseInt(budgetStr.replaceAll("[^0-9]", ""));
        }

        int maxDistance = parseDistance(selectedDistance());
        List<String> preferredCategories = Widgets.selected(categoryChips);

        // 3. 알레르기 심각도 저장
        com.safefood.service.OnboardingService onboardingService =
                new com.safefood.service.OnboardingService();

        for (Map.Entry<String, Integer> entry : severityByAllergy.entrySet()) {
            int allergyId = DemoData.ALLERGIES.indexOf(entry.getKey()) + 1;
            onboardingService.saveAllergy(currentUserId, allergyId, entry.getValue());
        }

        // 4. 취향 및 카테고리 저장
        onboardingService.savePreferences(
                currentUserId, spicyLevel, priceMax, maxDistance, preferredCategories);
    }

    private String selectedDistance() {
        ToggleButton picked = (ToggleButton) distanceGroup.getSelectedToggle();
        return picked == null ? "1km" : picked.getText();
    }

    /** "1km" → 1000(미터), "제한없음" → 0 */
    private static int parseDistance(String label) {
        return switch (label) {
            case "500m" -> 500;
            case "1km" -> 1000;
            case "2km" -> 2000;
            case "3km" -> 3000;
            default -> 0;
        };
    }
}
