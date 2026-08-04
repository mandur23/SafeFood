/**
 * 소켓 통신 (그룹 실시간 기능).
 *
 * <p>여러 명이 동시에 참여·투표하는 그룹 추천을 위해 TCP 연결을 유지합니다.
 * 소켓은 실시간 전달, DB는 기록 보관으로 역할을 나눕니다.
 * <ul>
 *   <li>{@code GroupServer}   — {@code ServerSocket}으로 접속 수락 (서버 진입점)</li>
 *   <li>{@code ClientHandler} — 클라이언트 1명당 스레드</li>
 *   <li>{@code RoomManager}   — 초대 코드 단위 접속자 관리 / 브로드캐스트</li>
 *   <li>{@code GroupClient}   — 클라이언트 소켓 + 수신 전용 스레드</li>
 *   <li>{@code Message}       — {@code 타입|본문} 프로토콜 파싱·생성</li>
 * </ul>
 *
 * <p>메시지 종류와 진행 흐름은 README의 <b>소켓 통신 설계</b> 절을 참고하세요.
 *
 * <p>주의: 접속자 목록·투표 집계는 여러 스레드가 함께 건드립니다.
 * {@code ConcurrentHashMap}을 쓰거나 {@code synchronized}로 보호하세요.
 */
package com.foodmate.network;
