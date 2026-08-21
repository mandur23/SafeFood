package com.safefood.view;

import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Organic 디자인 시스템의 작은 조각들을 코드로 만드는 자리.
 *
 * <p>스타일은 전부 {@code app.css} 의 클래스를 붙이는 것으로만 합니다 — 여기에 색·크기를
 * 인라인으로 박으면 토큰을 바꿔도 화면이 따라오지 않습니다.
 */
public final class Widgets {

    private Widgets() {
    }

    // ── 칩 (선택 가능) ───────────────────────────────────────────

    public static ToggleButton chip(String text) {
        ToggleButton chip = new ToggleButton(text);
        chip.getStyleClass().add("chip");
        return chip;
    }

    /** 필터 줄에 쓰는 작은 칩. */
    public static ToggleButton compactChip(String text) {
        ToggleButton chip = chip(text);
        chip.getStyleClass().add("compact");
        return chip;
    }

    public static void fillChips(FlowPane target, List<String> names, List<ToggleButton> out) {
        target.getChildren().clear();
        out.clear();
        for (String name : names) {
            ToggleButton chip = chip(name);
            out.add(chip);
            target.getChildren().add(chip);
        }
    }

    public static List<String> selected(List<ToggleButton> chips) {
        List<String> picked = new ArrayList<>();
        for (ToggleButton chip : chips) {
            if (chip.isSelected()) {
                picked.add(chip.getText());
            }
        }
        return picked;
    }

    public static void preselect(List<ToggleButton> chips, List<String> names) {
        for (ToggleButton chip : chips) {
            if (names.contains(chip.getText())) {
                chip.setSelected(true);
            }
        }
    }

    /** 세그먼티드 컨트롤 한 칸 — 첫/끝 칸만 pill 로 둥글게 깎입니다. */
    public static ToggleButton segOption(String text, boolean first, boolean last) {
        ToggleButton option = new ToggleButton(text);
        option.getStyleClass().add("seg-opt");
        if (first) {
            option.getStyleClass().add("first");
        }
        if (last) {
            option.getStyleClass().add("last");
        }
        return option;
    }

    // ── 라벨 ────────────────────────────────────────────────────

    /** 스타일 클래스를 붙인 라벨. 대부분의 텍스트는 이걸로 만듭니다. */
    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    /** 읽기 전용 태그. {@code variants} 는 accent / accent2 / neutral / solid / warn / danger / outline. */
    public static Label tag(String text, String... variants) {
        Label tag = new Label(text);
        tag.getStyleClass().add("tag");
        tag.getStyleClass().addAll(variants);
        return tag;
    }

    public static Label sub(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sub");
        label.setWrapText(true);
        return label;
    }

    public static Label micro(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("micro");
        label.setWrapText(true);
        return label;
    }

    /** 원형 뱃지 — 브랜드 마크, 순위 번호, 아바타에 공통으로 씁니다. */
    public static Label circle(String text, String baseClass, String... variants) {
        Label label = new Label(text);
        label.getStyleClass().add(baseClass);
        label.getStyleClass().addAll(variants);
        return label;
    }

    /** 이름의 첫 글자를 딴 아바타. */
    public static Label avatar(String name, String... variants) {
        String initial = name == null || name.isBlank() ? "?" : name.substring(0, 1);
        return circle(initial, "avatar", variants);
    }

    // ── 여백과 선 ───────────────────────────────────────────────

    /** HBox 안에서 남는 가로 공간을 전부 먹는 여백. */
    public static Region hSpacer() {
        Region spacer = new Region();
        javafx.scene.layout.HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /** VBox 안에서 남는 세로 공간을 전부 먹는 여백. */
    public static Region vSpacer() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static Region rule() {
        Region rule = new Region();
        rule.getStyleClass().add("rule");
        return rule;
    }

    public static Region ruleV(double height) {
        Region rule = new Region();
        rule.getStyleClass().add("rule-v");
        rule.setPrefHeight(height);
        rule.setMinHeight(height);
        return rule;
    }

    // ── 상태 표시 ───────────────────────────────────────────────

    public static Label rankBadge(int rank, int score) {
        String text = rank == 0 ? "차단됨 (0점)" : "추천 " + rank + "순위 (" + score + "점)";
        String style = switch (rank) {
            case 1 -> "rank-1";
            case 2 -> "rank-2";
            default -> "blocked";
        };
        Label badge = new Label(text);
        badge.getStyleClass().addAll("badge", style);
        return badge;
    }

    /** 추천 카드 왼쪽의 큰 원형 순위 번호. */
    public static Label rankCircle(int rank) {
        String variant = switch (rank) {
            case 1 -> "rank-1";
            case 2 -> "rank-2";
            default -> "blocked";
        };
        return circle(rank == 0 ? "✕" : String.valueOf(rank), "rank-circle", variant);
    }

    public static Label safetyBadge(DemoData.Safety safety) {
        Label badge = new Label(safety.shortLabel);
        badge.getStyleClass().addAll("safety", safety.styleClass);
        return badge;
    }

    public static Label statusLabel(String type) {
        String text = switch (type) {
            case "EATEN" -> "먹음";
            case "VIEWED" -> "조회";
            case "BLOCKED" -> "차단";
            default -> "추천";
        };
        Label label = new Label(text);
        label.getStyleClass().addAll("status", type.toLowerCase());
        return label;
    }

    public static Label memberState(String state) {
        Label label = new Label(state);
        label.getStyleClass().addAll("member-state", state.toLowerCase());
        return label;
    }

    /** 채팅 흐름 가운데에 놓이는 시스템 알림. {@code variants} 는 good / bad. */
    public static Label systemPill(String text, String... variants) {
        Label pill = new Label(text);
        pill.getStyleClass().add("system-pill");
        pill.getStyleClass().addAll(variants);
        return pill;
    }

    // ── 오류 문구 ───────────────────────────────────────────────

    public static void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void hideError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    /** 입력칸 테두리를 정상/성공/오류 중 하나로 맞춥니다 (셋은 서로 배타적입니다). */
    public static void markField(javafx.scene.control.Control field, Boolean ok) {
        field.getStyleClass().removeAll("ok", "error");
        if (ok == null) {
            return;
        }
        field.getStyleClass().add(ok ? "ok" : "error");
    }
}
