package com.safefood;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // resources 폴더 내의 signup.fxml 경로 지정
        URL fxmlLocation = getClass().getResource("/com/safefood/view/signup.fxml");

        if (fxmlLocation == null) {
            throw new IllegalStateException("signup.fxml 파일을 찾을 수 없습니다. resources 경로를 다시 확인해 주세요.");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        primaryStage.setTitle("SafeFood - 회원가입 테스트");
        primaryStage.setScene(new Scene(root, 400, 500));
        primaryStage.show();
    }

    public static void main(String[] args) {
        // JavaFX 애플리케이션 실행
        launch(args);
    }
}