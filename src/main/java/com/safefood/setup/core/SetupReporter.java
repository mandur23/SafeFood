package com.safefood.setup.core;

/**
 * 마법사의 진행 상황을 화면에 전달하는 통로.
 *
 * <p>DB 작업·파일 작업 클래스가 {@code System.out}을 직접 부르면 콘솔에서만 쓸 수 있게 됩니다.
 * 그래서 "무엇을 알릴지"만 이 인터페이스로 넘기고, "어떻게 보여줄지"는 각 화면이 정합니다.
 * <ul>
 *   <li>콘솔 → {@code Ui.reporter()} (텍스트 출력)</li>
 *   <li>JavaFX → {@code SetupWizardFx} (로그 패널에 색을 넣어 표시)</li>
 * </ul>
 *
 * <p>README의 계층 규칙(“service·dao는 System.out을 직접 쓰지 않는다”)과 같은 이유입니다.
 *
 * <p>구현은 화면 쪽({@code com.safefood.setup})에 있습니다.
 * 이 패키지는 화면을 알지 못하고, 화면이 이 인터페이스를 구현해 넘겨 줍니다.
 */
public interface SetupReporter {

    /** 단계 시작을 알립니다. (예: {@code step(3, 7, "데이터베이스 생성")}) */
    void step(int number, int total, String title);

    /** 성공 */
    void ok(String message);

    /** 경고 — 넘어갈 수는 있지만 알아 둬야 하는 상황 */
    void warn(String message);

    /** 실패 */
    void fail(String message);

    /** 일반 안내 */
    void info(String message);

    /** 목록·부연 설명처럼 한 단계 들여쓸 내용 */
    void detail(String message);
}
