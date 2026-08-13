package com.safefood.service;

import com.safefood.dao.AllergyDao;
import com.safefood.dao.MenuDao;
import com.safefood.dao.MenuDao.MenuDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [ SafeFood - 개인 맞춤 추천 엔진 서비스 ]
 *
 * 담당자: 조영준
 * 관련 기능 ID: R-01(개인 추천 알고리즘), R-02(심각도 비례 감점 적용), R-03(추천 사유 문구 생성)
 */
public class RecommendService {

    private final AllergyService allergyService;
    private final AllergyDao allergyDao;
    private final MenuDao menuDao;

    public RecommendService() {
        this.allergyService = new AllergyService();
        this.allergyDao = new AllergyDao();
        this.menuDao = new MenuDao();
    }

    /**
     * [추천 결과 DTO] SC-04 UI 카드 출력을 위한 데이터 객체
     */
    public static class RecommendResult {
        private String menuName;
        private String category;
        private int finalScore;
        private String reason;

        public RecommendResult(String menuName, String category, int finalScore, String reason) {
            this.menuName = menuName;
            this.category = category;
            this.finalScore = finalScore;
            this.reason = reason;
        }

        public String getMenuName() { return menuName; }
        public String getCategory() { return category; }
        public int getFinalScore() { return finalScore; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("[%s (%s)] 점수: %d점 | 사유: %s", menuName, category, finalScore, reason);
        }
    }

    // =========================================================================
    // 1. 단일 메뉴 점수 연산 및 사유 생성
    // =========================================================================
    public int calculateMenuScore(int baseScore, Map<Integer, Integer> userSeverityMap, List<Integer> menuPossibleAllergyIds) {
        int penalty = allergyService.calculatePenalty(userSeverityMap, menuPossibleAllergyIds);
        int finalScore = baseScore - penalty;
        return Math.max(0, finalScore);
    }

    public String generateRecommendReason(String categoryName, boolean isSafe, boolean hasPenalty) {
        if (!isSafe) {
            return "알레르기 유발 물질이 포함되어 위험한 메뉴입니다.";
        }
        if (hasPenalty) {
            return "선호하시는 " + categoryName + " 메뉴이지만, 교차 오염 가능성이 있으니 주의하세요.";
        }
        return "회원님이 선호하시는 " + categoryName + " 메뉴이며, 알레르기 위험 없이 안전한 추천 음식입니다.";
    }

    // =========================================================================
    // 2. DB 데이터 기반 개인 맞춤 추천 실행 메서드
    // =========================================================================
    /**
     * 특정 회원(userId)을 위한 맞춤 추천 메뉴 리스트 도출
     */
    public List<RecommendResult> getPersonalizedRecommendations(int userId) {
        List<RecommendResult> results = new ArrayList<>();

        // 1. DB에서 회원의 알레르기 및 심각도 정보 조회
        List<Integer> userAllergies = allergyDao.getUserAllergyIds(userId);
        Map<Integer, Integer> userSeverityMap = allergyDao.getUserSeverityMap(userId);

        // 2. DB에서 전체 후보 메뉴 조회
        List<MenuDto> candidateMenus = menuDao.getAllCandidateMenus();

        for (MenuDto menu : candidateMenus) {
            // 3. 해당 메뉴의 CONTAINS / POSSIBLE 알레르기 DB 조회
            List<Integer> containsAllergies = allergyDao.getMenuAllergiesByRiskLevel(menu.getId(), "CONTAINS");
            List<Integer> possibleAllergies = allergyDao.getMenuAllergiesByRiskLevel(menu.getId(), "POSSIBLE");

            // 4. 1차 필터: CONTAINS 위험도 대조 (안전하지 않다면 추천에서 제외)
            boolean isSafe = allergyService.isSafe(userAllergies, containsAllergies);
            if (!isSafe) {
                continue;
            }

            // 5. 2차 필터 및 점수 차감: POSSIBLE 혼입 가능성 점수 계산 (기본 100점 시작)
            int baseScore = 100;
            int finalScore = calculateMenuScore(baseScore, userSeverityMap, possibleAllergies);
            boolean hasPenalty = (baseScore > finalScore);

            // 6. UI 추천 사유 문구 생성
            String reason = generateRecommendReason(menu.getCategory(), true, hasPenalty);

            results.add(new RecommendResult(menu.getName(), menu.getCategory(), finalScore, reason));
        }

        return results;
    }

    // =========================================================================
    // [통합 DB 연동 테스트] main 실행
    // =========================================================================
    public static void main(String[] args) {
        RecommendService service = new RecommendService();

        // user.id = 100 (조영준, 땅콩 알레르기 보유)
        // '땅콩 짜장면' -> CONTAINS(땅콩)이므로 완전히 제외됨
        // '순한 우동'   -> POSSIBLE(땅콩)이므로 심각도 3 반영 차감(30점 감점) 후 70점으로 출력됨
        System.out.println("=== 회원 PK 100 개인 맞춤 추천 결과 ===");
        List<RecommendResult> recommendations = service.getPersonalizedRecommendations(100);

        for (RecommendResult result : recommendations) {
            System.out.println(result);
        }
    }
}