/**
 * 소켓 통신 (그룹 실시간 기능).
 *
 * <p><b>방을 만든 사람이 서버가 됩니다.</b> 따로 켜 두는 중앙 서버 없이,
 * 방장이 그룹을 만드는 순간 그 앱 안에서 {@code GroupServer}가 시작되고 일행이 직접 붙습니다.
 * 소켓은 실시간 전달, DB는 기록 보관으로 역할을 나눕니다.
 * <ul>
 *   <li>{@code GroupServer}   — 방장 앱이 여는 서버 ({@code ServerSocket}, 접속 수락)</li>
 *   <li>{@code ClientHandler} — 참여자 1명당 스레드</li>
 *   <li>{@code Room}          — 접속자 목록 · 브로드캐스트 · 투표 집계</li>
 *   <li>{@code GroupClient}   — 소켓 + 수신 전용 스레드 (방장도 자기 서버에 접속)</li>
 *   <li>{@code Message}       — {@code 타입|본문} 프로토콜 파싱·생성</li>
 * </ul>
 *
 * <p>메시지 종류·접속 주소 안내 방법·이 방식의 한계는
 * README의 <b>소켓 통신 설계</b> 절에 정리돼 있습니다.
 *
 * <p>구현할 때 특히 주의할 점
 * <ul>
 *   <li>{@code accept()}는 접속이 올 때까지 멈춥니다. {@code GroupServer}는 반드시 별도 스레드에서 돌리세요.</li>
 *   <li>접속자 목록·투표 집계는 여러 스레드가 함께 건드립니다.
 *       {@code ConcurrentHashMap}을 쓰거나 {@code synchronized}로 보호하세요.</li>
 *   <li>방장이 방을 닫을 때는 참여자에게 알린 뒤 모든 소켓을 정리합니다.</li>
 *   <li>DB에는 방장 쪽에서만 접근합니다. 참여자는 소켓으로만 대화하므로 MySQL이 없어도 참여할 수 있습니다.</li>
 * </ul>
 */
package com.safefood.network;
