package com.safefood;

import com.safefood.view.AppNav;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        AppNav.init(primaryStage);
        AppNav.show("로그인", "login.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
