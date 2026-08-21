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
 *   <li>{@code InviteCode}    — 접속 주소를 품은 초대 코드 (발급 · 해석)</li>
 *   <li>{@code RoomBeacon}    — 방장 쪽 자동 탐색 응답기 (UDP)</li>
 *   <li>{@code RoomFinder}    — 참여자 쪽 자동 탐색 질의 (UDP)</li>
 * </ul>
 *
 * <p><b>참여자가 방장을 찾는 방법은 두 단계입니다.</b> 중앙 서버가 없으니 코드로 주소를 조회해 줄 곳도
 * 없어서, 조회하는 대신 <b>주소를 초대 코드 안에 접어 넣습니다</b>({@code InviteCode}).
 * 다만 그 주소는 발급 당시의 값이라 방장의 IP가 바뀌면 낡은 값이 되므로,
 * 직접 접속이 실패할 때만 <b>같은 네트워크에 방을 찾는 질의</b>를 뿌립니다
 * ({@code RoomFinder} → {@code RoomBeacon}). 평소에는 UDP를 전혀 쓰지 않습니다.
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
 *   <li>자동 탐색(UDP)은 <b>실패해도 되는 보조 수단</b>입니다. 방화벽에 막혀 못 켜져도 코드에 담긴
 *       주소로는 그대로 접속되므로, 탐색 실패로 방 열기를 중단하지 마세요.</li>
 *   <li>탐색 질의에는 코드의 <b>주소부만</b> 싣습니다. 암호 역할을 하는 시크릿은 네트워크에 흘리지 않고,
 *       입장 자격은 TCP {@code JOIN}에서 {@code Room}이 코드 전체로 검사합니다.</li>
 * </ul>
 */
package com.safefood.network;
