/**
 * SafeFood 애플리케이션 루트 패키지.
 *
 * <p>하위 패키지 구성 (자세한 내용은 README의 <b>프로젝트 구조</b> 절 참고)
 * <ul>
 *   <li>{@code dto}     — 계층 사이로 데이터를 옮기는 클래스</li>
 *   <li>{@code dao}     — DB 접근 (JDBC)</li>
 *   <li>{@code service} — 비즈니스 로직 (인증·추천·알레르기·그룹·지도)</li>
 *   <li>{@code network} — 소켓 통신 (그룹 실시간 기능)</li>
 *   <li>{@code view}    — 화면 출력과 입력 처리</li>
 * </ul>
 *
 * <p>의존 방향은 <b>view → service → dao → DB</b> 한 방향으로만 흐르게 합니다.
 * (dao가 service를 부르거나, service가 화면에 직접 출력하지 않도록)
 */
package com.safefood;
