package com.safefood.view;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.function.Consumer;

public final class AppNav {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private static final String BASE = "/com/safefood/view/";
    private static final String CSS = BASE + "app.css";

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
            scene = new Scene(root, WIDTH, HEIGHT);
            applyCss(scene);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
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

        Scene modalScene = new Scene(root);
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

    // 알림창도 다이얼로그와 같은 이유로 미룹니다 — close() 직후 알림을 띄우는 코드가 안전해집니다.
    // (예: 정보 변경 창을 닫으며 "변경되었습니다" 안내)
    public static void info(String message) {
        Platform.runLater(() -> alert(Alert.AlertType.INFORMATION, "안내", message));
    }

    public static void warn(String message) {
        Platform.runLater(() -> alert(Alert.AlertType.WARNING, "확인해 주세요", message));
    }

    public static void error(String message) {
        Platform.runLater(() -> alert(Alert.AlertType.ERROR, "오류", message));
    }

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
