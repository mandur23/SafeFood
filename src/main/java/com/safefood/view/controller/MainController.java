package com.safefood.view.controller;

import com.safefood.dto.FavoriteDto;
import com.safefood.dto.RecommendationDto;
import com.safefood.dto.UserDto;
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
    private boolean excludeEaten = true;

    @FXML
    private void initialize() {
        String name;
        if (com.safefood.dto.Session.getCurrentUser() != null) {
            name = com.safefood.dto.Session.getCurrentUser().getNickname();
            if (name == null || name.isBlank()) {
                name = com.safefood.dto.Session.getCurrentUser().getLoginId();
            }
        } else {
            name = GroupSession.get().displayName();
        }
        if (name == null || name.isBlank()) {
            name = "사용자";
        }
        userLabel.setText(name + "님");
        greetingLabel.setText("오늘 뭐 먹지, " + name + "님?");

        categoryBox.getItems().add("전체");
        categoryBox.getItems().addAll(DemoData.CATEGORIES);
        categoryBox.setValue("전체");

        distanceBox.getItems().setAll("500m", "1km", "2km", "3km", "제한없음");
        distanceBox.setValue("1km");

        budgetBox.getItems().setAll(DemoData.BUDGETS);
        budgetBox.setValue("12,000원 이하");

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
        if (!moodChips.isEmpty()) {
            moodChips.get(0).setSelected(true);
        }
    }

    private void renderExcludeNotice() {
        excludeLabel.setText(excludeEaten
                ? "어제 먹은 메뉴는 자동으로 제외했어요 ·"
                : "어제 먹은 메뉴도 후보에 넣었어요 ·");
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
        if (blockedBox != null) {
            blockedBox.getChildren().clear();
        }

        List<RecommendationDto> realItems = new com.safefood.dao.RecommendationDao().getRecommendationsFromDb();

        UserDto me = com.safefood.dto.Session.getCurrentUser();
        List<FavoriteDto> myFavs = (me != null && me.getId() != -1)
                ? new com.safefood.service.FavoriteService().getFavorites(me.getId())
                : new ArrayList<>();

        for (RecommendationDto item : realItems) {
            boolean isAlreadyFavorited = false;
            for (FavoriteDto fav : myFavs) {
                if (fav.getMenu() != null && fav.getMenu().equals(item.getMenu())
                        && fav.getRestaurant() != null && fav.getRestaurant().equals(item.getRestaurant())) {
                    isAlreadyFavorited = true;
                    break;
                }
            }

            if (item.isBlocked()) {
                if (blockedBox != null) {
                    blockedBox.getChildren().add(blockedRow(item));
                } else {
                    cardBox.getChildren().add(card(item, isAlreadyFavorited));
                }
            } else {
                cardBox.getChildren().add(card(item, isAlreadyFavorited));
            }
        }
    }

    private HBox card(RecommendationDto item, boolean isAlreadyFavorited) {
        // 왼쪽 — 원형 순위 + 점수
        Label score = Widgets.micro(item.getScore() + "점");
        VBox rank = new VBox(4, Widgets.rankCircle(item.getRank()), score);
        rank.setAlignment(Pos.CENTER);
        rank.setMinWidth(56);
        rank.setPrefWidth(56);

        // 제목 줄 — 메뉴 · 가게·평점 · 안전 · 맵기
        Label menu = new Label(item.getMenu());
        menu.getStyleClass().add("menu-name");

        Label meta = new Label(item.getRestaurant() + " · ★ " + item.getRating());
        meta.getStyleClass().add("restaurant-name");

        HBox titleRow = new HBox(10, menu, meta, Widgets.hSpacer(),
                Widgets.safetyBadge(item.getSafety()),
                Widgets.tag("맵기 " + item.getSpicyLevel() + "단계", "neutral"));
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label reason = new Label(item.getReason());
        reason.setWrapText(true);

        // 액션 — 버튼 2개 + 찜 아이콘
        Button eat = new Button("이걸로 먹을래요");
        eat.getStyleClass().add("primary");
        eat.setOnAction(event -> {
            UserDto u = com.safefood.dto.Session.getCurrentUser();
            if (u == null || u.getId() == -1) {
                AppNav.warn("게스트는 기록을 저장할 수 없습니다.");
                return;
            }
            boolean success = new com.safefood.service.HistoryService().saveEatenHistory(u.getId(), item.getMenuId());
            if (success) {
                AppNav.success(item.getMenu() + "(으)로 정했어요 — 기록에 남기고 다음 추천에서 제외합니다");
            } else {
                AppNav.error("저장에 실패했습니다.");
            }
        });
        eat.setDisable(item.isBlocked());

        Button map = new Button("가게·지도 보기");
        map.getStyleClass().add("ghost");
        map.setOnAction(event -> AppNav.dialog(
                item.getMenu() + " — 맛집 상세", "restaurant-detail.fxml",
                (RestaurantDetailController controller) -> controller.setItem(item)));

        ToggleButton favorite = new ToggleButton(isAlreadyFavorited ? "♥" : "♡");
        favorite.getStyleClass().addAll("icon");
        favorite.setSelected(isAlreadyFavorited);
        favorite.selectedProperty().addListener((observable, before, on) -> {
            favorite.setText(on ? "♥" : "♡");
            UserDto u = com.safefood.dto.Session.getCurrentUser();
            if (u == null || u.getId() == -1) {
                AppNav.warn("게스트는 찜하기를 사용할 수 없습니다.");
                return;
            }
            if (on) {
                new com.safefood.service.FavoriteService().addFavorite(u.getId(), item.getRestaurantId(), item.getMenuId());
            } else {
                new com.safefood.service.FavoriteService().removeFavoriteByMenu(u.getId(), item.getRestaurantId(), item.getMenuId());
            }
        });

        HBox actions = new HBox(8, eat, map, Widgets.hSpacer(), favorite);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(7, titleRow, reason, actions);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox card = new HBox(20, rank, body);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("card", "soft", "elev");

        if (item.isBlocked()) {
            card.getStyleClass().add("blocked");
            if (item.getAlternative() != null) {
                body.getChildren().add(Widgets.sub("대체 메뉴 — " + item.getAlternative()));
            }
        }
        return card;
    }

    /** 차단된 메뉴 — 왜 빠졌는지와 대체 메뉴만 한 줄로 보여 줍니다. */
    private HBox blockedRow(RecommendationDto item) {
        Label struck = new Label(item.getMenu() + " · " + item.getRestaurant());
        struck.getStyleClass().add("strike");

        Hyperlink why = new Hyperlink("왜 빠졌나요?");
        why.setOnAction(event -> AppNav.info(item.getReason()));

        HBox row = new HBox(12,
                Widgets.tag(item.getSafety().label, "danger"),
                struck,
                Widgets.sub(item.getAlternative() == null ? "" : "대체 — " + item.getAlternative()),
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
        com.safefood.dto.Session.setCurrentUser(null);
        GroupSession.get().setGuest(false);
        AppNav.show("SafeFood 로그인", "login.fxml");
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
        AppNav.dialog("같이 먹기 옵션 선택", "group-option.fxml");
    }
}
