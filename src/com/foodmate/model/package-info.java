/**
 * 데이터를 담는 클래스 모음 (도메인 모델).
 *
 * <p>DB 테이블 하나당 클래스 하나를 기본으로 합니다.
 * <ul>
 *   <li>{@code User}, {@code UserPreference}, {@code Allergy}</li>
 *   <li>{@code Restaurant}, {@code Menu}, {@code Mood}</li>
 *   <li>{@code DiningGroup}, {@code GroupMember}, {@code GroupCandidate}</li>
 *   <li>{@code Favorite}, {@code History}, {@code Feedback}</li>
 *   <li>{@code RiskLevel} — CONTAINS / POSSIBLE / UNKNOWN (enum)</li>
 * </ul>
 *
 * <p>계산 로직은 두지 않고 필드와 getter/setter 위주로 작성합니다.
 * 테이블 정의는 README의 <b>데이터베이스 스키마</b> 절을 참고하세요.
 */
package com.foodmate.model;
