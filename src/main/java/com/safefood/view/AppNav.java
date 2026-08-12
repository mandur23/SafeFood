package com.safefood.view;

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

    public static void dialog(String title, String fxml) {
        dialogRoot(title, load(fxml));
    }

    public static <C> void dialog(String title, String fxml, Consumer<C> initializer) {
        dialogRoot(title, load(fxml, initializer));
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

    public static void info(String message) {
        alert(Alert.AlertType.INFORMATION, "안내", message);
    }

    public static void warn(String message) {
        alert(Alert.AlertType.WARNING, "확인해 주세요", message);
    }

    public static void error(String message) {
        alert(Alert.AlertType.ERROR, "오류", message);
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
