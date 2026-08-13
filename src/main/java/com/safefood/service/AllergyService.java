package com.safefood.service;

import java.util.List;
import java.util.Map;

public class AllergyService {

    // (추후 DB 연동 시) AllergyDao 선언 및 생성자 작성하기
    // private final AllergyDao allergyDao;
    // public AllergyService() { this.allergyDao = new AllergyDao(); }


    // =========================================================================
    // 알레르기 안전성 판정 (CONTAINS)
    // =========================================================================
    /**
     * 사용자의 보유 알레르기와 메뉴의 CONTAINS(원재료 포함) 알레르기를 대조하여
     * 메뉴가 안전한지 여부를 판단
     *
     * @param userAllergyIds 사용자가 가지고 있는 알레르기 ID 목록 (예: [4] - 땅콩)
     * @param menuContainsAllergyIds 메뉴에 확실히 포함된 CONTAINS 알레르기 ID 목록
     * @return 안전하면 true, 하나라도 겹치면 false (추천 후보 제외 대상)
     */
    public boolean isSafe(List<Integer> userAllergyIds, List<Integer> menuContainsAllergyIds) {
        if (userAllergyIds == null || userAllergyIds.isEmpty()) {
            return true; // 사용자가 보유한 알레르기가 없으면 무조건 안전
        }
        if (menuContainsAllergyIds == null || menuContainsAllergyIds.isEmpty()) {
            return true; // 메뉴에 알레르기 유발물질이 없으면 무조건 안전
        }

        for (Integer userAllergy : userAllergyIds) {
            if (menuContainsAllergyIds.contains(userAllergy)) {
                return false; // 위험한 알레르기 포함됨
            }
        }
        return true;
    }


    // =========================================================================
    // 혼입 가능성 감점 점수 계산 (POSSIBLE)
    // =========================================================================
    /**
     * 메뉴에 POSSIBLE(혼입 가능성 있음) 등급으로 지정된 알레르기가 있을 때,
     * 사용자의 알레르기 심각도(user_allergy.severity: 1~5단계)에 비례하여 감점 점수를 계산합니다.
     *
     * @param userSeverityMap 사용자의 알레르기 ID별 심각도 Map (Key: allergy_id, Value: severity 1~5)
     *                        예: Map.of(4, 3) -> 땅콩(4번) 심각도 3
     * @param menuPossibleAllergyIds 메뉴의 POSSIBLE 등급 알레르기 ID 목록 (예: [4] -> 땅콩 혼입 가능)
     * @return 감점할 총 점수 (기본 점수 100점에서 차감할 점수)
     */
    public int calculatePenalty(Map<Integer, Integer> userSeverityMap, List<Integer> menuPossibleAllergyIds) {
        int totalPenalty = 0;

        // 예외 처리: 데이터가 없는 경우 0점 감점
        if (userSeverityMap == null || menuPossibleAllergyIds == null || menuPossibleAllergyIds.isEmpty()) {
            return 0;
        }

        // 메뉴의 possible 알레르기 ID를 순회
        for (Integer possibleAllergyId :menuPossibleAllergyIds) {
            // if 사용자가 해당 알레르기를 갖고 있는 경우
            if(userSeverityMap.containsKey(possibleAllergyId)){
                // 해당 알레르기의 심각도(1~5) 추출
                int severity = userSeverityMap.get(possibleAllergyId);
                // 심각도 *10점씩 누적 감점
                totalPenalty = severity * 10;
            }
        }

        return totalPenalty;
    }


    // =========================================================================
    // 알레르기 차단 이력(BLOCKED) 안내
    // =========================================================================
    /**
     * CONTAINS 등급으로 차단된 메뉴 정보를 history 테이블에 type='BLOCKED'로
     * 기록하거나 사용자에게 알릴 사유 문구를 생성
     *
     * @param blockedAllergyNames 차단 원인이 된 알레르기 이름 목록 (예: ["땅콩", "대두"])
     * @return 차단 사유 안내 문구 (예: "알레르기 유발물질(땅콩, 대두)이 포함되어 제외되었습니다.")
     */
    public String createBlockedReason(List<String> blockedAllergyNames) {
        if (blockedAllergyNames == null || blockedAllergyNames.isEmpty()) {
            return "";
        }

        String names = String.join(" ,", blockedAllergyNames);

        return "알레르기 유발물질(" + names + ")이 포함되어 추천에서 제외되었습니다.";
    }



    public static void main(String[] args) {
        AllergyService service = new AllergyService();

        System.out.println("=== 1. isSafe 테스트 ===");
        List<Integer> myAllergies = List.of(4); // 땅콩(4)
        List<Integer> menuA = List.of(4, 2);    // 땅콩, 계란 포함  false
        List<Integer> menuB = List.of(1, 2);    // 우유, 계란 포함  true

        System.out.println("A메뉴(땅콩 있음) 먹어도 되나요?: " + service.isSafe(myAllergies, menuA)); // false
        System.out.println("B메뉴(땅콩 없음) 먹어도 되나요?: " + service.isSafe(myAllergies, menuB)); // true


        System.out.println("\n=== 2. calculatePenalty 테스트 ===");
        // 땅콩(4) 알레르기 심각도가 3단계인 사용자
        Map<Integer, Integer> mySeverityMap = Map.of(4, 3);
        List<Integer> menuC_Possible = List.of(4); // 땅콩 혼입 가능성이 있는 메뉴

        int penalty = service.calculatePenalty(mySeverityMap, menuC_Possible);
        System.out.println("감점 점수 (30점이 나와야 함): " + penalty + "점");


        System.out.println("\n=== 3. createBlockedReason 테스트 ===");
        String reason = service.createBlockedReason(List.of("땅콩", "대두"));
        System.out.println("차단 사유 문구: " + reason);
    }
}