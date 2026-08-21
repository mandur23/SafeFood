package com.safefood.view.controller;

import com.safefood.view.AppNav;
import com.safefood.view.DemoData;
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
import java.util.List;
import java.util.Locale;

/**
 * 나의 기록 — 날짜로 묶은 타임라인 (리디자인 1i).
 *
 * <p>줄마다 날짜를 반복해 적던 것을 날짜 머리글 하나로 묶고, 상태 콤보 필터를 칩으로
 * 바꿨습니다. 즐겨찾기는 날짜가 없으므로 머리글 없이 같은 모양의 줄로만 그립니다.
 */
public class HistoryController {

    /** 필터 칩 — 라벨과 DemoData 의 type 값 짝. type 이 null 이면 전체. */
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
            userLabel.setText(com.safefood.dto.Session.getCurrentUser().getNickname() + "님");
        } else {
            userLabel.setText(com.safefood.network.GroupSession.get().displayName() + "님");
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
                tabGroup.selectToggle(before);   // 한 쪽은 늘 켜져 있어야 합니다
                return;
            }
            showingHistory = after == history;
            filterRow.setVisible(showingHistory);
            filterRow.setManaged(showingHistory);   // 즐겨찾기에는 상태 필터가 없습니다
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
        String lastDate = null;
        int shown = 0;

        for (DemoData.HistoryRow row : DemoData.HISTORY) {
            if (wanted != null && !wanted.equals(row.type())) {
                continue;
            }
            // 날짜가 바뀌는 지점에서만 머리글을 답니다 — 같은 날은 한 번만
            if (!row.date().equals(lastDate)) {
                lastDate = row.date();
                Label header = Widgets.label(formatDate(row.date()), "timeline-date");
                VBox.setMargin(header, new javafx.geometry.Insets(shown == 0 ? 4 : 12, 0, 2, 6));
                timelineBox.getChildren().add(header);
            }
            timelineBox.getChildren().add(historyRow(row));
            shown++;
        }

        if (shown == 0) {
            timelineBox.getChildren().add(Widgets.sub("해당하는 기록이 없습니다."));
        }
    }

    private HBox historyRow(DemoData.HistoryRow row) {
        Label menu = new Label(row.menu());
        menu.getStyleClass().add("BLOCKED".equals(row.type()) ? "strike" : "section-title");

        HBox line = new HBox(12,
                Widgets.statusLabel(row.type()),
                menu,
                Widgets.sub(row.restaurant()),
                Widgets.hSpacer(),
                Widgets.sub(row.note()));
        line.setAlignment(Pos.CENTER_LEFT);
        line.getStyleClass().addAll("card", "soft");
        line.setPadding(new javafx.geometry.Insets(13, 18, 13, 18));

        if ("EATEN".equals(row.type())) {
            Button rate = new Button("평가하기");
            rate.getStyleClass().add("ghost-quiet");
            rate.setOnAction(event ->
                    AppNav.info("좋아요 / 만족도 1~5점을 입력받는 자리입니다. (H-04)"));
            line.getChildren().add(rate);
        }
        return line;
    }

    private void renderFavorites() {
        if (DemoData.FAVORITES.isEmpty()) {
            timelineBox.getChildren().add(Widgets.sub("찜한 가게가 없습니다."));
            return;
        }
        for (DemoData.FavoriteRow row : DemoData.FAVORITES) {
            timelineBox.getChildren().add(favoriteRow(row));
        }
    }

    private HBox favoriteRow(DemoData.FavoriteRow row) {
        Button remove = new Button("♥ 찜 취소");
        remove.getStyleClass().add("ghost-quiet");

        HBox line = new HBox(12,
                Widgets.tag("찜", "accent"),
                Widgets.label(row.restaurant(), "section-title"),
                Widgets.sub(row.menu()),
                Widgets.hSpacer(),
                Widgets.sub("★ " + row.rating() + " · 리뷰 " + row.reviewCount()),
                remove);
        line.setAlignment(Pos.CENTER_LEFT);
        line.getStyleClass().addAll("card", "soft");
        line.setPadding(new javafx.geometry.Insets(13, 18, 13, 18));

        remove.setOnAction(event -> timelineBox.getChildren().remove(line));
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
        AppNav.dialog("같이 먹기", "group-option.fxml");
    }

    @FXML
    private void handleLogout() {
        AppNav.show("로그인", "login.fxml");
    }
}
