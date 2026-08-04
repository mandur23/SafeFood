/**
 * 계층 사이로 데이터를 옮기는 클래스 모음 (DTO, Data Transfer Object).
 *
 * <p>DB 테이블 하나당 클래스 하나를 기본으로 하고, dao가 조회 결과를 여기에 담아
 * service·view로 전달합니다.
 * <ul>
 *   <li>{@code UserDto}, {@code UserPreferenceDto}, {@code AllergyDto}</li>
 *   <li>{@code RestaurantDto}, {@code MenuDto}, {@code MoodDto}</li>
 *   <li>{@code DiningGroupDto}, {@code GroupMemberDto}, {@code GroupCandidateDto}</li>
 *   <li>{@code FavoriteDto}, {@code HistoryDto}, {@code FeedbackDto}</li>
 *   <li>{@code RiskLevel} — CONTAINS / POSSIBLE / UNKNOWN (enum)</li>
 * </ul>
 *
 * <p>계산·판단 로직은 두지 않고 필드와 getter/setter 위주로 작성합니다.
 * "이 메뉴가 안전한가?" 같은 판단은 service의 몫입니다.
 *
 * <p>테이블 정의는 README의 <b>데이터베이스 스키마</b> 절을 참고하세요.
 */
package com.foodmate.dto;
