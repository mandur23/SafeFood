/**
 * 설정 마법사의 <b>처리부</b> — 화면이 없어도 돌아갑니다(headless).
 *
 * <p>이 패키지는 <b>화면을 알지 못합니다.</b> {@code System.out}도, JavaFX도 쓰지 않습니다.
 * 진행 상황은 {@link com.safefood.setup.core.SetupReporter}로 넘기고,
 * 그것을 어떻게 보여줄지는 화면 패키지({@code com.safefood.setup})가 정합니다.
 * 덕분에 콘솔판과 창(JavaFX)판이 <b>같은 코드를 그대로 씁니다.</b>
 *
 * <p>의존 방향은 한 방향입니다. 반대로 가는 import를 추가하지 마세요.
 *
 * <pre>
 * com.safefood.setup (화면)  ──→  com.safefood.setup.core (처리)
 * </pre>
 *
 * <h2>화면이 쓰는 것 (public)</h2>
 * <table border="1">
 *   <caption>화면 → 처리 진입점</caption>
 *   <tr><th>클래스</th><th>쓰임</th></tr>
 *   <tr><td>{@link com.safefood.setup.core.SetupService}</td><td>3~7단계 실행</td></tr>
 *   <tr><td>{@link com.safefood.setup.core.SetupRequest}</td><td>실행에 필요한 값 묶음</td></tr>
 *   <tr><td>{@link com.safefood.setup.core.SetupResult}</td><td>실행 결과</td></tr>
 *   <tr><td>{@link com.safefood.setup.core.SetupReporter}</td><td>진행 상황 통로 (화면이 구현)</td></tr>
 *   <tr><td>{@link com.safefood.setup.core.DbConfig}</td><td>접속 정보</td></tr>
 *   <tr><td>{@link com.safefood.setup.core.DatabaseInitializer}</td><td>연결 테스트, 오류 해석</td></tr>
 *   <tr><td>{@link com.safefood.setup.core.ConfigFileWriter}</td><td>프로젝트 경로 찾기</td></tr>
 *   <tr><td>{@link com.safefood.setup.core.JdbcDriverSetup}</td><td>드라이버 확인 (1단계)</td></tr>
 * </table>
 *
 * <p>{@code Schema}와 {@code DataStoreInitializer}는 이 패키지 안에서만 쓰므로 열어 두지 않았습니다.
 * 새 클래스를 추가할 때도 <b>화면이 직접 부르는 것만</b> public으로 두세요.
 *
 * <p>자세한 설명은 {@code docs/SetupWizard.md} 참고.
 */
package com.safefood.setup.core;
