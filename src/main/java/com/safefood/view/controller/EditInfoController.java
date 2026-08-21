package com.safefood.view.controller;

import com.safefood.dto.Session;
import com.safefood.dto.UserDto;
import com.safefood.service.AuthService;
import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * 정보 변경 — 닉네임 중복 확인을 버튼에서 실시간 검증으로 옮겼습니다 (회원가입 1e 와 같은 방식).
 */
public class EditInfoController {

    private static final Duration SETTLE = Duration.millis(400);

    @FXML private VBox root;
    @FXML private PasswordField currentPassword;
    @FXML private TextField nicknameField;
    @FXML private Label nicknameHint;
    @FXML private PasswordField newPassword;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();
    private final PauseTransition nicknameSettle = new PauseTransition(SETTLE);

    /** 마지막 조회 결과 — 조회 전이면 null. 지금 쓰는 닉네임이면 true 로 봅니다. */
    private Boolean nicknameAvailable;

    @FXML
    private void initialize() {
        UserDto me = Session.getCurrentUser();
        if (me != null) {
            nicknameField.setText(me.getNickname());
            nicknameAvailable = true;   // 처음 값은 자기 것이라 그대로 둬도 됩니다
        }

        nicknameSettle.setOnFinished(event -> checkNickname());
        nicknameField.textProperty().addListener((observable, before, after) -> {
            nicknameAvailable = null;
            Widgets.markField(nicknameField, null);
            hide(nicknameHint);
            nicknameSettle.playFromStart();
        });
    }

    private void checkNickname() {
        String nickname = nicknameField.getText().trim();
        if (nickname.isBlank()) {
            return;
        }
        UserDto me = Session.getCurrentUser();

        // 내 원래 닉네임을 그대로 둔 경우는 중복이 아닙니다
        if (me != null && nickname.equals(me.getNickname())) {
            nicknameAvailable = true;
            hint(true, "지금 쓰고 있는 닉네임이에요");
            return;
        }

        nicknameAvailable = authService.isNicknameAvailable(nickname);
        hint(nicknameAvailable, nicknameAvailable
                ? "✓ 사용할 수 있어요" : "이미 누군가 쓰고 있어요");
    }

    @FXML
    private void handleSave() {
        String currentPw = currentPassword.getText();
        String newPw = newPassword.getText();
        String newNick = nicknameField.getText().trim();

        if (currentPw.isBlank()) {
            Widgets.showError(errorLabel, "현재 비밀번호를 입력해 주세요.");
            return;
        }
        if (newNick.isBlank()) {
            Widgets.showError(errorLabel, "닉네임을 입력해 주세요.");
            return;
        }

        UserDto me = Session.getCurrentUser();

        // 비밀번호 확인
        if (!authService.verifyPassword(currentPw, me.getPassword())) {
            Widgets.showError(errorLabel, "현재 비밀번호가 틀렸습니다.");
            return;
        }

        // 검증이 아직 안 돌았으면(빨리 눌렀거나 대기 중) 여기서 한 번에 확인합니다
        if (nicknameAvailable == null) {
            nicknameSettle.stop();
            checkNickname();
        }
        if (Boolean.FALSE.equals(nicknameAvailable)) {
            Widgets.showError(errorLabel, "이미 사용 중인 닉네임입니다. 다른 닉네임을 골라 주세요.");
            return;
        }

        if (!authService.updateProfile(me.getId(), newPw, newNick, me.getPassword())) {
            Widgets.showError(errorLabel, "저장 중 오류가 발생했습니다.");
            return;
        }

        Widgets.hideError(errorLabel);
        me.setNickname(newNick);
        if (!newPw.isBlank()) {
            me.setPassword(newPw);
        }
        AppNav.close(root);
        AppNav.success("개인정보를 변경했어요");
    }

    @FXML
    private void handleCancel() {
        AppNav.close(root);
    }

    private void hint(boolean ok, String text) {
        Widgets.markField(nicknameField, ok);
        nicknameHint.getStyleClass().removeAll("hint-ok", "hint-bad");
        nicknameHint.getStyleClass().add(ok ? "hint-ok" : "hint-bad");
        nicknameHint.setText(text);
        nicknameHint.setVisible(true);
        nicknameHint.setManaged(true);
    }

    private static void hide(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }
}
