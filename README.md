# FoodMate 🍽️

> 오늘 뭐 먹지? 고민을 대신 해주는 **음식 추천** 서비스

Java로 개발하는 B팀 팀 프로젝트입니다.
사용자의 **취향·알레르기·기분·위치**를 바탕으로 알맞은 메뉴와 맛집을 추천하고,
추천 이유를 함께 설명해 주는 것을 목표로 합니다.
혼자일 때는 물론, **여러 명이 함께 먹을 때**도 모두가 안전하게 먹을 수 있는 메뉴를 찾아줍니다.

<br>

## 👥 팀 소개

**B팀**

| 이름 | 역할 | 담당 |
|------|------|------|
| 김민수 | 조장 | <!-- 담당 기능 작성 --> |
| 길동현 | 조원 | <!-- 담당 기능 작성 --> |
| 조영준 | 조원 | <!-- 담당 기능 작성 --> |

<br>

## 🛠️ 기술 스택

| 구분 | 사용 기술 |
|------|-----------|
| Language | Java <!-- 사용할 JDK 버전 확정 후 기입 (예: 17) --> |
| Database | MySQL 8.0 |
| DB 연동 | JDBC (MySQL Connector/J) |
| 실시간 통신 | Java Socket (TCP) — `ServerSocket` / `Socket`, 클라이언트별 스레드 |
| 지도 | Kakao Map API <!-- 또는 Naver Map API — 택 1 후 정리 --> |
| Build | <!-- Gradle / Maven / IDE 직접 실행 중 택 1 --> |
| IDE | IntelliJ IDEA |
| 협업 | Git, GitHub |

