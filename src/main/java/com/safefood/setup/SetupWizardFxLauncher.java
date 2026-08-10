package com.safefood.setup;

import javafx.application.Application;

/**
 * IntelliJ ▶ 버튼으로 {@link SetupWizardFx}를 실행하기 위한 시작 클래스.
 *
 * <p><b>왜 이런 클래스가 따로 필요한가</b>
 *
 * <p>JavaFX 11부터는 JavaFX가 JDK에서 빠져 별도 라이브러리가 됐습니다.
 * 그래서 JVM은 <b>메인 클래스가 {@link Application}을 상속하면</b>
 * JavaFX가 <i>모듈 경로(--module-path)</i>에 올라와 있는지 검사하고,
 * 없으면 이렇게 거절합니다.
 *
 * <pre>오류: 이 애플리케이션을 실행하는 데 필요한 JavaFX 런타임 구성요소가 누락되었습니다.</pre>
 *
 * <p>그런데 IntelliJ의 ▶ 버튼은 Maven 의존성을 <i>클래스패스(-classpath)</i>에 올립니다.
 * jar는 분명히 있는데도 위 오류가 나는 이유입니다.
 *
 * <p>이 검사는 <b>메인 클래스가 Application을 상속할 때만</b> 동작합니다.
 * 그래서 Application을 상속하지 않은 이 클래스를 대신 실행하면 검사를 지나가고,
 * JavaFX는 클래스패스에서 정상적으로 로드됩니다.
 *
 * <p><b>어느 것을 실행하나</b>
 * <table border="1">
 *   <caption>실행 방법별 시작 클래스</caption>
 *   <tr><th>실행 방법</th><th>시작 클래스</th></tr>
 *   <tr><td>IntelliJ ▶ 버튼</td><td>이 클래스 ({@code SetupWizardFxLauncher})</td></tr>
 *   <tr><td>{@code mvn javafx:run -Pwizard-fx}</td><td>{@link SetupWizardFx}
 *       (플러그인이 모듈 경로를 붙여 주므로 그대로 됨)</td></tr>
 * </table>
 *
 * <p>같은 이유로 앞으로 만들 앱 GUI에도 이런 시작 클래스가 하나 필요합니다.
 */
public final class SetupWizardFxLauncher {

    private SetupWizardFxLauncher() {
    }

    public static void main(String[] args) {
        // 실행할 Application 클래스를 명시적으로 넘깁니다.
        // (인자 없는 launch()는 호출한 클래스를 Application으로 보기 때문에 여기서는 쓸 수 없습니다)
        Application.launch(SetupWizardFx.class, args);
    }
}
