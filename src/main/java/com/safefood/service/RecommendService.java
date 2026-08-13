package com.safefood.service;

import java.util.List;
import java.util.Map;


public class RecommendService {

    private final AllergyService allergyService;

    public RecommendService() {
        this.allergyService = new AllergyService();
    }

    // =========================================================================
    // 단일 메뉴에 대한 최종 추천 점수 계산 (Score Calculation)
    // =========================================================================
    /**
     * 메뉴의 기본 점수에서 알레르기 혼입 가능성(POSSIBLE) 감점 등을 적용하여
     * 최종 추천 점수를 계산
     */
    public int calculateMenuScore(int baseScore, Map<Integer, Integer> userSeverityMap, List<Integer> menuPossibleAllergyIds) {
        // AllergyService의 calculatePenalty()를 호출하여 감점 점수(penalty) 산출
        int penalty = allergyService.calculatePenalty(userSeverityMap, menuPossibleAllergyIds);

        // 기본 점수에서 감점 차감
        int finalScore = baseScore - penalty;

        // 점수가 음수가 되지 않도록 최소 0점 보장
        return Math.max(0, finalScore);
    }


    // =========================================================================
    // UI 출력용 개인 맞춤 추천 사유 문구 생성 (Reason Generation)
    // =========================================================================
    /**
     * SC-04 UI 카드에 표시될 맞춤 추천 사유 문구를 생성합니다.
     */
    public String generateRecommendReason(String categoryName, boolean isSafe, boolean hasPenalty) {
        if (!isSafe) {
            return "알레르기 유발 물질이 포함되어 위험한 메뉴입니다.";
        }

        // 안전하지만 혼입 가능성(POSSIBLE)으로 인한 감점이 있는 경우
        if (hasPenalty) {
            return "선호하시는 " + categoryName + " 메뉴이지만, 교차 오염 가능성이 있으니 주의하세요.";
        }

        // 완전하게 안전하고 감점도 없는 경우
        return "회원님이 선호하시는 " + categoryName + " 메뉴이며, 알레르기 위험 없이 안전한 추천 음식입니다.";
    }


    // =========================================================================
    // main 메서드
    // =========================================================================
    public static void main(String[] args) {
        RecommendService recommendService = new RecommendService();

        System.out.println("=== 1. 메뉴 추천 점수 계산 테스트 ===");
        int baseScore = 100;
        Map<Integer, Integer> userSeverityMap = Map.of(4, 3); // 땅콩(4번) 심각도 3
        List<Integer> menuPossibleAllergies = List.of(4);     // 땅콩 혼입 가능(POSSIBLE)

        int finalScore = recommendService.calculateMenuScore(baseScore, userSeverityMap, menuPossibleAllergies);
        System.out.println("최종 추천 점수 (70점이 나와야 함): " + finalScore + "점");


        System.out.println("\n=== 2. 추천 사유 문구 생성 테스트 ===");
        String reason1 = recommendService.generateRecommendReason("일식", true, false);
        System.out.println("안전한 메뉴 사유: " + reason1);

        String reason2 = recommendService.generateRecommendReason("한식", true, true);
        System.out.println("주의(POSSIBLE) 메뉴 사유: " + reason2);
    }
}