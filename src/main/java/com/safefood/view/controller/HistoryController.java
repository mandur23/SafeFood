package com.safefood.view.controller;

import com.safefood.dto.FavoriteDto;
import com.safefood.dto.HistoryDto;
import com.safefood.dto.UserDto;
import com.safefood.network.GroupSession;
import com.safefood.service.FavoriteService;
import com.safefood.service.FeedbackService;
import com.safefood.service.HistoryService;
import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 나의 기록 — 날짜로 묶은 타임라인 (리디자인 1i).
 */
public class HistoryController {

    private record Filter(String label, String type) {
    }

    private static final List<Filter> FILTERS = List.of(
            new Filter("전체", null),
            new Filter("먹음", "EATEN"),
            new Filter("추천받음", "RECOMMENDED"),
            new Filter("조회", "VIEWED"),
            new Filter("차단됨", "BLOCKED")
    );

    @FXML private HBox tabSeg;
    @FXML private HBox filterRow;
    @FXML private VBox timelineBox;
    @FXML private Label userLabel;

    private final ToggleGroup tabGroup = new ToggleGroup();
    private final ToggleGroup filterGroup = new ToggleGroup();

    private boolean showingHistory = true;

    @FXML
    private void initialize() {
        if (com.safefood.dto.Session.getCurrentUser() != null) {
            String nick = com.safefood.dto.Session.getCurrentUser().getNickname();
            if (nick == null || nick.isBlank()) {
                nick = com.safefood.dto.Session.getCurrentUser().getLoginId();
            }
            userLabel.setText((nick != null ? nick : "사용자") + "님");
        } else {
            userLabel.setText(GroupSession.get().displayName() + "님");
        }

        buildTabs();
        buildFilters();
        render();
    }

    private void buildTabs() {
        ToggleButton history = Widgets.segOption("히스토리", true, false);
        ToggleButton favorite = Widgets.segOption("즐겨찾기", false, true);
        history.setToggleGroup(tabGroup);
        favorite.setToggleGroup(tabGroup);
        history.setSelected(true);
        tabSeg.getChildren().setAll(history, favorite);

        tabGroup.selectedToggleProperty().addListener((observable, before, after) -> {
            if (after == null) {
                tabGroup.selectToggle(before);
                return;
            }
            showingHistory = (after == history);
            filterRow.setVisible(showingHistory);
            filterRow.setManaged(showingHistory);
            render();
        });
    }

    private void buildFilters() {
        filterRow.getChildren().clear();
        for (Filter filter : FILTERS) {
            ToggleButton chip = Widgets.compactChip(filter.label());
            chip.setToggleGroup(filterGroup);
            chip.setUserData(filter);
            if (filter.type() == null) {
                chip.setSelected(true);
            }
            filterRow.getChildren().add(chip);
        }
        filterGroup.selectedToggleProperty().addListener((observable, before, after) -> {
            if (after == null) {
                filterGroup.selectToggle(before);
                return;
            }
            render();
        });
    }

    // ── 그리기 ──────────────────────────────────────────────────

    private void render() {
        timelineBox.getChildren().clear();
        if (showingHistory) {
            renderHistory();
        } else {
            renderFavorites();
        }
    }

    private void renderHistory() {
        String wanted = selectedType();
        UserDto me = com.safefood.dto.Session.getCurrentUser();

        List<HistoryDto> allHistories = (me != null && me.getId() != -1)
                ? new HistoryService().getHistories(me.getId())
                : new ArrayList<>();

        String lastDate = null;
        int shown = 0;

        for (HistoryDto row : allHistories) {
            if (wanted != null && !wanted.equals(row.getType())) {
                continue;
            }
            if (row.getDate() != null && !row.getDate().equals(lastDate)) {
                lastDate = row.getDate();
                Label header = Widgets.label(formatDate(row.getDate()), "timeline-date");
                VBox.setMargin(header, new javafx.geometry.Insets(shown == 0 ? 4 : 12, 0, 2, 6));
                timelineBox.getChildren().add(header);
            }
            timelineBox.getChildren().add(historyRow(row, me));
            shown++;
        }

        if (shown == 0) {
            timelineBox.getChildren().add(Widgets.sub("해당하는 기록이 없습니다."));
        }
    }

