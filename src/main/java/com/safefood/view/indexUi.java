package com.safefood.view;

import javafx.application.Application;
import javafx.stage.Stage;

public class indexUi extends Application {

    @Override
    public void start(Stage stage) {
        AppNav.init(stage);
        AppNav.show("로그인", "login.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
