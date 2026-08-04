/**
 * DB 접근 계층 (JDBC).
 *
 * <p>SQL은 <b>이 패키지 안에서만</b> 다룹니다. service·view 코드에 SQL이 섞이지 않게 합니다.
 * <ul>
 *   <li>{@code UserDao}       — 회원 조회·저장, 취향·알레르기 읽기</li>
 *   <li>{@code MenuDao}       — 메뉴 조회, 알레르기 위험도 판정용 데이터</li>
 *   <li>{@code RestaurantDao} — 가게 조회 (거리·카테고리 필터)</li>
 *   <li>{@code GroupDao}      — 그룹·참여자·후보·투표</li>
 *   <li>{@code HistoryDao}    — 히스토리·즐겨찾기·피드백</li>
 * </ul>
 *
 * <p>조회 결과는 {@code com.foodmate.dto}의 클래스에 담아 돌려줍니다.
 * {@code ResultSet}을 그대로 밖으로 내보내지 마세요. (커넥션이 닫히면 못 읽습니다)
 *
 * <p>접속 정보는 {@code src/config.properties}에서 읽습니다. 코드에 직접 적지 마세요.
 * 값을 SQL에 넣을 때는 문자열을 이어 붙이지 말고 {@code PreparedStatement}의 {@code ?}를 씁니다.
 */
package com.foodmate.dao;