    private HBox historyRow(HistoryDto row, UserDto me) {
        Label menu = new Label(row.getMenu());
        menu.getStyleClass().add("BLOCKED".equals(row.getType()) ? "strike" : "section-title");

        HBox line = new HBox(12,
                Widgets.statusLabel(row.getType()),
                menu,
                Widgets.sub(row.getRestaurant()),
                Widgets.hSpacer(),
                Widgets.sub(row.getNote() != null ? row.getNote() : ""));
        line.setAlignment(Pos.CENTER_LEFT);
        line.getStyleClass().addAll("card", "soft");
        line.setPadding(new javafx.geometry.Insets(13, 18, 13, 18));

        if ("EATEN".equals(row.getType())) {
            boolean rated = row.getFeedbackId() > 0;
            Button rate = new Button(rated ? "평가완료" : "평가하기");
            rate.getStyleClass().add("ghost-quiet");
            rate.setDisable(rated);
            rate.setOnAction(event -> {
                if (me == null || me.getId() == -1) {
                    AppNav.warn("게스트는 평가할 수 없습니다.");
                    return;
                }
                List<Integer> choices = List.of(5, 4, 3, 2, 1);
                javafx.scene.control.ChoiceDialog<Integer> dialog =
                        new javafx.scene.control.ChoiceDialog<>(5, choices);
                dialog.setTitle("식사 평가");
                dialog.setHeaderText(row.getMenu() + "은(는) 어떠셨나요?");
                dialog.setContentText("별점을 선택해주세요 (1~5점):");
                dialog.showAndWait().ifPresent(rating -> {
                    boolean ok = new FeedbackService()
                            .saveFeedback(me.getId(), row.getHistoryId(), row.getMenuId(), rating);
                    if (ok) {
                        AppNav.info("평가가 저장되었습니다!\n(앞으로 추천 알고리즘 가중치에 반영됩니다)");
                        rate.setDisable(true);
                        rate.setText("평가완료");
                    } else {
                        AppNav.error("평가 저장에 실패했습니다.");
                    }
                });
            });
            line.getChildren().add(rate);
        }
        return line;
    }

    private void renderFavorites() {
        UserDto me = com.safefood.dto.Session.getCurrentUser();
        List<FavoriteDto> favs = (me != null && me.getId() != -1)
                ? new FavoriteService().getFavorites(me.getId())
                : new ArrayList<>();

        if (favs.isEmpty()) {
            timelineBox.getChildren().add(Widgets.sub("찜한 가게가 없습니다."));
            return;
        }
        for (FavoriteDto row : favs) {
            timelineBox.getChildren().add(favoriteRow(row));
        }
    }

    private HBox favoriteRow(FavoriteDto row) {
        Button remove = new Button("♥ 찜 취소");
        remove.getStyleClass().add("ghost-quiet");

        HBox line = new HBox(12,
                Widgets.tag("찜", "accent"),
                Widgets.label(row.getRestaurant(), "section-title"),
                Widgets.sub(row.getMenu()),
                Widgets.hSpacer(),
                Widgets.sub("★ " + row.getRating() + " · 리뷰 " + row.getReviewCount()),
                remove);
        line.setAlignment(Pos.CENTER_LEFT);
        line.getStyleClass().addAll("card", "soft");
        line.setPadding(new javafx.geometry.Insets(13, 18, 13, 18));

        remove.setOnAction(event -> {
            new FavoriteService().removeFavorite(row.getFavoriteId());
            timelineBox.getChildren().remove(line);
        });
        return line;
    }

    private String selectedType() {
        ToggleButton picked = (ToggleButton) filterGroup.getSelectedToggle();
        return picked == null ? null : ((Filter) picked.getUserData()).type();
    }

    /** "2026-08-12" → "8월 12일 화요일". 형식이 다르면 원문을 그대로 씁니다. */
    private static String formatDate(String iso) {
        try {
            LocalDate date = LocalDate.parse(iso);
            return date.getMonthValue() + "월 " + date.getDayOfMonth() + "일 "
                    + date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        } catch (DateTimeParseException e) {
            return iso;
        }
    }

    // ── 내비게이션 ──────────────────────────────────────────────

    @FXML
    private void handleMain() {
        AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
    }

    @FXML
    private void handleProfile() {
        AppNav.show("프로필 및 설정 관리", "profile.fxml");
    }

    @FXML
    private void handleGroup() {
        AppNav.dialog("같이 먹기 옵션 선택", "group-option.fxml");
    }

    @FXML
    private void handleLogout() {
        com.safefood.dto.Session.setCurrentUser(null);
        GroupSession.get().setGuest(false);
        AppNav.show("SafeFood 로그인", "login.fxml");
    }
}
