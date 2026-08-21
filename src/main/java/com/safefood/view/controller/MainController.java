package com.safefood.view.controller;

import com.safefood.network.GroupSession;
import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * 메인 추천 화면 (리디자인 1a).
 *
 * <p>카드에서 액션을 둘로 줄인 것이 이 화면의 핵심입니다 — '이걸로 먹을래요'와 '가게·지도 보기'만
 * 버튼으로 두고, 찜은 아이콘으로, 기록은 '먹을래요'에 흡수했습니다.
 */
public class MainController {

    @FXML private ComboBox<String> categoryBox;
    @FXML private ComboBox<String> distanceBox;
    @FXML private ComboBox<String> budgetBox;
    @FXML private FlowPane moodPane;
    @FXML private VBox cardBox;
    @FXML private VBox blockedBox;
    @FXML private Label userLabel;
    @FXML private Label greetingLabel;
    @FXML private Label excludeLabel;
    @FXML private Hyperlink excludeToggle;

    private final List<ToggleButton> moodChips = new ArrayList<>();

    /** 어제 먹은 메뉴를 빼고 볼지 — 예전 체크박스를 문장 + 링크로 바꾼 자리입니다. */
    private boolean excludeEaten = true;

    @FXML
    private void initialize() {
        // 회원은 로그인 세션의 닉네임을, 게스트는 그룹 참여용 표시 이름을 씁니다.
        String name;
        if (com.safefood.dto.Session.getCurrentUser() != null) {
            name = com.safefood.dto.Session.getCurrentUser().getNickname();
        } else {
            name = GroupSession.get().displayName();
        }
        userLabel.setText(name + "님");
        greetingLabel.setText("오늘 뭐 먹지, " + name + "님?");

        categoryBox.getItems().add("전체");
        categoryBox.getItems().addAll(DemoData.CATEGORIES);
        categoryBox.setValue("한식");

        distanceBox.getItems().setAll(DemoData.DISTANCES);
        distanceBox.setValue("1km");

        budgetBox.getItems().setAll(DemoData.BUDGETS);
        budgetBox.setValue("10,000원 이하");

        fillMoodChips();
        renderExcludeNotice();
        fillCards();
    }

    private void fillMoodChips() {
        moodPane.getChildren().clear();
        moodChips.clear();
        for (String mood : List.of("든든한 한끼", "속풀이·해장", "가벼운 식사", "혼밥")) {
            ToggleButton chip = Widgets.compactChip(mood);
            moodChips.add(chip);
            moodPane.getChildren().add(chip);
        }
        moodChips.get(0).setSelected(true);
    }

    private void renderExcludeNotice() {
        excludeLabel.setText(excludeEaten
                ? "어제 먹은 제육볶음은 자동으로 제외했어요 ·"
                : "어제 먹은 제육볶음도 후보에 넣었어요 ·");
        excludeToggle.setText(excludeEaten ? "포함하기" : "다시 제외하기");
    }

    @FXML
    private void handleToggleEaten() {
        excludeEaten = !excludeEaten;
        renderExcludeNotice();
        fillCards();
    }

    // ── 카드 ────────────────────────────────────────────────────

    private void fillCards() {
        cardBox.getChildren().clear();
        blockedBox.getChildren().clear();
        for (DemoData.Recommendation item : DemoData.RECOMMENDATIONS) {
            if (item.blocked()) {
                blockedBox.getChildren().add(blockedRow(item));   // 카드가 아니라 한 줄로 접습니다
            } else {
                cardBox.getChildren().add(card(item));
            }
        }
    }

    private HBox card(DemoData.Recommendation item) {
        // 왼쪽 — 원형 순위 + 점수
        Label score = Widgets.micro(item.score() + "점");
        VBox rank = new VBox(4, Widgets.rankCircle(item.rank()), score);
        rank.setAlignment(Pos.CENTER);
        rank.setMinWidth(56);
        rank.setPrefWidth(56);

        // 제목 줄 — 메뉴 · 가게·거리·평점 · 안전 · 맵기
        Label menu = new Label(item.menu());
        menu.getStyleClass().add("menu-name");

        Label meta = new Label(item.restaurant() + " · " + item.distance()
                + " · ★ " + item.rating());
        meta.getStyleClass().add("restaurant-name");

        HBox titleRow = new HBox(10, menu, meta, Widgets.hSpacer(),
                Widgets.safetyBadge(item.safety()),
                Widgets.tag("맵기 " + item.spicyLevel(), "neutral"));
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label reason = new Label(item.reason());
        reason.setWrapText(true);

        // 액션 — 버튼 2개 + 찜 아이콘
        Button eat = new Button("이걸로 먹을래요");
        eat.getStyleClass().add("primary");
        eat.setOnAction(event ->
                AppNav.success(item.menu() + "(으)로 정했어요 — 기록에 남기고 다음 추천에서 제외합니다"));

        Button map = new Button("가게·지도 보기");
        map.getStyleClass().add("ghost");
        map.setOnAction(event -> AppNav.dialog(
                item.menu() + " — 맛집 상세", "restaurant-detail.fxml",
                (RestaurantDetailController controller) -> controller.setItem(item)));

        ToggleButton favorite = new ToggleButton("♡");
        favorite.getStyleClass().addAll("icon");
        favorite.selectedProperty().addListener((observable, before, on) ->
                favorite.setText(on ? "♥" : "♡"));

        HBox actions = new HBox(8, eat, map, Widgets.hSpacer(), favorite);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(7, titleRow, reason, actions);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox card = new HBox(20, rank, body);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("card", "soft", "elev");
        return card;
    }

    /** 차단된 메뉴 — 왜 빠졌는지와 대체 메뉴만 한 줄로 보여 줍니다. */
    private HBox blockedRow(DemoData.Recommendation item) {
        Label struck = new Label(item.menu() + " · " + item.restaurant());
        struck.getStyleClass().add("strike");

        Hyperlink why = new Hyperlink("왜 빠졌나요?");
        why.setOnAction(event -> AppNav.info(item.reason()));

        HBox row = new HBox(12,
                Widgets.tag(item.safety().shortLabel, "danger"),
                struck,
                Widgets.sub(item.alternative() == null
                        ? "" : "대체 — " + item.alternative()),
                Widgets.hSpacer(),
                why);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("blocked-row");
        return row;
    }

    // ── 내비게이션 ──────────────────────────────────────────────

    @FXML
    private void handleProfile() {
        AppNav.show("프로필 및 설정 관리", "profile.fxml");
    }

    @FXML
    private void handleHistory() {
        AppNav.show("식사 기록 및 즐겨찾기", "history.fxml");
    }

    @FXML
    private void handleLogout() {
        AppNav.show("로그인", "login.fxml");
    }

    @FXML
    private void handleReroll() {
        fillCards();
    }

    @FXML
    private void handleRoulette() {
        AppNav.info("후보 중 무작위로 하나를 고르는 기능입니다. (R-08, 3차 구현)");
    }

    @FXML
    private void handleGroup() {
        AppNav.dialog("같이 먹기", "group-option.fxml");
    }
}
