# Setup Wizard

> SafeFood **개발 환경 설정 마법사** — 저장소를 클론한 팀원이 한 번 실행하면 끝

MySQL 접속 확인부터 DB·테이블 생성, 기본 데이터 삽입, `config.properties` 작성까지
[README의 시작하기 3~4단계](../README.md#-시작하기)를 자동으로 처리합니다.

**MySQL을 쓰지 않는 경우**에는 DB 단계를 건너뛰고, 프로젝트 루트의 `data/` 폴더에
정보를 생성·저장하는 방식으로 진행할 수 있습니다. (아래 [데이터베이스 미사용](#-데이터베이스-미사용--data-폴더) 참고)

**여러 번 실행해도 안전합니다.** 이미 있는 DB·테이블·데이터·`data/` 파일은 건드리지 않고 건너뜁니다.

**화면은 두 가지입니다.** 창(JavaFX)과 콘솔 중 편한 쪽을 쓰면 되고, **하는 일과 결과는 같습니다.**

<br>

## 🚀 실행 방법

### 준비물

- JDK 21 이상 (`pom.xml`의 `maven.compiler.release` 기준)
- Maven 3.6 이상 — IntelliJ에 내장돼 있어 따로 설치하지 않아도 됩니다
- **(선택)** 실행 중인 MySQL 8.0 — DB를 쓸 때만 필요

> MySQL에 연결하지 못하면 DB 단계를 건너뜁니다.
> 이때는 **설정 파일**을 만들고, 앱 데이터는 **`data/` 폴더**에 생성·저장합니다.

> 📦 **JDBC 드라이버는 준비할 필요가 없습니다.** `pom.xml`에 의존성으로 들어 있어
> Maven이 자동으로 내려받고 클래스패스에 올려 줍니다.

### 창으로 실행 (권장)

```bash
mvn javafx:run -Pwizard-fx
```

단계 목록이 왼쪽에 계속 보이고, 입력을 **되돌아가서 고칠 수 있습니다.**
비밀번호도 가려집니다. 자세한 차이는 아래 [어느 화면을 쓸까](#-어느-화면을-쓸까)를 보세요.

### 콘솔로 실행

```bash
mvn compile exec:java -Pwizard
```

> **한글이 깨진다면** 실행 전에 `chcp 65001`을 한 번 실행하세요.

### IntelliJ에서

`main` 메서드 옆 ▶ 버튼을 누르면 됩니다.
(`pom.xml`을 `Open as Project`로 열어 Maven 프로젝트로 인식된 상태여야 합니다.)

| 화면 | 열 파일 |
|------|---------|
| 창 | `src/main/java/com/safefood/setup/SetupWizardFxLauncher.java` |
| 콘솔 | `src/main/java/com/safefood/setup/SetupWizard.java` |

> ⚠️ **창 화면은 `SetupWizardFx`가 아니라 `SetupWizardFxLauncher`를 실행해야 합니다.**
> `SetupWizardFx`를 직접 ▶로 실행하면 이렇게 거절당합니다.
>
> ```text
> 오류: 이 애플리케이션을 실행하는 데 필요한 JavaFX 런타임 구성요소가 누락되었습니다.
> ```
>
> JavaFX 11부터 JavaFX가 JDK에서 빠져서, JVM은 **메인 클래스가 `Application`을 상속하면**
> JavaFX가 *모듈 경로*에 있는지 검사합니다. 그런데 IntelliJ ▶ 버튼은 의존성을
> *클래스패스*에 올리기 때문에, jar가 분명히 있는데도 위 오류가 납니다.
> `SetupWizardFxLauncher`는 `Application`을 상속하지 않아 이 검사를 지나갑니다.
> (`mvn javafx:run -Pwizard-fx`는 플러그인이 모듈 경로를 붙여 주므로 어느 쪽이든 됩니다.)

<br>

## 🖥 어느 화면을 쓸까

| | 창 (JavaFX) | 콘솔 |
|--|-------------|------|
| 실행 | `mvn javafx:run -Pwizard-fx` | `mvn compile exec:java -Pwizard` |
| 입력 수정 | 언제든 [뒤로]로 돌아가 고칠 수 있음 | 지나가면 다시 실행해야 함 |
| 잘못된 입력 | 그 자리에서 빨간 글씨로 안내 | 같은 질문을 다시 물어봄 |
| 비밀번호 | 항상 가려짐 | IntelliJ 실행창에서는 그대로 보임 |
| 연결 테스트 | 버튼으로 여러 번, 창이 멈추지 않음 | 다음 단계로 넘어가며 1회 |
| 진행 상황 | 진행 막대 + 로그 | 텍스트 로그 |
| 원격 접속·CI | 화면이 없으면 못 씀 | 어디서나 됨 |

> 두 화면 모두 값을 모아 `SetupService`에 넘기는 구조라서 **결과가 달라질 일이 없습니다.**

<br>

## 📋 마법사가 하는 일

| 단계 | 내용 | 실패하면 |
|------|------|----------|
| 1 | JDBC 드라이버 확인 (Maven이 이미 준비) | `data/` 모드로만 진행할지 선택 |
| 2 | 접속 정보 입력 → 연결 테스트 | 원인을 한글로 안내하고 재입력 / DB 건너뛰기 |
| 3 | `CREATE DATABASE safefood` | 이후 DB 단계 건너뛰고 `data/` 모드로 진행 |
| 4 | 테이블 18개 생성 | 이후 DB 단계 건너뜀 |
| 5 | 알레르기·기분 태그 기본 데이터 삽입 | 건너뜀 (기본 목록은 6단계에서 `data/public/`에도 생성) |
| 6 | `data/public`·`data/private` 폴더와 기본 파일 준비 | 알려 주고 7단계는 계속 진행 |
| 7 | `config.properties` 작성 + `.gitignore` 확인 | — |

1·2단계는 사용자에게 묻는 부분이라 화면(`setup/`)이 맡고,
3~7단계는 두 화면이 공유하는 처리부(`setup/core/`)가 담당합니다.
자세한 구성은 아래 [파일 구성](#-파일-구성)을 보세요.

중간에 실패해도 **할 수 있는 데까지 진행하고**, 마지막에 무엇이 남았는지 알려 줍니다.
MySQL을 켠 뒤 다시 실행하면 남은 DB 준비를 이어서 합니다.

> 6단계는 **DB를 쓰든 안 쓰든 항상** 실행합니다.
> 나중에 MySQL을 끄고 파일 모드로 바꿔도 바로 쓸 수 있게 하기 위해서입니다.

### 만들어지는 파일

| 파일 | 커밋 | 설명 |
|------|------|------|
| `config.properties` | ❌ **금지** | 내 DB 계정·API 키가 들어 있음 (프로젝트 루트) |
| `config.properties.example` | ⭕ | 값이 빈 공유용 사본 |
| `config.properties.bak` | ❌ | 덮어쓰기 전 자동 백업 |
| `.gitignore` | ⭕ | `/config.properties`·`/config.properties.bak`·`data/private/` 항목이 없으면 자동 추가 |
| `data/public/` | ⭕ | 앱 동작에 필요한 공통 데이터 |
| `data/private/` | ❌ | 개인 데이터 · 팀 서버 데이터 |

> `config.properties.example`은 **항상 프로젝트 기본값**(`safefood`, `5000`)으로 씁니다.
> 팀이 공유하는 커밋 대상이라, 각자 고른 DB 이름·포트가 들어가면 실행할 때마다 충돌하기 때문입니다.
> 내용이 이미 같으면 아예 손대지 않습니다.

<br>

## 💾 데이터베이스 미사용 — `data/` 폴더

MySQL을 설치하지 않았거나, 드라이버·연결이 없어 DB 단계를 건너뛴 경우
앱이 쓰는 정보는 프로젝트 루트의 **`data/`** 폴더에 생성·저장합니다.

### 언제 쓰이나

- JDBC 드라이버가 없을 때
- MySQL 서버에 연결하지 못했을 때
- 팀원이 DB 없이 UI·소켓·추천 로직만 먼저 돌리고 싶을 때

> 폴더와 기본 파일은 **DB를 쓰더라도 미리 만들어 둡니다.**
> 나중에 MySQL을 끄고 파일 모드로 바꿔도 그대로 쓸 수 있게 하기 위해서입니다.

### 폴더 역할

| 폴더 | 들어가는 것 | 커밋 |
|------|-------------|------|
| `data/public/` | 앱이 **동작하는 데 필요한** 공통 데이터 (알레르기·기분 태그·메뉴 마스터 등) | ⭕ |
| `data/private/` | **개인 데이터**(회원·취향·즐겨찾기·내 히스토리) + **팀 서버 데이터**(방장 쪽 그룹·투표·접속 기록 등) | ❌ |

### 폴더·파일 구조

```
data/
├── public/                        # 앱에 필요한 공통 데이터 (커밋 가능)
│   ├── allergy.txt                # 알레르기 마스터 19종
│   ├── mood.txt                   # 기분 태그 마스터 10종
│   ├── restaurant.txt             # 가게 마스터 (선택)
│   └── menu.txt                   # 메뉴 마스터 (선택)
└── private/                       # 커밋 금지
    ├── users.txt                  # [개인] 회원·게스트
    ├── preferences.txt            # [개인] 취향·예산·거리
    ├── user_allergies.txt         # [개인] 보유 알레르기 + 심각도
    ├── categories.txt             # [개인] 선호 음식 종류
    ├── favorites.txt              # [개인] 즐겨찾기
    ├── history.txt                # [개인] 내 추천 히스토리
    ├── groups.txt                 # [팀 서버] 그룹·초대 코드
    ├── group_members.txt          # [팀 서버] 참여자·조건
    ├── group_member_allergies.txt # [팀 서버] 참여 시점 알레르기 스냅샷
    └── votes.txt                  # [팀 서버] 투표·결과
```

> 파일이 없으면 마법사(또는 앱 최초 실행)가 **기본 내용으로 새로 만듭니다.**
> 이미 있는 파일은 **덮어쓰지 않습니다.**

### 저장 규칙

| 항목 | 내용 |
|------|------|
| 위치 | 프로젝트 루트 `data/public/`, `data/private/` |
| 인코딩 | UTF-8 |
| 형식 | 텍스트 한 줄 = 한 레코드, 필드는 `\|`로 구분 |
| 커밋 | `public/`만 공유 · `private/`(개인·팀 서버)는 `.gitignore` |

> ⚠️ **주석 줄을 넣지 마세요.** 한 줄이 곧 한 레코드라서 `#`으로 시작하는 줄도
> 레코드로 읽힙니다. 그래서 마법사가 만드는 기본 파일에도 주석이 없습니다.

**필드 순서** — `core/Schema.java`의 테이블 컬럼 순서와 같습니다.
(데이터베이스 설계서의 테이블 정의서와도 같아야 합니다)

| 파일 | 대응 테이블 | 필드 |
|------|------------|------|
| `public/allergy.txt` | `allergy` | `name` |
| `public/mood.txt` | `mood` | `name` |
| `public/restaurant.txt` | `restaurant` | `id\|name\|category\|address\|phone\|open_time\|close_time\|latitude\|longitude\|rating\|review_count` |
| `public/menu.txt` | `menu` | `id\|restaurant_id\|name\|price\|category\|spicy_level\|description` |
| `private/users.txt` | `user` | `id\|login_id\|password\|nickname\|created_at` |
| `private/preferences.txt` | `user_preference` | `user_id\|spicy_level\|price_min\|price_max\|max_distance` |
| `private/user_allergies.txt` | `user_allergy` | `user_id\|allergy_id\|severity` |
| `private/categories.txt` | `user_category` | `user_id\|category` |
| `private/favorites.txt` | `favorite` | `id\|user_id\|restaurant_id\|menu_id\|created_at` |
| `private/history.txt` | `history` | `id\|user_id\|menu_id\|group_id\|type\|created_at` |
| `private/groups.txt` | `dining_group` | `id\|name\|owner_id\|invite_code\|status\|created_at` |
| `private/group_members.txt` | `group_member` | `id\|group_id\|user_id\|guest_name\|latitude\|longitude\|joined_at` |
| `private/group_member_allergies.txt` | `group_member_allergy` | `member_id\|allergy_id` |
| `private/votes.txt` | `group_vote` | `candidate_id\|member_id\|voted_at` |

ENUM 값도 DB와 같게 씁니다.

| 필드 | 허용 값 |
|------|---------|
| `history.type` | `RECOMMENDED` · `EATEN` · `VIEWED` · `BLOCKED` |
| `dining_group.status` | `OPEN` · `VOTING` · `CLOSED` |
| `user_allergy.severity` | `1`(거의 없음) ~ `5`(극도로 높음), 기본 `3` |

예시 (`data/public/allergy.txt` — 마법사가 만드는 기본 내용):

```text
우유
계란
메밀
땅콩
대두
밀
호두
잣
새우
게
오징어
조개류
고등어
복숭아
토마토
돼지고기
쇠고기
닭고기
아황산류
```

`restaurant.txt`·`menu.txt`와 `private/`의 파일들은 **빈 파일로** 만들어집니다.

### DB 모드와의 관계

| | MySQL 사용 | `data/` 사용 |
|--|------------|--------------|
| 준비 | MySQL + Connector/J | 폴더·파일만 있으면 됨 |
| 앱 공통 데이터 | DB에 INSERT | `data/public/`에 마스터 파일 생성 |
| 개인·팀 서버 데이터 | 테이블에 저장 | `data/private/`에 개인·그룹·투표 파일 저장 |
| 여러 기기 공유 | 공용 DB면 가능 | `public/`은 저장소로 공유, `private/`는 해당 PC만 |

나중에 MySQL을 켜고 마법사를 다시 실행하면 DB·테이블을 이어서 만들 수 있습니다.
`data/`에 쌓인 내용을 DB로 옮기는 마이그레이션은 **별도 작업**입니다.

<br>

## 🧩 파일 구성

**폴더가 곧 역할**입니다. 바깥은 화면, `core/`는 처리부입니다.

```
src/main/java/com/safefood/setup/
├── SetupWizard.java            # 콘솔 화면 (진입점)
├── SetupWizardFx.java          # 창(JavaFX) 화면
├── SetupWizardFxLauncher.java  # 창 화면의 IntelliJ ▶ 진입점
├── Ui.java                     # 콘솔 출력·입력 (기본값, y/n, 비밀번호 가리기)
└── core/                       # 화면 없이 도는 처리부
    ├── SetupService.java       # 3~7단계 실행. 두 화면이 함께 씁니다
    ├── SetupRequest.java       # 실행에 필요한 값 묶음 (화면 → 처리)
    ├── SetupResult.java        # 실행 결과 (처리 → 화면)
    ├── SetupReporter.java      # 진행 상황을 화면에 알리는 통로
    ├── DbConfig.java           # 접속 정보 + JDBC URL 조립
    ├── Schema.java             # 테이블 DDL과 기본 데이터
    ├── DatabaseInitializer.java   # DB·테이블 생성, 데이터 삽입, 오류 해석
    ├── DataStoreInitializer.java  # data/ 폴더·기본 파일 생성
    ├── ConfigFileWriter.java      # 설정 파일·.gitignore 처리
    ├── JdbcDriverSetup.java       # Maven을 안 쓸 때의 드라이버 확보
    └── package-info.java

src/main/resources/com/safefood/setup/
└── setup-wizard.css            # 창 화면 스타일
```

### 왜 나눴나

**의존 방향이 한쪽으로만 흐르게** 하기 위해서입니다.

```
com.safefood.setup (화면)  ──→  com.safefood.setup.core (처리)
```

`core/`는 화면을 알지 못합니다. `System.out`도 JavaFX도 쓰지 않고, 진행 상황은
`SetupReporter`로만 넘깁니다. 그래서 **콘솔판과 창판이 같은 코드를 그대로 씁니다.**
전에는 이 규칙이 "지키기로 한 약속"이었지만, 이제는 **패키지가 갈려 있어 컴파일러가 막아 줍니다.**

`core/`에서 화면 쪽을 import 하고 싶어졌다면 설계가 잘못된 신호입니다.
필요한 값은 `SetupRequest`에 담아 넘기고, 알릴 것은 `SetupReporter`로 보내세요.

### core/에서 열어 둔 것

화면이 직접 부르는 것만 `public`입니다.

| 클래스 | 화면에서 쓰는 곳 |
|--------|-----------------|
| `SetupService` | 3~7단계 실행 |
| `SetupRequest` · `SetupResult` | 값 전달 · 결과 받기 |
| `SetupReporter` | 화면이 구현해서 넘김 |
| `DbConfig` | 접속 정보 담기 |
| `DatabaseInitializer` | 연결 테스트(2단계), 오류 해석 |
| `ConfigFileWriter` | `findProjectRoot()`, `configPath()` |
| `JdbcDriverSetup` | 드라이버 확인(1단계) |

`Schema`와 `DataStoreInitializer`는 `core/` 안에서만 쓰므로 열어 두지 않았습니다.
**새 클래스를 추가할 때도 화면이 직접 부르는 것만 `public`으로 두세요.**

<br>

## ⚠️ 스키마를 고칠 때

같은 스키마가 **세 곳**에 있습니다. 하나만 고치면 다음 사람이 틀린 쪽을 믿게 됩니다.

| 위치 | 성격 |
|------|------|
| `setup/core/Schema.java` | 실제로 실행되는 코드 |
| README의 **데이터베이스 스키마** 절 | 사람이 읽는 문서 |
| 데이터베이스 설계서 (테이블 정의서 · 부록 A) | 설계 기준 |

마스터 목록(알레르기·기분 태그)은 `data/public/`의 기본 파일도 `Schema.java`에서 가져다 쓰므로
**따로 맞출 필요가 없습니다.** 다만 이미 만들어진 `allergy.txt`·`mood.txt`는 덮어쓰지 않으니,
목록을 바꿨다면 해당 파일을 지우고 마법사를 다시 실행하세요.

테이블 컬럼을 바꿨다면 `core/DataStoreInitializer.java`의 필드 순서 주석과
위 [저장 규칙](#저장-규칙)의 표도 같이 고쳐 주세요.

모든 DDL이 `CREATE TABLE IF NOT EXISTS`라서 **이미 만들어진 테이블의 구조는 바뀌지 않습니다.**
컬럼을 추가·변경했다면 직접 반영해야 합니다.

```sql
-- 방법 1) 해당 테이블만 지우고 마법사 재실행 (다른 테이블 데이터는 유지)
DROP TABLE menu_allergy;

-- 방법 2) 처음부터 다시 (⚠️ 데이터가 전부 사라집니다)
DROP DATABASE safefood;
```

> 마법사는 **어떤 것도 지우지 않습니다.** 삭제는 위처럼 직접 실행하세요.
> `data/private/`를 초기화하려면 해당 파일을 지운 뒤 마법사(또는 앱)를 다시 실행하면 됩니다.

<br>

## 🔄 이미 DB를 만든 팀원이라면 (v2.0 스키마)

데이터베이스 설계서 v2.0에서 컬럼 3개와 ENUM 값 1개가 늘었습니다.
**이전에 마법사를 돌려 테이블이 이미 있는 팀원**은 마법사를 다시 실행해도
`CREATE TABLE IF NOT EXISTS`가 건너뛰므로 새 컬럼이 생기지 않습니다.
아래를 한 번 실행해 주세요. (처음 설치하는 팀원은 그냥 마법사만 실행하면 됩니다)

```sql
USE safefood;

ALTER TABLE user_allergy
    ADD COLUMN severity TINYINT NOT NULL DEFAULT 3;

ALTER TABLE restaurant
    ADD COLUMN rating       DECIMAL(2,1) NULL,
    ADD COLUMN review_count INT NULL DEFAULT 0;

ALTER TABLE history
    MODIFY COLUMN type ENUM('RECOMMENDED', 'EATEN', 'VIEWED', 'BLOCKED') NOT NULL;
```

| 변경 | 왜 필요한가 |
|------|-------------|
| `user_allergy.severity` | 프로필 화면(SC-07)의 심각도 Class 1~5를 저장할 곳이 없었음 |
| `restaurant.rating` · `review_count` | 즐겨찾기 화면(SC-06-b)의 ★ 평점·리뷰 수를 저장할 곳이 없었음 |
| `history.type`에 `BLOCKED` | 기록 화면(SC-06)의 '차단됨' 상태에 대응하는 값이 없었음 |

내가 어느 쪽인지 확인하려면:

```sql
SHOW COLUMNS FROM user_allergy LIKE 'severity';
```

결과가 비어 있으면 위 ALTER를 실행해야 합니다.

> `data/` 파일 모드를 쓰고 있었다면 ALTER는 필요 없습니다.
> 다만 새로 생긴 `user_allergies.txt`·`categories.txt`·`group_member_allergies.txt`가
> 없으므로, 마법사를 한 번 더 실행하면 빈 파일로 만들어 줍니다.
> 기존 `restaurant.txt`에 데이터를 넣어 뒀다면 뒤에 `rating`·`review_count` 두 칸을 더해 주세요.

<br>

## 🔧 자주 나오는 문제

**`MySQL Connector/J를 클래스패스에서 찾지 못했습니다`**
드라이버 jar가 등록되지 않았습니다. DB를 쓸 거라면 위 [준비물](#준비물) 안내대로 추가하세요.
지금은 DB 없이 가도 된다면 설정 파일만 만들고, 데이터는 `data/`에 두세요.

**`계정 또는 비밀번호가 맞지 않습니다` (1045)**
MySQL 계정·비밀번호를 확인하세요. 설치할 때 정한 root 비밀번호입니다.
연결을 포기하면 DB 단계를 건너뛰고 `data/` 모드로 갈 수 있습니다.

**`MySQL 서버에 연결하지 못했습니다`**
서버가 꺼져 있거나 포트가 다릅니다. Windows는 `services.msc`에서 MySQL 서비스 상태를 확인하세요.
당장 MySQL이 필요 없다면 연결을 건너뛰고 `data/`에 정보를 저장하면 됩니다.

**`이 계정에는 해당 데이터베이스를 다룰 권한이 없습니다` (1044)**
DB를 만들 권한이 있는 계정(보통 root)으로 다시 실행하세요.

**`data/` 폴더가 없어요 / 파일이 비어 있어요**
마법사 또는 앱을 한 번 실행해 기본 파일을 만들게 하세요.
`private/`의 파일들은 원래 빈 파일로 만들어집니다. (아직 쌓인 데이터가 없어서 정상입니다)
프로젝트 루트(README가 있는 폴더)에서 실행했는지 확인하세요.

**비밀번호가 화면에 그대로 보여요**
IntelliJ 실행창은 `System.console()`을 지원하지 않아 가릴 수 없습니다.
터미널에서 실행하거나, **창 화면(`mvn javafx:run -Pwizard-fx`)을 쓰면** 항상 가려집니다.

**`오류: 이 애플리케이션을 실행하는 데 필요한 JavaFX 런타임 구성요소가 누락되었습니다`**
IntelliJ ▶ 버튼으로 `SetupWizardFx`를 직접 실행했을 때 납니다.
`SetupWizardFxLauncher`를 대신 실행하세요. (`mvn javafx:run -Pwizard-fx`도 됩니다)

JavaFX jar가 없어서가 아닙니다. JVM은 **메인 클래스가 `Application`을 상속하면**
JavaFX가 *모듈 경로*(`--module-path`)에 있는지 검사하는데, IntelliJ ▶ 버튼은
의존성을 *클래스패스*(`-classpath`)에 올리기 때문입니다.
`SetupWizardFxLauncher`는 `Application`을 상속하지 않아 이 검사를 지나갑니다.
앞으로 만들 앱 GUI(`view/`)에도 같은 이유로 시작 클래스가 하나 필요합니다.

**`Unsupported JavaFX configuration: classes were loaded from 'unnamed module'` 경고가 떠요**
클래스패스로 JavaFX를 쓸 때 나오는 정상적인 경고입니다. 그냥 두면 됩니다.
`mvn javafx:run -Pwizard-fx`로 실행하면 나오지 않습니다.

**창 화면에서 [설치 시작]을 눌렀는데 아무 반응이 없어요**
DB 작업은 백그라운드에서 돌아가 창이 멈추지 않습니다. 진행 막대와 로그를 확인하세요.
연결이 안 되는 주소를 넣으면 MySQL이 시간 초과될 때까지(수십 초) 기다릴 수 있습니다.
미리 [연결 테스트] 버튼으로 확인하는 편이 빠릅니다.

<br>

## 🔒 주의

- `config.properties`는 **절대 커밋하지 마세요.** 팀원과는 `.example` 파일로 공유합니다.
- `data/private/`는 **커밋하지 마세요.** 개인 데이터와 팀 서버 데이터가 들어갑니다.
- `data/public/`은 앱에 필요한 공통 데이터라 팀과 공유해도 됩니다.
- 실수로 커밋했다면 커밋만 지우는 것으로는 부족합니다. **비밀번호·API 키를 새로 발급**하세요.
- 생성되는 JDBC URL의 `useSSL=false`는 로컬 개발 전용입니다. 실제 서버에 올린다면 제거하세요.
