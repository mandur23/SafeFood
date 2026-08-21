package com.safefood.view.controller;

import com.safefood.service.AuthService;
import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * 회원가입 — 중복 확인을 버튼에서 실시간 검증으로 옮겼습니다 (리디자인 1e).
 *
 * <p>글자마다 DB에 묻지 않도록 {@link PauseTransition} 으로 입력이 멎을 때까지 기다립니다
 * (디바운스). 타자를 다시 치면 대기가 처음부터 다시 시작되므로, 실제 조회는 사람이 손을
 * 멈춘 뒤 한 번만 일어납니다.
 */
public class SignUpController {

    /** 입력이 멎었다고 볼 시간 — 짧으면 조회가 잦아지고, 길면 결과가 늦게 뜹니다. */
    private static final Duration SETTLE = Duration.millis(400);

    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private TextField nicknameField;

    @FXML private Label idHint;
    @FXML private Label confirmHint;
    @FXML private Label nicknameHint;
    @FXML private Label errorLabel;

    @FXML private Region strength1;
    @FXML private Region strength2;
    @FXML private Region strength3;


    private final AuthService authService = new AuthService();

    private final PauseTransition idSettle = new PauseTransition(SETTLE);
    private final PauseTransition nicknameSettle = new PauseTransition(SETTLE);

    /** 마지막 조회 결과. 제출 때 이 값으로 판정합니다 (조회 전이면 null). */
    private Boolean idAvailable;
    private Boolean nicknameAvailable;

    @FXML
    private void initialize() {
        idSettle.setOnFinished(event -> checkId());
        nicknameSettle.setOnFinished(event -> checkNickname());

        idField.textProperty().addListener((observable, before, after) -> {
            idAvailable = null;                  // 고친 순간 이전 결과는 무효
            Widgets.markField(idField, null);
            hide(idHint);
            Widgets.hideError(errorLabel);
            idSettle.playFromStart();            // 다시 멎을 때까지 대기
        });

        nicknameField.textProperty().addListener((observable, before, after) -> {
            nicknameAvailable = null;
            Widgets.markField(nicknameField, null);
            hide(nicknameHint);
            nicknameSettle.playFromStart();
        });

        passwordField.textProperty().addListener((observable, before, after) -> {
            renderStrength(strengthOf(after));
            renderConfirm();
        });
        confirmField.textProperty().addListener((observable, before, after) -> renderConfirm());

        renderStrength(0);
    }

    // ── 실시간 검증 ─────────────────────────────────────────────

    private void checkId() {
        String loginId = idField.getText().trim();
        if (loginId.isBlank()) {
            return;                              // 빈 칸에 대고 "이미 쓰는 아이디"라고 하지 않습니다
        }
        idAvailable = authService.isLoginIdAvailable(loginId);
        Widgets.markField(idField, idAvailable);
        hint(idHint, idAvailable,
                "✓ 사용할 수 있는 아이디예요", "이미 사용 중인 아이디예요");
    }

    private void checkNickname() {
        String nickname = nicknameField.getText().trim();
        if (nickname.isBlank()) {
            return;
        }
        nicknameAvailable = authService.isNicknameAvailable(nickname);
        Widgets.markField(nicknameField, nicknameAvailable);
        hint(nicknameHint, nicknameAvailable,
                "✓ 사용 가능", "이미 누군가 쓰고 있어요");
    }

    private void renderConfirm() {
        String confirm = confirmField.getText();
        if (confirm.isEmpty()) {
            hide(confirmHint);
            Widgets.markField(confirmField, null);
            return;
        }
        boolean same = confirm.equals(passwordField.getText());
        Widgets.markField(confirmField, same);
        confirmHint.getStyleClass().removeAll("hint-ok", "hint-bad");
        confirmHint.getStyleClass().add(same ? "hint-ok" : "hint-bad");
        confirmHint.setText(same ? "✓ 일치합니다" : "비밀번호가 일치하지 않아요");
        show(confirmHint);
    }

    /** 0~3 — 길이, 문자 종류 섞임, 넉넉한 길이를 한 칸씩 쳐 줍니다. */
    private static int strengthOf(String password) {
        if (password == null || password.length() < 8) {
            return password == null || password.isEmpty() ? 0 : 1;
        }
        boolean letter = password.matches(".*[A-Za-z].*");
        boolean digit = password.matches(".*[0-9].*");
        boolean symbol = password.matches(".*[^A-Za-z0-9].*");
        if (letter && digit && (symbol || password.length() >= 12)) {
            return 3;
        }
        return letter && digit ? 2 : 1;
    }

    private void renderStrength(int level) {
        paint(strength1, level >= 1);
        paint(strength2, level >= 2);
        paint(strength3, level >= 3);
    }

    private static void paint(Region bar, boolean on) {
        bar.getStyleClass().remove("on");
        if (on) {
            bar.getStyleClass().add("on");
        }
    }

    // ── 제출 ────────────────────────────────────────────────────

    @FXML
    private void handleSubmit() {
        String loginId = idField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmField.getText();
        String nickname = nicknameField.getText().trim();

        if (loginId.isBlank() || password.isBlank() || nickname.isBlank()) {
            Widgets.showError(errorLabel, "빈 칸을 모두 채워 주세요.");
            return;
        }
        if (!password.equals(confirm)) {
            Widgets.showError(errorLabel, "비밀번호가 일치하지 않습니다.");
            return;
        }

        // 검증이 아직 안 돌았으면(빨리 눌렀거나 대기 중) 여기서 한 번에 확인합니다
        if (idAvailable == null) {
            idSettle.stop();
            checkId();
        }
        if (nicknameAvailable == null) {
            nicknameSettle.stop();
            checkNickname();
        }
        if (Boolean.FALSE.equals(idAvailable)) {
            Widgets.showError(errorLabel, "이미 사용 중인 아이디입니다.");
            return;
        }
        if (Boolean.FALSE.equals(nicknameAvailable)) {
            Widgets.showError(errorLabel, "이미 사용 중인 닉네임입니다.");
            return;
        }

        if (!authService.register(loginId, password, nickname)) {
            Widgets.showError(errorLabel, "회원가입 처리 중 문제가 발생했습니다.");
            return;
        }

        Widgets.hideError(errorLabel);
        OnboardingController.newUserId = loginId;
        AppNav.show("온보딩 설문", "onboarding.fxml");
    }

    @FXML
    private void handleCancel() {
        AppNav.show("로그인", "login.fxml");
    }

    // ── 문구 표시 ───────────────────────────────────────────────

    private static void hint(Label label, boolean ok, String okText, String badText) {
        label.getStyleClass().removeAll("hint-ok", "hint-bad");
        label.getStyleClass().add(ok ? "hint-ok" : "hint-bad");
        label.setText(ok ? okText : badText);
        show(label);
    }

    private static void show(Label label) {
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void hide(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }
}
