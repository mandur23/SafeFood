package com.safefood.service;

import com.safefood.dao.GroupDao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * [ SafeFood - 그룹 참여자 조건 병합 서비스 ]
 *
 * 담당자: 조영준
 * 관련 기능 ID: G-03 (그룹 조건 병합 알고리즘)
 */
public class GroupMergeService {

    private final GroupDao groupDao;

    public GroupMergeService() {
        this.groupDao = new GroupDao();
    }

    /**
     * [병합 결과 DTO] 그룹 조건 병합 결과를 담는 클래스
     */
    public static class MergedGroupCondition {
        private List<Integer> mergedAllergies;
        private int minSpicyLevel;

        public MergedGroupCondition(List<Integer> mergedAllergies, int minSpicyLevel) {
            this.mergedAllergies = mergedAllergies;
            this.minSpicyLevel = minSpicyLevel;
        }

        public List<Integer> getMergedAllergies() { return mergedAllergies; }
        public int getMinSpicyLevel() { return minSpicyLevel; }

        @Override
        public String toString() {
            return String.format("[그룹 병합 결과] 알레르기 합집합: %s | 매운맛 최솟값: %d단계", mergedAllergies, minSpicyLevel);
        }
    }

    // =========================================================================
    // 1. 단순 순수 자바 병합 로직 (기존 구현 완료)
    // =========================================================================
    public List<Integer> mergeAllergies(List<List<Integer>> membersAllergies) {
        if (membersAllergies == null || membersAllergies.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> mergedSet = new HashSet<>();
        for (List<Integer> singleMemberAllergies : membersAllergies) {
            if (singleMemberAllergies != null) {
                mergedSet.addAll(singleMemberAllergies);
            }
        }
        return new ArrayList<>(mergedSet);
    }

    public int mergeSpicyLevel(List<Integer> memberSpicyLevels) {
        if (memberSpicyLevels == null || memberSpicyLevels.isEmpty()) {
            return 5;
        }

        int minSpicy = 5;
        for (Integer level : memberSpicyLevels) {
            if (level != null) {
                minSpicy = Math.min(minSpicy, level);
            }
        }
        return minSpicy;
    }

    // =========================================================================
    // 2. DB 데이터 기반 그룹 조건 통합 병합 실행 메서드 (신규 추가)
    // =========================================================================
    /**
     * 特定 그룹(groupId)의 참여자 조건들을 DB에서 조회하여 합집합 알레르기 및 최솟값 매운맛 조건 생성
     */
    public MergedGroupCondition getMergedConditionForGroup(int groupId) {
        // 1. DB에서 참여자 알레르기 스냅샷 목록 조회 및 합집합 연산
        List<List<Integer>> membersAllergies = groupDao.getGroupMembersAllergies(groupId);
        List<Integer> mergedAllergies = mergeAllergies(membersAllergies);

        // 2. DB에서 참여자 매운맛 선호도 목록 조회 및 최솟값 연산
        List<Integer> spicyLevels = groupDao.getGroupMembersSpicyLevels(groupId);
        int minSpicyLevel = mergeSpicyLevel(spicyLevels);

        return new MergedGroupCondition(mergedAllergies, minSpicyLevel);
    }

    // =========================================================================
    // [실행 및 테스트] main 메서드
    // =========================================================================
    public static void main(String[] args) {
        GroupMergeService service = new GroupMergeService();

        // 1. 순수 연산 테스트
        System.out.println("=== 1. 자바 병합 로직 테스트 ===");
        List<List<Integer>> memberAllergies = List.of(List.of(4), List.of(1, 2), List.of(4, 5));
        System.out.println("병합 알레르기: " + service.mergeAllergies(memberAllergies));

        // 2. DB 통합 테스트 (그룹 ID 1번 기준)
        System.out.println("\n=== 2. DB 연동 그룹 조건 병합 테스트 ===");
        MergedGroupCondition result = service.getMergedConditionForGroup(1);
        System.out.println(result);
    }
}