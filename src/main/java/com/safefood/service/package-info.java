/**
 * 비즈니스 로직 계층.
 *
 * <p>화면(view)과 DB(dao) 사이에서 "무엇을 어떻게 판단할지"를 담당합니다.
 * <ul>
 *   <li>{@code AuthService}      — 로그인 / 회원가입 (비밀번호는 반드시 해시로 저장)</li>
 *   <li>{@code RecommendService} — 추천 알고리즘 (후보 수집 → 필터 → 점수 계산)</li>
 *   <li>{@code AllergyService}   — 알레르기 매칭 / 위험도 판정</li>
 *   <li>{@code GroupService}     — 그룹 생성·참여, 조건 병합, 투표 집계</li>
 *   <li>{@code MapService}       — 지도 API 호출</li>
 * </ul>
 *
 * <p>추천 흐름과 그룹 조건 병합 규칙은 README의 <b>추천 알고리즘 구상</b> 절에 정리돼 있습니다.
 * 화면에 직접 출력({@code System.out})하지 말고 결과 객체를 돌려주세요.
 */
package com.safefood.service;
