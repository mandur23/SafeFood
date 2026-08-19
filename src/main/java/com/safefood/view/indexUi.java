package com.safefood.view;

import com.safefood.Main;

/**
 * {@code mvn javafx:run} 진입점 (pom.xml의 javafx-maven-plugin이 이 클래스를 실행).
 * 시작 로직은 {@link Main}과 같아서 상속만 합니다 — 시작 화면을 바꿀 때는 Main만 고치면 됩니다.
 */
public class indexUi extends Main {

    public static void main(String[] args) {
        launch(indexUi.class, args);
    }
}
