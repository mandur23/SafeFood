package com.safefood.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GroupMergeService {

    // =========================================================================
    // 참여자 알레르기 목록 합집합 연산 (Set/List)
    // =========================================================================
    /**
     * 여러 참여자의 알레르기 ID 목록을 합쳐서 중복이 없는 합집합 알레르기 리스트를 반환합니다.
     *
     * @param membersAllergies 각 참여자별 알레르기 ID 리스트의 리스트
     *                         예: [[4], [1, 2]] -> 1번 참여자는 땅콩(4), 2번 참여자는 우유(1), 계란(2)
     * @return 병합된 알레르기 ID 리스트 (중복 제거된 [4, 1, 2])
     */
    public List<Integer> mergeAllergies(List<List<Integer>> membersAllergies) {
        if (membersAllergies == null || membersAllergies.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> mergedSet = new HashSet<>();

        for(List<Integer> singleMemberAllergies : membersAllergies) {
            if(singleMemberAllergies != null) {
                mergedSet.addAll(singleMemberAllergies);
            }
        }

        return new ArrayList<>(mergedSet);
    }


    // =========================================================================
    // 매운맛 수용 단계 최솟값 계산
    // =========================================================================
    /**
     * 참여자들의 매운맛 수용 단계(1~5단계) 중 가장 낮은 값을 선택합니다.
     *
     * @param memberSpicyLevels 각 참여자의 매운맛 수용 단계 리스트 (예: [3, 1, 4] -> 가장 못먹는 사람은 1단계)
     * @return 그룹의 최종 매운맛 적용 단계 (기본값: 5, 입력된 최솟값 선택)
     */
    public int mergeSpicyLevel(List<Integer> memberSpicyLevels) {
        if (memberSpicyLevels == null || memberSpicyLevels.isEmpty()) {
            return 5; // 입력 정보가 없으면 기본 5단계
        }

        int minSpicy = 5; // 최대 단계로 초기화

        for(Integer level : memberSpicyLevels) {
            if(level != null) {
                minSpicy = Math.min(minSpicy, level);
            }
        }
        return minSpicy;
    }


    // =========================================================================
    // [실행 및 테스트] main 메서드
    // =========================================================================
    public static void main(String[] args) {
        GroupMergeService service = new GroupMergeService();

        System.out.println("=== 1. 알레르기 합집합 병합 테스트 ===");
        List<List<Integer>> memberAllergies = List.of(
                List.of(4),       // 참여자 A: 땅콩(4)
                List.of(1, 2),    // 참여자 B: 우유(1), 계란(2)
                List.of(4, 5)     // 참여자 C: 땅콩(4), 대두(5)
        );

        List<Integer> mergedAllergies = service.mergeAllergies(memberAllergies);
        System.out.println("병합된 알레르기 목록 (1, 2, 4, 5가 나와야 함): " + mergedAllergies);


        System.out.println("\n=== 2. 매운맛 최솟값 병합 테스트 ===");
        List<Integer> memberSpicyLevels = List.of(3, 1, 4); // 1단계가 최솟값
        int finalSpicy = service.mergeSpicyLevel(memberSpicyLevels);
        System.out.println("최종 적용 매운맛 단계 (1이 나와야 함): " + finalSpicy + "단계");
    }
}