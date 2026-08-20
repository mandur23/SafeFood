package com.safefood.view;

import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.List;

public final class Widgets {

    private Widgets() {
    }

    public static ToggleButton chip(String text) {
        ToggleButton chip = new ToggleButton(text);
        chip.getStyleClass().add("chip");
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

    public static Label safetyBadge(com.safefood.dto.RecommendationDto.Safety safety) {
        Label badge = new Label(safety.label);
        badge.getStyleClass().addAll("safety", safety.styleClass);
        return badge;
    }

    public static Label statusLabel(String type) {
        String text = switch (type) {
            case "EATEN" -> "먹음";
            case "VIEWED" -> "조회함";
            case "BLOCKED" -> "차단됨";
            default -> "추천받음";
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

    public static Label sub(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sub");
        label.setWrapText(true);
        return label;
    }

    public static void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void hideError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }
}
