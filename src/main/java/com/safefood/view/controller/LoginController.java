package com.safefood.view.controller;

import com.safefood.dto.UserDto;
import com.safefood.network.GroupSession;
import com.safefood.network.InviteCode;
import com.safefood.service.AuthService;
import com.safefood.view.AppNav;
import com.safefood.view.Widgets;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * 로그인 — 회원과 게스트, 두 길이 한 화면에 나란히 있습니다 (리디자인 1c).
 */
public class LoginController {

    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private Label memberError;

    @FXML private TextField nameField;
    @FXML private TextField guestCodeField;
    @FXML private Label guestError;

    private final AuthService authService = new AuthService();

    // ══ 회원 ════════════════════════════════════════════════════

    @FXML
    private void handleLogin() {
        String loginId = idField.getText().trim();
        String password = passwordField.getText();

        if (loginId.isBlank() || password.isBlank()) {
            Widgets.showError(memberError, "아이디와 비밀번호를 입력해 주세요.");
            return;
        }

        UserDto user = authService.login(loginId, password);
        if (user == null) {
            Widgets.showError(memberError, "아이디 또는 비밀번호가 올바르지 않습니다.");
            return;
        }

        // Session에 정보 보관
        com.safefood.dto.Session.setCurrentUser(user);

        Widgets.hideError(memberError);
        // 그룹 참여(소켓 JOIN) 때 쓸 표시 이름
        String displayName = (user.getNickname() == null || user.getNickname().isBlank())
                ? loginId : user.getNickname();
        GroupSession.get().setDisplayName(displayName);
        GroupSession.get().setGuest(false);
        AppNav.show("SafeFood — 맞춤 맛집 추천", "main.fxml");
    }

    @FXML
    private void handleSignUp() {
        AppNav.show("회원가입", "signup.fxml");
    }

    // ══ 게스트 ══════════════════════════════════════════════════

    @FXML
    private void handleGuest() {
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            Widgets.showError(guestError, "이름을 입력해 주세요.");
            return;
        }

        // 코드는 선택이지만, 적었다면 형식이 맞는지 여기서 걸러 줍니다
        String code = "";
        if (guestCodeField != null && guestCodeField.getText() != null) {
            code = InviteCode.normalize(guestCodeField.getText());
            if (!code.isEmpty()) {
                try {
                    InviteCode.parse(code);
                } catch (IllegalArgumentException e) {
                    Widgets.showError(guestError, e.getMessage());
                    return;
                }
            }
        }

        Widgets.hideError(guestError);
        UserDto guest = new UserDto(null, null, name);
        guest.setId(-1);
        com.safefood.dto.Session.setCurrentUser(guest);
        GroupSession.get().setDisplayName(name);
        GroupSession.get().setGuest(true);

        final String finalCode = code;
        AppNav.dialog("같이 먹기", "group-option.fxml",
                (GroupOptionController controller) -> {
                    if (!finalCode.isEmpty()) {
                        controller.prefillCode(finalCode);
                    }
                });
    }
}
