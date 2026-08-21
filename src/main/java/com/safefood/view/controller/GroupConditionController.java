package com.safefood.view.controller;

import com.safefood.dto.PreferenceDto;
import com.safefood.dto.Session;
import com.safefood.dto.UserDto;
import com.safefood.network.GroupClient;
import com.safefood.network.GroupSession;
import com.safefood.service.ProfileService;
import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 그룹 조건 — 입력이 아니라 '확인'입니다 (리디자인 1g).
 *
 * <p>회원이면 프로필에서 알레르기·맵기·예산을 그대로 끌어와 채우므로, 바꿀 게 없으면
 * 버튼 한 번으로 준비가 끝납니다. 고칠 항목만 '수정'을 눌러 그 자리에서 펼칩니다.
 *
 * <p>게스트는 프로필이 없어 기본값으로 시작합니다 — 그래서 안내 문구가 달라집니다.
 */
public class GroupConditionController {

    private static final String NO_LOCATION = "아직 설정 안 함";

    @FXML private VBox root;
    @FXML private Label sourceLabel;

    @FXML private FlowPane allergySummary;
    @FXML private VBox allergyEditor;
    @FXML private FlowPane allergyPane;

    @FXML private Label spicySummary;
    @FXML private HBox spicyEditor;
    @FXML private Slider spicySlider;

    @FXML private Label budgetSummary;
    @FXML private VBox budgetEditor;
    @FXML private ComboBox<String> budgetBox;

    @FXML private Label locationSummary;

    private final List<ToggleButton> allergyChips = new ArrayList<>();

    @FXML
    private void initialize() {
        // 조건 확인 중 X로 닫으면 방에서 나갑니다 — 준비 안 된 유령 참여자로 남아 전원 READY를 막지 않게
        AppNav.onDialogClosed(root, () -> GroupSession.get().shutdown());

        Widgets.fillChips(allergyPane, DemoData.ALLERGIES, allergyChips);
        budgetBox.getItems().setAll(DemoData.BUDGETS);

        // 요약 줄은 편집기의 값이 바뀔 때마다 따라 갱신됩니다
        for (ToggleButton chip : allergyChips) {
            chip.selectedProperty().addListener((observable, before, on) -> renderAllergy());
        }
        spicySlider.valueProperty().addListener((observable, before, after) -> renderSpicy());
        budgetBox.valueProperty().addListener((observable, before, after) -> renderBudget());

        loadFromProfile();
        locationSummary.setText(NO_LOCATION);
        renderAllergy();
        renderSpicy();
        renderBudget();
    }

    /** 회원이면 프로필 값으로, 게스트면 기본값으로 채웁니다. */
    private void loadFromProfile() {
        UserDto me = Session.getCurrentUser();
        if (me == null) {
            sourceLabel.setText("게스트는 저장된 프로필이 없어 기본값으로 시작해요 — 바꿀 것만 눌러 고치세요.");
            budgetBox.setValue("10,000원 이하");
            spicySlider.setValue(2);
            return;
        }

        sourceLabel.setText("내 프로필에서 가져왔어요 — 바꿀 것만 눌러 고치세요.");
        ProfileService profileService = new ProfileService();

        Map<String, Integer> myAllergies = profileService.getMyAllergies(me.getId());
        if (myAllergies != null) {
            Widgets.preselect(allergyChips, new ArrayList<>(myAllergies.keySet()));
        }

        PreferenceDto pref = profileService.getMyPreference(me.getId());
        if (pref == null) {
            budgetBox.setValue("10,000원 이하");
            spicySlider.setValue(2);
            return;
        }
        spicySlider.setValue(pref.getSpicyLevel());
        budgetBox.setValue(nearestBudget(pref.getPriceMax()));
    }

    /** DB의 숫자 예산을 화면의 보기 중 가장 가까운 것으로 맞춥니다. */
    private static String nearestBudget(int priceMax) {
        if (priceMax <= 0) {
            return "제한없음";
        }
        String best = DemoData.BUDGETS.get(0);
        int bestGap = Integer.MAX_VALUE;
        for (String option : DemoData.BUDGETS) {
            String digits = option.replaceAll("\\D", "");
            if (digits.isEmpty()) {
                continue;   // "제한없음"
            }
            int gap = Math.abs(Integer.parseInt(digits) - priceMax);
            if (gap < bestGap) {
                bestGap = gap;
                best = option;
            }
        }
        return best;
    }

    // ── 요약 줄 그리기 ──────────────────────────────────────────

    private void renderAllergy() {
        allergySummary.getChildren().clear();
        List<String> picked = Widgets.selected(allergyChips);
        if (picked.isEmpty()) {
            allergySummary.getChildren().add(Widgets.sub("없음"));
            return;
        }
        for (String name : picked) {
            allergySummary.getChildren().add(Widgets.tag(name, "accent"));
        }
    }

    private void renderSpicy() {
        spicySummary.setText("맵기 " + (int) spicySlider.getValue() + "단계");
    }

    private void renderBudget() {
        budgetSummary.setText(budgetBox.getValue() == null ? "제한없음" : budgetBox.getValue());
    }

    // ── 펼치기 ──────────────────────────────────────────────────

    @FXML
    private void handleEditAllergy() {
        toggle(allergyEditor);
    }

    @FXML
    private void handleEditSpicy() {
        toggle(spicyEditor);
    }

    @FXML
    private void handleEditBudget() {
        toggle(budgetEditor);
    }

    private static void toggle(Node editor) {
        boolean open = !editor.isVisible();
        editor.setVisible(open);
        editor.setManaged(open);
    }

    @FXML
    private void handlePickLocation() {
        locationSummary.setText("중간 지점 계산용 좌표 자리 (G-08, 3차 구현)");
    }

    // ── 준비 완료 ───────────────────────────────────────────────

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