> ⚠️ **UI 형태 결정 필요**
> 지도 표시(#3)와 외부 지도 앱 연동(#11)은 **콘솔 앱으로는 구현이 어렵습니다.**
> 지도를 쓰려면 **웹(Spring Boot + HTML/JS)** 또는 **Swing/JavaFX GUI** 중 하나를 골라야 합니다.
> 결정 후 이 표와 [시작하기](#-시작하기)의 실행 방법을 갱신하세요.

<br>

## ✨ 주요 기능

> ⭐ 표시는 먼저 만들면 좋을 **핵심 기능(MVP)** 제안입니다. 팀 논의 후 조정하세요.

### 🔐 계정 / 온보딩

- [ ] ⭐ **로그인 / 회원가입**
- [ ] ⭐ **온보딩 설문** — 가입 직후 초기 취향·알레르기 입력
- [ ] ⭐ **취향 선택** — 선호 음식 종류, 매운맛 정도, 예산대
- [ ] ⭐ **알레르기 선택** — 보유 알레르기 등록
- [ ] **프로필 관리** — 등록한 취향·알레르기 수정

### 🎯 추천

- [ ] ⭐ **음식 추천 알고리즘** — 취향·알레르기·조건을 종합해 메뉴 선정
- [ ] ⭐ **추천 필터** — 거리 / 가격대 / 음식 종류 / 매운맛
- [ ] ⭐ **추천 이유 설명** — "왜 이 메뉴·이 가게인지" 근거 제시
- [ ] **기분·컨디션 기반 추천** — 예) 속이 느끼함 → 얼큰한 국물, 스트레스 → 매운 음식
- [ ] **재추천** — "다른 추천 보기"로 새 결과 받기
- [ ] **룰렛 추천** — 후보 몇 개를 뽑아 랜덤으로 하나 선택
- [ ] **전날 메뉴 중복 제외** — 어제 먹은 메뉴는 추천에서 제외
- [ ] **대체 메뉴 추천** — 알레르기·비선호로 걸러진 메뉴의 대안 제시

### 👥 그룹 추천 (여러 명이 함께 먹을 때)

> 참여 알림·투표 현황처럼 **여러 명에게 동시에 전달돼야 하는 부분**은
> [소켓 통신](#-소켓-통신-설계)으로 처리합니다.

- [ ] ⭐ **그룹 만들기 / 참여** — 방장이 그룹을 만들고 **초대 코드**로 일행을 초대
- [ ] **비회원(게스트) 참여** — 계정 없이 이름 + 알레르기·취향만 입력하고 참여
- [ ] ⭐ **조건 합치기** — 참여자 전원의 알레르기·취향·예산을 하나의 조건으로 병합
  ([병합 규칙](#그룹-추천-조건-병합-규칙) 참고)
- [ ] ⭐ **모두가 먹을 수 있는 메뉴 추천** — 한 명이라도 못 먹는 메뉴는 후보에서 제외
- [ ] **중간 지점 기준 검색** — 참여자들의 위치 중심으로 맛집 탐색
- [ ] **후보 투표** — 추천 후보 3~5개를 놓고 참여자가 투표해 최종 결정
- [ ] **그룹 추천 이유 설명** — "OO님 갑각류 알레르기 제외 / 전원 선호 한식 / 예산 1만 원 이하"
- [ ] **제외 사유 표시** — 특정 메뉴가 왜 빠졌는지, 누구의 조건 때문인지 안내
- [ ] **그룹 히스토리** — 이 그룹으로 먹었던 메뉴 기록 (다음에 중복 방지)

### 🔌 실시간 통신 (소켓)

- [ ] ⭐ **소켓 서버** — `ServerSocket`으로 다중 접속을 받고 클라이언트마다 스레드 배정
- [ ] ⭐ **소켓 클라이언트** — 접속 + 수신 전용 스레드 (입력 대기 중에도 서버 메시지 수신)
- [ ] ⭐ **그룹 방 관리** — 초대 코드 단위로 접속자를 묶고, 같은 그룹에만 브로드캐스트
- [ ] ⭐ **실시간 참여 알림** — 새 참여자가 들어오면 전원에게 "OO님 참여 (3명)" 즉시 전달
- [ ] ⭐ **실시간 투표** — 누가 투표하면 전원 화면의 득표 현황이 바로 갱신
- [ ] **최종 결과 브로드캐스트** — 확정된 메뉴·가게를 전원에게 동시에 안내
- [ ] **그룹 채팅** — 메뉴 정하는 동안 간단한 대화
- [ ] **접속 종료 / 끊김 처리** — 나가거나 강제 종료된 참여자를 감지해 목록에서 제거

### 🚨 알레르기 안전

- [ ] ⭐ **알레르기 ↔ 음식 매칭** — 등록한 알레르기가 포함된 메뉴 판별
- [ ] ⭐ **위험도 표시 및 경고** — 3단계로 구분해 안내
  - 🔴 **포함** — 해당 알레르기 원재료가 확실히 들어감
  - 🟡 **가능성 있음** — 조리 과정상 섞일 수 있음
  - ⚪ **확인 필요** — 원재료 정보 없음

### 🗺️ 지도 / 맛집

- [ ] ⭐ **지도 API 연동** — 주변 맛집을 지도에 표시
- [ ] ⭐ **맛집 상세** — 주소 / 영업시간 / 전화번호 / 메뉴 / 가격
- [ ] **길찾기** — 외부 지도 앱(카카오맵·네이버지도)으로 연결

### 📚 기록 / 개인화

- [ ] **즐겨찾기** — 찜한 가게·메뉴 저장
- [ ] **히스토리** — 먹은 기록, 최근 추천, 최근 본 항목
- [ ] **피드백** — 좋아요 / 싫어요, 만족도 평가 → 추천 정확도 개선

<br>

## 📁 프로젝트 구조

```
FoodMate/
├── src/
│   ├── Main.java                      # 진입점 (엔트리 포인트)
│   ├── config.properties              # 개인 설정 — 커밋 금지 (Setup Wizard가 생성)
│   ├── com/foodmate/
│   │   ├── model/                     # User, Menu, Restaurant, Allergy ...
│   │   ├── dao/                       # DB 접근 (JDBC) — SQL은 여기에서만
│   │   ├── service/                   # 비즈니스 로직
│   │   │   ├── AuthService            # 로그인 / 회원가입
│   │   │   ├── RecommendService       # 추천 알고리즘
│   │   │   ├── AllergyService         # 알레르기 매칭 / 위험도 판정
│   │   │   ├── GroupService           # 그룹 생성·참여, 조건 병합, 투표
│   │   │   └── MapService             # 지도 API 호출
│   │   ├── network/                   # 소켓 통신
│   │   │   ├── GroupServer            # ServerSocket, 접속 수락 (서버 진입점)
│   │   │   ├── ClientHandler          # 클라이언트 1명당 스레드
│   │   │   ├── RoomManager            # 그룹별 접속자 관리 / 브로드캐스트
│   │   │   ├── GroupClient            # 클라이언트 소켓 + 수신 스레드
│   │   │   └── Message                # 프로토콜 메시지 (타입 + 본문) 파싱·생성
│   │   └── view/                      # 화면 출력 / 입력 처리
│   └── SetupWizard/                   # 개발 환경 설정 마법사 (DB·설정 파일 자동 준비)
├── .gitignore
└── README.md
```

> 📦 **패키지(폴더)만 만들어 둔 상태입니다.**
> `service/`·`network/` 아래 클래스 이름은 **앞으로 만들 예정 목록**이라 아직 파일이 없습니다.
> 각 폴더의 `package-info.java`에 "여기엔 무엇을 넣는지"를 적어 뒀으니 작업 전에 한 번 읽어 보세요.

**계층 규칙** — 의존 방향은 `view → service → dao → DB` 한 방향으로만 흐르게 합니다.

| 계층 | 하는 일 | 하지 말 것 |
|------|---------|-----------|
| `view` | 출력·입력 | 판단 로직, SQL |
| `service` | 조건 판단, 추천·병합 계산 | `System.out` 직접 출력, SQL |
| `dao` | SQL 실행, 결과를 `model`로 변환 | service 호출 |
| `model` | 데이터 보관 | 계산 로직 |

<br>

## 🚀 시작하기

### 1. 요구 사항

- JDK <!-- 버전 --> 이상
- MySQL 8.0 이상
- MySQL Connector/J (JDBC 드라이버)
- 지도 API 키 (Kakao Developers 등에서 발급)
- 비어 있는 TCP 포트 1개 (소켓 서버용, 기본 `5000`)

### 2. 저장소 클론

```bash
git clone <저장소-주소>
cd FoodMate
```

> ⚡ **3~4단계는 [Setup Wizard](src/SetupWizard/SetupWizard.md)로 자동 처리할 수 있습니다.**
> IntelliJ에서 `src/SetupWizard/SetupWizard.java`를 열고 ▶ 버튼을 누르면
> DB·테이블 생성부터 `config.properties` 작성까지 한 번에 끝납니다.
> 아래 3~4단계는 **직접 하고 싶거나, 무엇이 만들어지는지 확인하고 싶을 때** 참고하세요.

### 3. 데이터베이스 준비

MySQL에 접속해 데이터베이스를 생성합니다.

```sql
CREATE DATABASE foodmate DEFAULT CHARACTER SET utf8mb4;
```

이후 [데이터베이스 스키마](#-데이터베이스-스키마)의 테이블 생성 쿼리를 실행합니다.

### 4. 설정 파일 작성

DB 계정과 API 키는 **소스 코드에 직접 적지 말고** 별도 설정 파일로 분리합니다.
(계정·키가 GitHub에 그대로 올라가는 것을 막기 위함)

`src/config.properties` 파일을 만들고 자신의 환경에 맞게 채웁니다.

```properties
db.url=jdbc:mysql://localhost:3306/foodmate?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
db.user=본인_계정
db.password=본인_비밀번호

map.api.key=발급받은_지도_API_키

# 소켓 서버 주소 (클라이언트가 접속할 곳)
socket.host=localhost
socket.port=5000
```

> ⚠️ `config.properties`는 `.gitignore`에 추가해 커밋되지 않도록 하세요.
> 팀원 공유용으로는 값을 비운 `config.properties.example`을 대신 올립니다.

### 5. 실행

```bash
# 컴파일
javac -encoding UTF-8 -d out src/Main.java

# 실행 (JDBC 드라이버 경로를 클래스패스에 포함)
java -cp "out;lib/mysql-connector-j.jar" Main
```

그룹 추천처럼 여러 명이 함께 쓰는 기능은 **서버를 먼저 켠 뒤** 클라이언트를 붙입니다.

```bash
# 1) 소켓 서버 실행 (먼저!)
java -cp "out;lib/mysql-connector-j.jar" GroupServer

# 2) 클라이언트 실행 — 참여자 수만큼 터미널을 여러 개 띄웁니다
java -cp "out;lib/mysql-connector-j.jar" Main
```

> IntelliJ에서는 `Main.java`의 `main` 메서드 옆 ▶ 버튼으로 바로 실행할 수 있습니다.
> 클라이언트를 **여러 개 동시에** 띄우려면 실행 구성(Run/Debug Configurations)에서
> `Modify options → Allow multiple instances`를 체크하세요.
> <!-- 웹/GUI로 결정되면 이 부분을 해당 실행 방법으로 교체하세요 -->

<br>

## 🗄️ 데이터베이스 스키마

<!-- 아래는 초안입니다. 기능 확정 후 팀에서 함께 수정하세요 -->

### 회원 · 취향 · 알레르기

```sql
-- 회원
CREATE TABLE user (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    login_id    VARCHAR(30)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,          -- 평문 저장 금지! 해시값 저장
    nickname    VARCHAR(30)  NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 취향 (회원당 1행)
CREATE TABLE user_preference (
    user_id     INT PRIMARY KEY,
    spicy_level TINYINT,                        -- 0(안 매움) ~ 5(아주 매움)
    price_min   INT,
    price_max   INT,
    max_distance INT,                           -- 최대 거리(m)
    FOREIGN KEY (user_id) REFERENCES user(id)
);

-- 선호 음식 종류 (회원당 여러 행)
CREATE TABLE user_category (
    user_id     INT NOT NULL,
    category    VARCHAR(20) NOT NULL,           -- 한식 / 중식 / 일식 / 양식 ...
    PRIMARY KEY (user_id, category),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

-- 알레르기 목록 (공통 마스터)
CREATE TABLE allergy (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE            -- 우유 / 땅콩 / 갑각류 / 계란 ...
);

-- 회원별 보유 알레르기
CREATE TABLE user_allergy (
    user_id     INT NOT NULL,
    allergy_id  INT NOT NULL,
    PRIMARY KEY (user_id, allergy_id),
    FOREIGN KEY (user_id)    REFERENCES user(id),
    FOREIGN KEY (allergy_id) REFERENCES allergy(id)
);
```

### 맛집 · 메뉴

```sql
-- 가게
CREATE TABLE restaurant (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    category    VARCHAR(20) NOT NULL,
    address     VARCHAR(255),
    phone       VARCHAR(20),
    open_time   TIME,
    close_time  TIME,
    latitude    DECIMAL(10, 7),                 -- 위도 (지도 표시용)
    longitude   DECIMAL(10, 7)                  -- 경도
);

-- 메뉴
CREATE TABLE menu (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id INT NOT NULL,
    name          VARCHAR(50) NOT NULL,
    price         INT NOT NULL,                 -- 가격 필터용
    category      VARCHAR(20),
    spicy_level   TINYINT,                      -- 0 ~ 5
    description   VARCHAR(255),
    FOREIGN KEY (restaurant_id) REFERENCES restaurant(id)
);

-- 메뉴 ↔ 알레르기 매칭 (위험도 포함)
CREATE TABLE menu_allergy (
    menu_id     INT NOT NULL,
    allergy_id  INT NOT NULL,
    risk_level  ENUM('CONTAINS', 'POSSIBLE', 'UNKNOWN') NOT NULL,
    PRIMARY KEY (menu_id, allergy_id),
    FOREIGN KEY (menu_id)    REFERENCES menu(id),
    FOREIGN KEY (allergy_id) REFERENCES allergy(id)
);
```

### 기분 태그

```sql
-- 기분 / 컨디션
CREATE TABLE mood (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE            -- 느끼함 / 스트레스 / 해장 / 더움 ...
);

-- 기분에 어울리는 메뉴 연결
CREATE TABLE menu_mood (
    menu_id INT NOT NULL,
    mood_id INT NOT NULL,
    PRIMARY KEY (menu_id, mood_id),
    FOREIGN KEY (menu_id) REFERENCES menu(id),
    FOREIGN KEY (mood_id) REFERENCES mood(id)
);
```

### 그룹 추천

```sql
-- 모임 그룹
CREATE TABLE dining_group (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50),                    -- 예) 금요일 점심팀
    owner_id    INT NOT NULL,                   -- 방장
    invite_code VARCHAR(10) NOT NULL UNIQUE,    -- 초대 코드
    status      ENUM('OPEN', 'VOTING', 'CLOSED') DEFAULT 'OPEN',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES user(id)
);

-- 그룹 참여자
-- 회원은 user_id, 비회원은 guest_name 사용 (둘 중 하나만 채움)
CREATE TABLE group_member (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    group_id   INT NOT NULL,
    user_id    INT,
    guest_name VARCHAR(30),
    latitude   DECIMAL(10, 7),                  -- 참여자 위치 (중간 지점 계산용)
    longitude  DECIMAL(10, 7),
    joined_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES dining_group(id),
    FOREIGN KEY (user_id)  REFERENCES user(id)
);

-- 참여자별 알레르기 (참여 시점 스냅샷)
-- 회원은 user_allergy에서 복사, 비회원은 직접 입력
CREATE TABLE group_member_allergy (
    member_id  INT NOT NULL,
    allergy_id INT NOT NULL,
    PRIMARY KEY (member_id, allergy_id),
    FOREIGN KEY (member_id)  REFERENCES group_member(id),
    FOREIGN KEY (allergy_id) REFERENCES allergy(id)
);

-- 그룹 추천 후보
CREATE TABLE group_candidate (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,
    menu_id  INT NOT NULL,
    score    INT,                               -- 추천 점수
    reason   VARCHAR(255),                      -- 추천 이유
    FOREIGN KEY (group_id) REFERENCES dining_group(id),
    FOREIGN KEY (menu_id)  REFERENCES menu(id)
);

-- 후보 투표 (참여자당 후보 1표)
CREATE TABLE group_vote (
    candidate_id INT NOT NULL,
    member_id    INT NOT NULL,
    voted_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (candidate_id, member_id),
    FOREIGN KEY (candidate_id) REFERENCES group_candidate(id),
    FOREIGN KEY (member_id)    REFERENCES group_member(id)
);
```

### 기록 · 즐겨찾기 · 피드백

```sql
-- 즐겨찾기
CREATE TABLE favorite (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    restaurant_id INT,
    menu_id       INT,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)       REFERENCES user(id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurant(id),
    FOREIGN KEY (menu_id)       REFERENCES menu(id)
);

-- 히스토리 (추천 / 먹음 / 조회)
-- 'EATEN' 기록으로 전날 먹은 메뉴 중복 제외를 판정합니다
CREATE TABLE history (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    menu_id    INT NOT NULL,
    group_id   INT,                             -- 그룹으로 먹은 경우 (개인 추천이면 NULL)
    type       ENUM('RECOMMENDED', 'EATEN', 'VIEWED') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)  REFERENCES user(id),
    FOREIGN KEY (menu_id)  REFERENCES menu(id),
    FOREIGN KEY (group_id) REFERENCES dining_group(id)
);

-- 피드백
CREATE TABLE feedback (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    menu_id    INT NOT NULL,
    liked      BOOLEAN,                         -- 좋아요 / 싫어요
    rating     TINYINT,                         -- 만족도 1 ~ 5
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (menu_id) REFERENCES menu(id)
);
```

<br>

## 🧠 추천 알고리즘 구상

<!-- 실제 로직이 정해지면 구체적으로 채우세요 -->

### 개인 추천

1. **후보 수집** — 현재 위치 기준 `max_distance` 이내 가게의 메뉴를 모두 가져옴
2. **안전 필터** — 등록 알레르기가 `CONTAINS`인 메뉴 제외, `POSSIBLE`은 경고 표시 후 유지
3. **조건 필터** — 가격대 / 음식 종류 / 매운맛 / 거리
4. **중복 제거** — 어제 `EATEN` 기록이 있는 메뉴 제외
5. **점수 계산** — 선호 카테고리 일치, 기분 태그 일치, 과거 피드백(좋아요/싫어요) 반영
6. **결과 반환** — 상위 후보 중 선택, 점수 근거를 "추천 이유"로 함께 출력

### 그룹 추천 조건 병합 규칙

참여자 전원의 조건을 **하나의 조건으로 합친 뒤**, 위 개인 추천 흐름을 그대로 태웁니다.
핵심은 "**제약은 가장 빡빡한 사람에게 맞추고, 취향은 최대한 겹치게**"입니다.

| 조건 | 병합 방식 | 이유 |
|------|-----------|------|
| **알레르기** | **합집합** — 전원의 알레르기를 모두 제외 | 안전 최우선. 한 명이라도 못 먹으면 후보에서 뺌 |
| **매운맛** | **최솟값** — 가장 맵찔이 기준 | 못 먹는 사람이 생기지 않도록 |
| **예산** | **교집합** — `max(하한) ~ min(상한)` | 전원이 감당 가능한 구간 |
| **선호 카테고리** | **교집합** 우선, 없으면 **득표순** | 겹치는 취향이 없을 수도 있음 |
| **위치** | 참여자 좌표의 **중간 지점** | 모두가 비슷한 거리를 이동 |
| **탐색 반경** | **최솟값** | 가장 멀리 못 가는 사람 기준 |
| **전날 메뉴** | **합집합** — 누구든 어제 먹었으면 제외 | |

**예외 처리**

- 예산 구간이 겹치지 않으면 → 경고 후 `min(상한)` 기준으로 진행
- 알레르기를 다 빼고 나니 후보가 0개면 → 조건 완화 안내
  (`POSSIBLE` 등급까지 허용할지 방장이 선택 → 단, 경고 문구 필수 노출)
- 참여자가 1명이면 → 개인 추천과 동일하게 동작

**진행 흐름**

```
방장이 그룹 생성 → 초대 코드 공유 → 일행 참여(취향·알레르기 등록)
   → 조건 병합 → 후보 3~5개 추천 → 투표 → 최종 메뉴 확정 → 그룹 히스토리 저장
```

<br>

## 🔌 소켓 통신 설계

<!-- 초안입니다. 구현하면서 팀에서 함께 다듬으세요 -->

그룹 추천은 여러 명이 **동시에** 참여하고 투표하기 때문에, DB만으로는 "지금 누가 들어왔는지",
"몇 표가 들어왔는지"를 바로 알 수 없습니다. 그래서 그룹 기능은 **TCP 소켓으로 연결을 유지**합니다.

- **소켓** = 실시간 전달 (참여 알림, 투표 현황, 채팅, 최종 결과)
- **DB** = 기록 보관 (참여자, 후보, 투표 결과, 히스토리)

### 구조

```
   [클라이언트 A] ──┐
   [클라이언트 B] ──┼──▶  GroupServer (TCP :5000)  ──▶  MySQL
   [클라이언트 C] ──┘        └ ClientHandler 스레드 × N
```

- **서버** — `ServerSocket.accept()`로 접속을 받고, 클라이언트 1명당 **스레드 1개**(`ClientHandler`)를 배정
- **클라이언트** — 접속 후 **수신 전용 스레드**를 따로 띄움
  (사용자 입력을 기다리는 동안에도 서버가 보낸 메시지를 받아야 하므로)
- **그룹 = 방(room)** — 초대 코드를 키로 접속자 목록을 관리하고, **같은 그룹에만** 브로드캐스트

### 메시지 프로토콜

한 줄 = 한 메시지, `타입|본문` 형식입니다. (구분자 `|`)

| 방향 | 타입 | 예시 | 설명 |
|------|------|------|------|
| C→S | `JOIN` | `JOIN\|ABC123\|홍길동` | 초대 코드로 그룹 참여 |
| C→S | `INFO` | `INFO\|땅콩,갑각류\|2\|10000` | 알레르기 / 매운맛 / 예산 전달 |
| C→S | `READY` | `READY` | 조건 입력 완료 (전원 완료 시 추천 시작) |
| C→S | `VOTE` | `VOTE\|2` | 후보 번호에 투표 |
| C→S | `CHAT` | `CHAT\|짜장면 어때?` | 그룹 채팅 |
| C→S | `EXIT` | `EXIT` | 나가기 |
| S→C | `JOINED` | `JOINED\|홍길동\|3` | 참여 알림 + 현재 인원 |
| S→C | `CANDIDATES` | `CANDIDATES\|1.마라탕,2.순대국,3.돈까스` | 추천 후보 목록 |
| S→C | `VOTE_STATUS` | `VOTE_STATUS\|1:2,2:1,3:0` | 실시간 득표 현황 |
| S→C | `RESULT` | `RESULT\|마라탕\|OO식당` | 최종 확정 메뉴 |
| S→C | `LEFT` | `LEFT\|홍길동\|2` | 나감/끊김 알림 + 남은 인원 |
| S→C | `ERROR` | `ERROR\|존재하지 않는 초대 코드` | 오류 안내 |

> 문자열 파싱이 번거로우면 JSON이나 `ObjectOutputStream`(객체 직렬화)도 대안입니다.
> 다만 콘솔에서 눈으로 확인하며 디버깅하기엔 위 텍스트 방식이 가장 편합니다.

### 진행 흐름

```
방장                       서버                      참여자
 │ ── JOIN(방 생성) ───────▶│
 │                          │◀───────── JOIN(초대 코드) ──│
 │◀── JOINED(참여 알림) ────│───────────▶ JOINED ────────▶│
 │                          │◀───────── INFO / READY ─────│
 │          (전원 READY → 조건 병합 → 추천 실행)           │
 │◀── CANDIDATES ───────────│───────────▶ CANDIDATES ────▶│
 │ ── VOTE ────────────────▶│◀───────── VOTE ─────────────│
 │◀── VOTE_STATUS ──────────│───────────▶ VOTE_STATUS ───▶│
 │◀── RESULT ───────────────│───────────▶ RESULT ────────▶│
                  (그룹 히스토리를 DB에 저장)
```

### 구현 시 주의할 점

- **동시 접근** — 접속자 목록·투표 집계는 여러 스레드가 같이 건드립니다.
  `ConcurrentHashMap`을 쓰거나 `synchronized`로 보호하세요.
- **자원 정리** — 클라이언트가 나가면 스트림·소켓을 닫고 접속자 목록에서도 반드시 제거
- **비정상 종료** — 창을 그냥 닫은 경우 `IOException`으로 감지해 `LEFT`로 처리
- **한글 깨짐** — 스트림을 만들 때 인코딩을 명시
  (`new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)`)
- **포트 충돌** — 포트는 `config.properties`에서 읽어 팀원마다 바꿀 수 있게 합니다
- **테스트** — 같은 PC에서 서버 1개 + 클라이언트 여러 개를 띄워 `localhost`로 확인

<br>

## 🌿 협업 규칙

### 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | 최종 완성 코드 (직접 커밋 금지) |
| `dev` | 개발 통합 브랜치 |
| `feature/기능명` | 기능별 작업 브랜치 |

작업 흐름: `dev`에서 `feature/...` 브랜치를 만들어 작업 → Pull Request → 리뷰 후 `dev`에 병합

### 커밋 메시지 규칙

```
[타입] 작업 내용

예시)
[feat] 알레르기 위험도 판정 기능 추가
[fix] 추천 기록 저장 시 NPE 수정
[docs] README 기능 목록 갱신
```

| 타입 | 의미 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 코드 리팩터링 |
| `docs` | 문서 수정 |
| `chore` | 설정, 빌드 등 기타 작업 |

<br>

## 📌 진행 상황

- [x] 프로젝트 생성 및 저장소 세팅
- [x] 기능 목록 정리
- [x] 개발 환경 설정 마법사 작성 (`src/SetupWizard`)
- [x] 패키지 구조 만들기 (`com.foodmate`)
- [ ] 애플리케이션 형태(웹 / GUI) 결정
- [ ] 기능 우선순위 확정 (MVP 범위 정하기)
- [ ] DB 스키마 확정 및 테이블 생성
- [ ] 소켓 메시지 프로토콜 확정
- [ ] 역할 분담
- [ ] 기능 구현
- [ ] 발표 자료 준비
