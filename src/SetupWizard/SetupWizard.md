# Setup Wizard

> FoodMate **개발 환경 설정 마법사** — 저장소를 클론한 팀원이 한 번 실행하면 끝

MySQL 접속 확인부터 DB·테이블 생성, 기본 데이터 삽입, `config.properties` 작성까지
[README의 시작하기 3~4단계](../../README.md#-시작하기)를 자동으로 처리합니다.

**여러 번 실행해도 안전합니다.** 이미 있는 DB·테이블·데이터는 건드리지 않고 건너뜁니다.

<br>

## 🚀 실행 방법

### 준비물

- JDK 21 이상(프로젝트 설정 기준)
- 실행 중인 MySQL 8.0
- MySQL Connector/J (JDBC 드라이버)

> 드라이버가 없어도 실행은 됩니다. DB 단계를 건너뛰고 **설정 파일만** 만들지 물어봅니다.

### IntelliJ에서 (권장)

`SetupWizard.java`를 열고 `main` 메서드 옆 ▶ 버튼을 누르면 됩니다.

Connector/J가 아직 없다면 먼저 등록하세요.
`File → Project Structure → Libraries → + → Java →` 내려받은 `mysql-connector-j-x.x.x.jar` 선택

### 터미널에서

```bash
# 컴파일
javac -encoding UTF-8 -d out src/SetupWizard/*.java

# 실행 (Windows — 클래스패스 구분자는 세미콜론)
java -Dstdout.encoding=UTF-8 -cp "out;lib/mysql-connector-j.jar" SetupWizard.SetupWizard
```

> **한글이 깨진다면** 위처럼 `-Dstdout.encoding=UTF-8`을 붙이거나, 실행 전에 `chcp 65001`을 한 번 실행하세요.
> macOS·Linux는 클래스패스 구분자를 `:`로 바꿔 주세요. (`"out:lib/mysql-connector-j.jar"`)

<br>

## 📋 마법사가 하는 일

| 단계 | 내용 | 실패하면 |
|------|------|----------|
| 1 | JDBC 드라이버 확인 | 설치 방법 안내 후, 설정 파일만 만들지 선택 |
| 2 | 접속 정보 입력 → 연결 테스트 | 원인을 한글로 안내하고 재입력 |
| 3 | `CREATE DATABASE foodmate` | 이후 단계 건너뜀 |
| 4 | 테이블 18개 생성 | 이후 단계 건너뜀 |
| 5 | 알레르기·기분 태그 기본 데이터 삽입 | 건너뜀 |
| 6 | `src/config.properties` 작성 | — |

중간에 실패해도 **할 수 있는 데까지 진행하고**, 마지막에 무엇이 남았는지 알려 줍니다.
MySQL을 켠 뒤 다시 실행하면 남은 준비를 이어서 합니다.

### 만들어지는 파일

| 파일 | 커밋 | 설명 |
|------|------|------|
| `src/config.properties` | ❌ **금지** | 내 DB 계정·API 키가 들어 있음 |
| `src/config.properties.example` | ⭕ | 값이 빈 공유용 사본 |
| `src/config.properties.bak` | ❌ | 덮어쓰기 전 자동 백업 |
| `.gitignore` | ⭕ | `src/config.properties` 항목이 없으면 자동 추가 |

<br>

## 🧩 파일 구성

| 파일 | 역할 |
|------|------|
| `SetupWizard.java` | 진입점. 6단계 흐름 제어 |
| `Ui.java` | 콘솔 출력·입력 (기본값, y/n, 비밀번호 가리기) |
| `DbConfig.java` | 접속 정보 + JDBC URL 조립 |
| `Schema.java` | 테이블 DDL과 기본 데이터 |
| `DatabaseInitializer.java` | DB·테이블 생성, 데이터 삽입, 오류 해석 |
| `ConfigFileWriter.java` | 설정 파일·`.gitignore` 처리 |

<br>

## ⚠️ 스키마를 고칠 때

`Schema.java`와 README의 데이터베이스 스키마 절을 **둘 다** 고쳐 주세요.
(README는 사람이 읽는 문서, `Schema.java`는 실제로 실행되는 코드)

모든 DDL이 `CREATE TABLE IF NOT EXISTS`라서 **이미 만들어진 테이블의 구조는 바뀌지 않습니다.**
컬럼을 추가·변경했다면 직접 반영해야 합니다.

```sql
-- 방법 1) 해당 테이블만 지우고 마법사 재실행 (다른 테이블 데이터는 유지)
DROP TABLE menu_allergy;

-- 방법 2) 처음부터 다시 (⚠️ 데이터가 전부 사라집니다)
DROP DATABASE foodmate;
```

> 마법사는 **어떤 것도 지우지 않습니다.** 삭제는 위처럼 직접 실행하세요.

<br>

## 🔧 자주 나오는 문제

**`MySQL Connector/J를 클래스패스에서 찾지 못했습니다`**
드라이버 jar가 등록되지 않았습니다. 위 [준비물](#준비물) 안내대로 추가하세요.

**`계정 또는 비밀번호가 맞지 않습니다` (1045)**
MySQL 계정·비밀번호를 확인하세요. 설치할 때 정한 root 비밀번호입니다.

**`MySQL 서버에 연결하지 못했습니다`**
서버가 꺼져 있거나 포트가 다릅니다. Windows는 `services.msc`에서 MySQL 서비스 상태를 확인하세요.

**`이 계정에는 해당 데이터베이스를 다룰 권한이 없습니다` (1044)**
DB를 만들 권한이 있는 계정(보통 root)으로 다시 실행하세요.

**비밀번호가 화면에 그대로 보여요**
IntelliJ 실행창은 `System.console()`을 지원하지 않아 가릴 수 없습니다.
가리고 싶다면 터미널에서 실행하세요.

<br>

## 🔒 주의

- `config.properties`는 **절대 커밋하지 마세요.** 팀원과는 `.example` 파일로 공유합니다.
- 실수로 커밋했다면 커밋만 지우는 것으로는 부족합니다. **비밀번호·API 키를 새로 발급**하세요.
- 생성되는 JDBC URL의 `useSSL=false`는 로컬 개발 전용입니다. 실제 서버에 올린다면 제거하세요.
