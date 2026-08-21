package com.safefood.view;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.function.Consumer;

public final class AppNav {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private static final String BASE = "/com/safefood/view/";
    private static final String CSS = BASE + "app.css";

    /** 토스트가 쌓이는 층. 모든 Scene 의 루트 StackPane 이 하나씩 갖습니다. */
    private static final String TOAST_LAYER_ID = "toast-layer";

    /** 한 번에 보여 줄 최대 개수 — 넘치면 오래된 것부터 밀어냅니다. */
    private static final int MAX_TOASTS = 3;

    private static final Duration TOAST_IN = Duration.millis(180);
    private static final Duration TOAST_OUT = Duration.millis(240);

    private static Stage stage;
    private static Scene scene;

    private AppNav() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
    }

    public static Parent load(String fxml) {
        return read(loader(fxml));
    }

    public static <C> Parent load(String fxml, Consumer<C> initializer) {
        FXMLLoader loader = loader(fxml);
        Parent root = read(loader);
        initializer.accept(loader.getController());
        return root;
    }

    private static FXMLLoader loader(String fxml) {
        URL url = AppNav.class.getResource(BASE + fxml);
        if (url == null) {

            throw new IllegalStateException(
                    BASE + fxml + "을 찾지 못했습니다. src/main/resources 아래에 있는지 확인하세요.");
        }
        return new FXMLLoader(url);
    }

    private static Parent read(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {

            throw new UncheckedIOException(
                    "FXML을 읽지 못했습니다: " + loader.getLocation(), e);
        }
    }

    public static void show(String title, String fxml) {
        showRoot(title, load(fxml));
    }

    private static void showRoot(String title, Parent root) {
        if (scene == null) {
            scene = new Scene(withToastLayer(root), WIDTH, HEIGHT);
            applyCss(scene);
            stage.setScene(scene);
        } else {
            // 루트를 통째로 갈지 않고 화면만 갈아 끼웁니다 — 토스트 층이 살아남아야 합니다
            ((StackPane) scene.getRoot()).getChildren().set(0, root);
        }
        stage.setTitle(title);
        if (!stage.isShowing()) {
            stage.show();
        }
    }

    /**
     * 다이얼로그 열기. 실제로 여는 시점은 {@code Platform.runLater}로 한 박자 미룹니다.
     *
     * <p>⚠️ 미루지 않으면: {@code close(root); dialog(...)}처럼 <b>이전 다이얼로그를 닫는 중에</b>
     * 곧바로 다음 다이얼로그의 {@code showAndWait} 중첩 루프를 쌓으면, JavaFX가
     * "루프에서 나가는 중" 상태에 갇혀 화면 갱신(렌더 펄스)을 포함한 {@code runLater} 배달을
     * 전부 멈춥니다 → 새 창이 제목만 뜨고 내용은 하얗게 나옵니다. 미루면 이전 창의 루프가
     * 완전히 빠져나간 뒤에 새 루프가 시작되어 안전합니다.
     */
    public static void dialog(String title, String fxml) {
        Platform.runLater(() -> dialogRoot(title, load(fxml)));
    }

    public static <C> void dialog(String title, String fxml, Consumer<C> initializer) {
        Platform.runLater(() -> dialogRoot(title, load(fxml, initializer)));
    }

    private static void dialogRoot(String title, Parent root) {
        Stage modal = new Stage();
        modal.initOwner(stage);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(title);

        // 다이얼로그도 자기 토스트 층을 갖습니다 — 안 그러면 '복사되었습니다' 같은 알림이
        // 모달 뒤(본 창)에 떠서 보이지 않습니다
        Scene modalScene = new Scene(withToastLayer(root));
        applyCss(modalScene);
        modal.setScene(modalScene);
        modal.setResizable(false);
        modal.showAndWait();
    }

    public static void close(Node nodeInDialog) {
        if (nodeInDialog != null && nodeInDialog.getScene() != null
                && nodeInDialog.getScene().getWindow() != null) {
            nodeInDialog.getScene().getWindow().hide();
        }
    }

    /**
     * 다이얼로그가 창 닫기(X)로 닫힐 때 실행할 정리 동작을 등록합니다.
     * 코드로 {@link #close(Node)}를 부른 경우(hide)에는 실행되지 않습니다 —
     * 버튼으로 닫을 때와 X로 닫을 때의 정리를 다르게 하고 싶을 때 쓰세요.
     */
    public static void onDialogClosed(Node root, Runnable action) {
        root.sceneProperty().addListener((sceneObs, oldScene, scene) -> {
            if (scene == null) {
                return;
            }
            if (scene.getWindow() != null) {
                scene.getWindow().setOnCloseRequest(event -> action.run());
            } else {
                scene.windowProperty().addListener((winObs, oldWin, window) -> {
                    if (window != null) {
                        window.setOnCloseRequest(event -> action.run());
                    }
                });
            }
        });
    }

    // ══ 알림 — 토스트 ═══════════════════════════════════════════

    /*
     * 예전에는 셋 다 모달 Alert 이었습니다. "복사되었습니다" 한 줄을 읽으려고 확인 버튼을
     * 누르게 만드는 것이 리디자인의 '클릭 줄이기'와 정면으로 어긋나서, 화면 위에 잠깐
     * 떠올랐다 사라지는 토스트로 바꿨습니다.
     *
     * 토스트는 클릭하면 즉시 사라지고, 글이 길수록 오래 머뭅니다. 답을 받아야 하는
     * confirm() 만 모달로 남습니다 — 그건 사라지면 안 되는 물음입니다.
     */

    public static void info(String message) {
        toast(message, "info");
    }

    public static void warn(String message) {
        toast(message, "warn");
    }

    public static void error(String message) {
        toast(message, "error");
    }

    /** 좋은 소식용 — 세이지 톤. */
    public static void success(String message) {
        toast(message, "good");
    }

    private static void toast(String message, String variant) {
        // 수신 스레드에서 부르는 자리가 있어 UI 스레드로 넘깁니다.
        // 미루는 김에 close() 직후의 호출도 안전해집니다 — 창이 다 닫힌 뒤에 자리를 고릅니다.
        Platform.runLater(() -> showToast(message, variant));
    }

    private static void showToast(String message, String variant) {
        VBox layer = activeToastLayer();
        if (layer == null) {
            alert(Alert.AlertType.INFORMATION, "안내", message);   // 띄울 자리가 없으면 예전 방식으로
            return;
        }

        Label card = new Label(message);
        card.getStyleClass().addAll("toast", variant);
        card.setWrapText(true);
        card.setMaxWidth(440);
        card.setOpacity(0);
        card.setTranslateY(16);

        while (layer.getChildren().size() >= MAX_TOASTS) {
            layer.getChildren().remove(0);
        }
        layer.getChildren().add(card);

        FadeTransition fadeIn = new FadeTransition(TOAST_IN, card);
        fadeIn.setToValue(1);
        TranslateTransition riseIn = new TranslateTransition(TOAST_IN, card);
        riseIn.setToY(0);
        riseIn.play();

        PauseTransition stay = new PauseTransition(readingTime(message));

        FadeTransition fadeOut = new FadeTransition(TOAST_OUT, card);
        fadeOut.setToValue(0);

        SequentialTransition life = new SequentialTransition(fadeIn, stay, fadeOut);
        life.setOnFinished(event -> layer.getChildren().remove(card));
        life.play();

        // 눌러서 바로 치우기 — 긴 오류 문구를 다 읽은 사람이 기다리지 않아도 되게
        card.setOnMouseClicked(event -> {
            life.stop();
            layer.getChildren().remove(card);
        });
    }

    /** 글이 길수록 오래 머뭅니다 — 2.5초에서 시작해 8초까지. */
    private static Duration readingTime(String message) {
        double millis = 2500 + message.length() * 45.0;
        return Duration.millis(Math.min(millis, 8000));
    }

    /**
     * 지금 앞에 나와 있는 창의 토스트 층. 모달 다이얼로그가 떠 있으면 그 위에 얹어야
     * 보이므로, 본 창이 아니라 <b>포커스를 가진 창</b>을 기준으로 찾습니다.
     */
    private static VBox activeToastLayer() {
        Window target = null;
        for (Window window : Window.getWindows()) {
            if (window.isFocused() && window.isShowing()) {
                target = window;
                break;
            }
        }
        if (target == null) {
            target = stage;
        }
        if (target == null || target.getScene() == null) {
            return null;
        }
        Node found = target.getScene().getRoot().lookup("#" + TOAST_LAYER_ID);
        return found instanceof VBox layer ? layer : null;
    }

    /** 화면을 토스트 층과 함께 StackPane 에 담습니다. 화면이 아래, 토스트가 위. */
    private static Parent withToastLayer(Parent content) {
        VBox layer = new VBox(8);
        layer.setId(TOAST_LAYER_ID);
        layer.getStyleClass().add("toast-layer");
        layer.setAlignment(Pos.BOTTOM_CENTER);
        // 층 자체는 클릭을 가로채지 않고, 토스트 카드에만 클릭이 닿게 합니다
        layer.setPickOnBounds(false);

        return new StackPane(content, layer);
    }

    // ══ 확인 — 답을 받아야 해서 모달로 남습니다 ══════════════════

    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        decorate(alert, "확인");
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    private static void alert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        decorate(alert, header);
        alert.showAndWait();
    }

    private static void decorate(Alert alert, String header) {
        alert.setTitle("SafeFood");
        alert.setHeaderText(header);
        if (stage != null) {
            alert.initOwner(stage);
        }
        applyCss(alert.getDialogPane().getScene());
    }

    private static void applyCss(Scene target) {
        if (target == null) {
            return;
        }
        URL css = AppNav.class.getResource(CSS);
        if (css != null) {
            target.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("[UI] " + CSS + "를 찾지 못했습니다. 스타일 없이 표시합니다.");
        }
    }
}
