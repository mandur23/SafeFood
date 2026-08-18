package com.safefood.service;

import com.safefood.dao.AllergyDao;
import com.safefood.dao.MenuDao;
import com.safefood.dao.MenuDao.MenuDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * [ SafeFood - 개인 맞춤 추천 엔진 서비스 ]
 * 담당자: 조영준
 * 관련 기능 ID: R-01(개인 추천 알고리즘), R-02(심각도 비례 감점/가산점 적용), R-03(추천 사유 문구 생성)
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
     * [추천 결과 DTO] UI 카드(SC-04) 출력을 위한 데이터 객체
     */
    public static class RecommendResult {
        private final String menuName;
        private final String category;
        private final int finalScore;
        private final String reason;

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
    // 1. 점수 연산 로직 (기본점수 100점 + 선호 카테고리 가산점 - 알레르기 감점)
    // =========================================================================
    public int calculateMenuScore(int baseScore, boolean isPreferredCategory,
                                  Map<Integer, Integer> userSeverityMap,
                                  List<Integer> menuPossibleAllergyIds) {

        int score = baseScore;

        // [가산점] 선호 카테고리 일치 시 +20점
        if (isPreferredCategory) {
            score += 20;
        }

        // [감점] POSSIBLE 알레르기 심각도 비례 감점 차감
        int penalty = allergyService.calculatePenalty(userSeverityMap, menuPossibleAllergyIds);
        score -= penalty;

        // 음수 방지 (최소 0점 보장)
        return Math.max(0, score);
    }

    // =========================================================================
    // 2. UI 카드 노출용 추천 사유 문구 세분화 (경고 제거 리팩토링)
    // =========================================================================
    public String generateRecommendReason(String categoryName, boolean isPreferred, boolean hasPenalty) {
        if (isPreferred) {
            if (hasPenalty) {
                return "선호하시는 [" + categoryName + "] 메뉴이지만, 제조 과정에서 알레르기 성분이 혼입되었을 수 있으니 주의하세요.";
            } else {
                return "회원님이 선호하시는 [" + categoryName + "] 메뉴이며, 알레르기 걱정 없이 안심하고 드실 수 있습니다!";
            }
        } else {
            if (hasPenalty) {
                return "알레르기 유발 성분의 교차 오염 가능성이 있으니 주의가 필요합니다.";
            } else {
                return "알레르기 유발 물질로부터 안전한 메뉴입니다.";
            }
        }
    }

    // =========================================================================
    // 3. DB 연동 개인 맞춤 추천 실행 (정렬 로직 포함)
    // =========================================================================
    public List<RecommendResult> getPersonalizedRecommendations(int userId, List<String> preferredCategories) {
        List<RecommendResult> results = new ArrayList<>();

        List<Integer> userAllergies = allergyDao.getUserAllergyIds(userId);
        Map<Integer, Integer> userSeverityMap = allergyDao.getUserSeverityMap(userId);
        List<MenuDto> candidateMenus = menuDao.getAllCandidateMenus();

        for (MenuDto menu : candidateMenus) {
            List<Integer> containsAllergies = allergyDao.getMenuAllergiesByRiskLevel(menu.getId(), "CONTAINS");
            List<Integer> possibleAllergies = allergyDao.getMenuAllergiesByRiskLevel(menu.getId(), "POSSIBLE");

            // [1차 필터링] CONTAINS 알레르기 포함 시 즉시 제외
            if (!allergyService.isSafe(userAllergies, containsAllergies)) {
                continue;
            }

            // [2차 선호 카테고리 매칭]
            boolean isPreferred = (preferredCategories != null && preferredCategories.contains(menu.getCategory()));

            // [3차 점수 연산]
            int baseScore = 100;
            int finalScore = calculateMenuScore(baseScore, isPreferred, userSeverityMap, possibleAllergies);

            // 감점 여부 확인
            int penaltyOnly = allergyService.calculatePenalty(userSeverityMap, possibleAllergies);
            boolean hasPenalty = (penaltyOnly > 0);

            // [4차 사유 생성]
            String reason = generateRecommendReason(menu.getCategory(), isPreferred, hasPenalty);

            results.add(new RecommendResult(menu.getName(), menu.getCategory(), finalScore, reason));
        }

        // 점수 내림차순 정렬
        results.sort(Comparator.comparingInt(RecommendResult::getFinalScore).reversed());

        return results;
    }

    // =========================================================================
    // [실행 및 테스트] main 메서드
    // =========================================================================
    public static void main(String[] args) {
        RecommendService service = new RecommendService();

        List<String> myFavoriteCategories = List.of("일식");

        System.out.println("=== 회원 PK 100 고도화 추천 결과 (점수 높은 순 정렬) ===");
        List<RecommendResult> recommendations = service.getPersonalizedRecommendations(100, myFavoriteCategories);

        for (RecommendResult result : recommendations) {
            System.out.println(result);
        }
    }
}