package com.safefood.setup.core;

import java.util.List;

/**
 * 테이블 정의와 기본 데이터. 기준 문서는 <b>SafeFood 데이터베이스 설계서 v2.0</b>의 부록 A(전체 DDL)입니다.
 *
 * <p>같은 내용이 세 곳에 있습니다. 하나만 고치면 다음 사람이 틀린 쪽을 믿게 되므로
 * <b>반드시 셋을 함께</b> 고쳐 주세요.
 * <ol>
 *   <li>이 파일 — 실제로 실행되는 코드</li>
 *   <li>README의 <b>데이터베이스 스키마</b> 절 — 사람이 읽는 문서</li>
 *   <li>데이터베이스 설계서 — 테이블 정의서와 부록 A</li>
 * </ol>
 *
 * <p>모든 DDL은 {@code IF NOT EXISTS}라서 마법사를 여러 번 실행해도 기존 데이터가 지워지지 않습니다.
 * 대신 <b>이미 만들어진 테이블의 구조는 바뀌지 않습니다.</b>
 * 스키마를 수정한 뒤 반영하려면 해당 테이블을 직접 DROP 하고 다시 실행하거나, ALTER로 직접 반영하세요.
 * (설계서 10.1절에 v1.0 → v2.0 ALTER 문이 있습니다)
 */
public final class Schema {

    private Schema() {
    }

    /** 외래 키 때문에 만드는 순서가 중요해서 List로 관리합니다. */
    public record Table(String name, String ddl) {
    }

