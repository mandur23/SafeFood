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
 *
 * <p>탭을 없앤 대신 제출 버튼이 둘로 갈렸습니다. 게스트는 초대 코드를 미리 적어 두면
 * 같이 먹기 창이 그 코드로 채워진 채 열려, 코드를 다시 옮겨 적지 않아도 됩니다.
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
        GroupSession.get().setDisplayName(
                user.getNickname() == null || user.getNickname().isBlank()
                        ? loginId : user.getNickname());
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

        // 코드는 선택이지만, 적었다면 형식이 맞는지 여기서 걸러 줍니다 —
        // 다음 창까지 들고 가서 틀렸다고 하면 되돌아오는 길이 깁니다
        String code = InviteCode.normalize(guestCodeField.getText());
        if (!code.isEmpty()) {
            try {
                InviteCode.parse(code);
            } catch (IllegalArgumentException e) {
                Widgets.showError(guestError, e.getMessage());
                return;
            }
        }

        Widgets.hideError(guestError);
        GroupSession.get().setDisplayName(name);
        GroupSession.get().setGuest(true);
        AppNav.dialog("같이 먹기", "group-option.fxml",
                (GroupOptionController controller) -> controller.prefillCode(code));
    }
}
