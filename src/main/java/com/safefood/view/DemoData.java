package com.safefood.view;

import java.util.List;

public final class DemoData {

    private DemoData() {
    }

    public static final List<String> ALLERGIES = List.of(
            "우유", "계란", "메밀", "땅콩", "대두", "밀", "호두", "잣",
            "새우", "게", "오징어", "조개류", "고등어", "복숭아", "토마토",
            "돼지고기", "쇠고기", "닭고기", "아황산류"
    );

    public static final List<String> MOODS = List.of(
            "느끼함", "스트레스", "해장", "더움", "추움",
            "피곤함", "가벼운 식사", "든든하게", "혼밥", "기념일"
    );

    public static final List<String> CATEGORIES = List.of(
            "한식", "중식", "일식", "양식", "아시안", "패스트푸드"
    );

    public static final List<String> DISTANCES = List.of(
            "500m", "1km", "2km", "3km", "제한없음"
    );

    public static final List<String> BUDGETS = List.of(
            "6,000원 이하", "8,000원 이하", "10,000원 이하", "12,000원 이하",
            "15,000원 이하", "20,000원 이하", "제한없음"
    );

    public static final List<String> SEVERITIES = List.of(
            "Class 1 거의 없음", "Class 2 낮음", "Class 3 보통", "Class 4 높음", "Class 5 극도로 높음"
    );

    public enum Safety {

        SAFE("SAFE (알레르기 안전)", "✓ 알레르기 안전", "safe"),

        POSSIBLE("POSSIBLE (혼입 가능)", "⚠ 혼입 가능", "possible"),

        CONTAINS("CONTAINS (알레르기 원료 포함)", "✕ 원료 포함", "contains");

        public final String label;

        /** 카드·칩에 들어가는 짧은 표기 — 리디자인은 약어 대신 이쪽을 씁니다. */
        public final String shortLabel;

        public final String styleClass;

        Safety(String label, String shortLabel, String styleClass) {
            this.label = label;
            this.shortLabel = shortLabel;
            this.styleClass = styleClass;
        }
    }

    public record Recommendation(int rank, int score, String menu, String restaurant,
                                 Safety safety, int spicyLevel, String reason,
                                 String address, String hours, double rating,
                                 String distance, boolean blocked, String alternative) {
    }

    public static final List<Recommendation> RECOMMENDATIONS = List.of(
            new Recommendation(1, 94, "김치찌개", "할머니손맛",
                    Safety.SAFE, 3,
                    "선호하는 한식이고, 등록하신 알레르기 재료가 들어가지 않습니다. "
                            + "매운맛 3단계로 설정하신 정도와 맞고, 예산 안에 들어옵니다.",
                    "서울 강남구 테헤란로 12길 5", "11:00 ~ 21:00", 4.5,
                    "350m", false, null),

            new Recommendation(2, 88, "돈코츠 라멘", "이치란야",
                    Safety.POSSIBLE, 1,
                    "선호 카테고리는 아니지만 '든든하게' 태그와 잘 맞습니다. "
                            + "같은 조리대에서 새우를 다루므로 혼입 가능성이 있습니다.",
                    "서울 강남구 역삼로 24", "11:30 ~ 22:00", 4.2,
                    "600m", false, null),

            new Recommendation(0, 0, "감바스 알 아히요", "라핀카",
                    Safety.CONTAINS, 2,
                    "등록하신 '새우' 알레르기 원료가 포함되어 후보에서 제외했습니다.",
                    "서울 강남구 논현로 145", "17:00 ~ 24:00", 4.7,
                    "820m", true, "버섯 아히요는 드실 수 있어요")
    );

    public record HistoryRow(String date, String type, String menu, String restaurant, String note) {
    }

    public static final List<HistoryRow> HISTORY = List.of(
            new HistoryRow("2026-08-12", "EATEN", "김치찌개", "할머니손맛", "알레르기 안전"),
            new HistoryRow("2026-08-12", "BLOCKED", "감바스 알 아히요", "라핀카", "새우 포함으로 차단"),
            new HistoryRow("2026-08-11", "RECOMMENDED", "돈코츠 라멘", "이치란야", "혼입 가능 — 경고 표시"),
            new HistoryRow("2026-08-11", "VIEWED", "마라탕", "홍대마라", "알레르기 안전"),
            new HistoryRow("2026-08-10", "EATEN", "제육볶음", "백반집", "알레르기 안전")
    );

    public record FavoriteRow(String restaurant, String menu, double rating, int reviewCount) {
    }

    public static final List<FavoriteRow> FAVORITES = List.of(
            new FavoriteRow("할머니손맛", "김치찌개", 4.5, 312),
            new FavoriteRow("이치란야", "돈코츠 라멘", 4.2, 1024),
            new FavoriteRow("백반집", "제육볶음", 4.0, 87)
    );

    public record Member(String name, String role, String state) {
    }

    public static final List<Member> MEMBERS = List.of(
            new Member("김민수", "방장", "READY"),
            new Member("길동현", "참여자", "READY"),
            new Member("조영준", "참여자", "WAITING")
    );

    public record Candidate(int no, String menu, String restaurant, int votes) {
    }

    public static final List<Candidate> CANDIDATES = List.of(
            new Candidate(1, "김치찌개", "할머니손맛", 2),
            new Candidate(2, "제육볶음", "백반집", 1),
            new Candidate(3, "비빔밥", "한그릇", 0)
    );

    public static final List<String> SOCKET_LOG = List.of(
            "[19:02:11] 서버 시작 (:5000) — 초대 코드 ABC123",
            "[19:02:40] JOINED|김민수|1",
            "[19:03:02] JOINED|길동현|2",
            "[19:03:15] JOINED|조영준|3",
            "[19:03:50] INFO|새우,땅콩|2|10000  (길동현)",
            "[19:04:05] READY  (길동현)",
            "[19:04:20] CANDIDATES|1.김치찌개,2.제육볶음,3.비빔밥",
            "[19:04:33] VOTE_STATUS|1:2,2:1,3:0"
    );

    public static final List<String> MERGED_CONDITION = List.of(
            "알레르기 (합집합) — 새우 · 땅콩 · 우유 제외",
            "매운맛 (최솟값) — 2단계 이하",
            "예산 (교집합) — 8,000원 ~ 10,000원",
            "선호 카테고리 (교집합) — 한식",
            "탐색 반경 (최솟값) — 중간 지점 기준 1km"
    );
}