    public static final List<Table> TABLES = List.of(

            // ── 회원 · 취향 · 알레르기 ──────────────────────

            new Table("user", """
                    CREATE TABLE IF NOT EXISTS `user` (
                        id          INT AUTO_INCREMENT PRIMARY KEY,
                        login_id    VARCHAR(30)  NOT NULL UNIQUE,
                        password    VARCHAR(255) NOT NULL,
                        nickname    VARCHAR(30)  NOT NULL,
                        created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("user_preference", """
                    CREATE TABLE IF NOT EXISTS user_preference (
                        user_id      INT NOT NULL,
                        spicy_level  TINYINT,
                        price_min    INT,
                        price_max    INT,
                        max_distance INT,
                        PRIMARY KEY (user_id),
                        FOREIGN KEY (user_id) REFERENCES `user`(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("user_category", """
                    CREATE TABLE IF NOT EXISTS user_category (
                        user_id  INT NOT NULL,
                        category VARCHAR(20) NOT NULL,
                        PRIMARY KEY (user_id, category),
                        FOREIGN KEY (user_id) REFERENCES `user`(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("allergy", """
                    CREATE TABLE IF NOT EXISTS allergy (
                        id   INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(30) NOT NULL UNIQUE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("user_allergy", """
                    CREATE TABLE IF NOT EXISTS user_allergy (
                        user_id    INT NOT NULL,
                        allergy_id INT NOT NULL,
                        severity   TINYINT NOT NULL DEFAULT 3,
                        PRIMARY KEY (user_id, allergy_id),
                        FOREIGN KEY (user_id)    REFERENCES `user`(id),
                        FOREIGN KEY (allergy_id) REFERENCES allergy(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            // ── 음식 사전 · 재료 (공공데이터) ──────────────

            new Table("food", """
                    CREATE TABLE IF NOT EXISTS food (
                        id       INT AUTO_INCREMENT PRIMARY KEY,
                        food_cd  VARCHAR(20) NOT NULL UNIQUE,
                        name     VARCHAR(100) NOT NULL,
                        category VARCHAR(50)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("ingredient", """
                    CREATE TABLE IF NOT EXISTS ingredient (
                        id   INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL UNIQUE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("food_ingredient", """
                    CREATE TABLE IF NOT EXISTS food_ingredient (
                        food_id       INT NOT NULL,
                        ingredient_id INT NOT NULL,
                        quantity      VARCHAR(50),
                        PRIMARY KEY (food_id, ingredient_id),
                        FOREIGN KEY (food_id)       REFERENCES food(id),
                        FOREIGN KEY (ingredient_id) REFERENCES ingredient(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("ingredient_allergy", """
                    CREATE TABLE IF NOT EXISTS ingredient_allergy (
                        ingredient_id INT NOT NULL,
                        allergy_id    INT NOT NULL,
                        PRIMARY KEY (ingredient_id, allergy_id),
                        FOREIGN KEY (ingredient_id) REFERENCES ingredient(id),
                        FOREIGN KEY (allergy_id)    REFERENCES allergy(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            // ── 맛집 · 메뉴 ────────────────────────────────

            new Table("restaurant", """
                    CREATE TABLE IF NOT EXISTS restaurant (
                        id         INT AUTO_INCREMENT PRIMARY KEY,
                        name       VARCHAR(50) NOT NULL,
                        category   VARCHAR(20) NOT NULL,
                        address    VARCHAR(255),
                        phone      VARCHAR(20),
                        open_time  TIME,
                        close_time TIME,
                        latitude   DECIMAL(10, 7),
                        longitude  DECIMAL(10, 7),
                        rating       DECIMAL(2, 1),
                        review_count INT DEFAULT 0
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("menu", """
                    CREATE TABLE IF NOT EXISTS menu (
                        id            INT AUTO_INCREMENT PRIMARY KEY,
                        restaurant_id INT NOT NULL,
                        name          VARCHAR(50) NOT NULL,
                        price         INT NOT NULL,
                        category      VARCHAR(20),
                        spicy_level   TINYINT,
                        description   VARCHAR(255),
                        food_id       INT,
                        FOREIGN KEY (restaurant_id) REFERENCES restaurant(id),
                        FOREIGN KEY (food_id)       REFERENCES food(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("menu_allergy", """
                    CREATE TABLE IF NOT EXISTS menu_allergy (
                        menu_id    INT NOT NULL,
                        allergy_id INT NOT NULL,
                        risk_level ENUM('CONTAINS', 'POSSIBLE', 'UNKNOWN') NOT NULL,
                        ingredient VARCHAR(100),
                        PRIMARY KEY (menu_id, allergy_id),
                        FOREIGN KEY (menu_id)    REFERENCES menu(id),
                        FOREIGN KEY (allergy_id) REFERENCES allergy(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            // ── 기분 태그 ──────────────────────────────────

            new Table("mood", """
                    CREATE TABLE IF NOT EXISTS mood (
                        id   INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(30) NOT NULL UNIQUE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("menu_mood", """
                    CREATE TABLE IF NOT EXISTS menu_mood (
                        menu_id INT NOT NULL,
                        mood_id INT NOT NULL,
                        PRIMARY KEY (menu_id, mood_id),
                        FOREIGN KEY (menu_id) REFERENCES menu(id),
                        FOREIGN KEY (mood_id) REFERENCES mood(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            // ── 그룹 추천 ──────────────────────────────────

            new Table("dining_group", """
                    CREATE TABLE IF NOT EXISTS dining_group (
                        id          INT AUTO_INCREMENT PRIMARY KEY,
                        name        VARCHAR(50),
                        owner_id    INT NOT NULL,
                        invite_code VARCHAR(10) NOT NULL UNIQUE,
                        status      ENUM('OPEN', 'VOTING', 'CLOSED') DEFAULT 'OPEN',
                        created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (owner_id) REFERENCES `user`(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("group_member", """
                    CREATE TABLE IF NOT EXISTS group_member (
                        id         INT AUTO_INCREMENT PRIMARY KEY,
                        group_id   INT NOT NULL,
                        user_id    INT,
                        guest_name VARCHAR(30),
                        latitude   DECIMAL(10, 7),
                        longitude  DECIMAL(10, 7),
                        joined_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (group_id) REFERENCES dining_group(id),
                        FOREIGN KEY (user_id)  REFERENCES `user`(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("group_member_allergy", """
                    CREATE TABLE IF NOT EXISTS group_member_allergy (
                        member_id  INT NOT NULL,
                        allergy_id INT NOT NULL,
                        PRIMARY KEY (member_id, allergy_id),
                        FOREIGN KEY (member_id)  REFERENCES group_member(id),
                        FOREIGN KEY (allergy_id) REFERENCES allergy(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("group_candidate", """
                    CREATE TABLE IF NOT EXISTS group_candidate (
                        id       INT AUTO_INCREMENT PRIMARY KEY,
                        group_id INT NOT NULL,
                        menu_id  INT NOT NULL,
                        score    INT,
                        reason   VARCHAR(255),
                        FOREIGN KEY (group_id) REFERENCES dining_group(id),
                        FOREIGN KEY (menu_id)  REFERENCES menu(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("group_vote", """
                    CREATE TABLE IF NOT EXISTS group_vote (
                        candidate_id INT NOT NULL,
                        member_id    INT NOT NULL,
                        voted_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (candidate_id, member_id),
                        FOREIGN KEY (candidate_id) REFERENCES group_candidate(id),
                        FOREIGN KEY (member_id)    REFERENCES group_member(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            // ── 기록 · 즐겨찾기 · 피드백 ──────────────────

            new Table("favorite", """
                    CREATE TABLE IF NOT EXISTS favorite (
                        id            INT AUTO_INCREMENT PRIMARY KEY,
                        user_id       INT NOT NULL,
                        restaurant_id INT,
                        menu_id       INT,
                        created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id)       REFERENCES `user`(id),
                        FOREIGN KEY (restaurant_id) REFERENCES restaurant(id),
                        FOREIGN KEY (menu_id)       REFERENCES menu(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("history", """
                    CREATE TABLE IF NOT EXISTS history (
                        id         INT AUTO_INCREMENT PRIMARY KEY,
                        user_id    INT NOT NULL,
                        menu_id    INT NOT NULL,
                        group_id   INT,
                        type       ENUM('RECOMMENDED', 'EATEN', 'VIEWED', 'BLOCKED') NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id)  REFERENCES `user`(id),
                        FOREIGN KEY (menu_id)  REFERENCES menu(id),
                        FOREIGN KEY (group_id) REFERENCES dining_group(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """),

            new Table("feedback", """
                    CREATE TABLE IF NOT EXISTS feedback (
                        id         INT AUTO_INCREMENT PRIMARY KEY,
                        user_id    INT NOT NULL,
                        menu_id    INT NOT NULL,
                        liked      BOOLEAN,
                        rating     TINYINT,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES `user`(id),
                        FOREIGN KEY (menu_id) REFERENCES menu(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """)
    );

    /**
     * 알레르기 마스터 데이터.
     * 식품의약품안전처가 고시한 알레르기 유발물질 표시 대상 품목을 기준으로 삼았습니다.
     */
    static final List<String> ALLERGIES = List.of(
            "우유", "계란", "메밀", "땅콩", "대두", "밀", "호두", "잣",
            "새우", "게", "오징어", "조개류", "고등어", "복숭아", "토마토",
            "돼지고기", "쇠고기", "닭고기", "아황산류"
    );

    /** 기분·컨디션 마스터 데이터. 추천 알고리즘의 기분 태그로 씁니다. */
    static final List<String> MOODS = List.of(
            "느끼함", "스트레스", "해장", "더움", "추움",
            "피곤함", "가벼운 식사", "든든하게", "혼밥", "기념일"
    );
}
