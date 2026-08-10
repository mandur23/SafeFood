package com.safefood.setup;

import com.safefood.setup.core.ConfigFileWriter;
import com.safefood.setup.core.DatabaseInitializer;
import com.safefood.setup.core.DbConfig;
import com.safefood.setup.core.JdbcDriverSetup;
import com.safefood.setup.core.SetupRequest;
import com.safefood.setup.core.SetupResult;
import com.safefood.setup.core.SetupService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * SafeFood 개발 환경 설정 마법사 — <b>콘솔 화면</b>.
 *
 * <p>저장소를 클론한 팀원이 <b>한 번 실행</b>하면 아래 작업을 순서대로 처리합니다.
 * <ol>
 *   <li>JDBC 드라이버 확인 (Maven이 이미 올려 두므로 보통 바로 통과)</li>
 *   <li>MySQL 접속 정보 입력 및 연결 테스트</li>
 *   <li>데이터베이스 생성</li>
 *   <li>테이블 생성</li>
 *   <li>기본 데이터(알레르기·기분 태그) 삽입</li>
 *   <li>{@code data/} 폴더 준비 (DB를 쓰지 않을 때의 저장소)</li>
 *   <li>config.properties 작성 (프로젝트 루트)</li>
 * </ol>
 *
 * <p>이 클래스는 <b>1·2단계(사용자에게 묻는 부분)</b>와 값 수집만 맡습니다.
 * 실제 처리(3~7단계)는 {@link SetupService}에 있고, 창으로 된 화면
 * {@link SetupWizardFx}도 같은 클래스를 씁니다. 그래서 두 화면의 결과가 항상 같습니다.
 *
 * <p>중간에 실패해도 되는 만큼은 진행하고, 무엇이 남았는지 마지막에 알려 줍니다.
 * 여러 번 실행해도 기존 데이터는 지워지지 않습니다.
 *
 * <p>실행: {@code mvn compile exec:java -Pwizard} — 자세한 설명은 {@code docs/SetupWizard.md} 참고.
 */
public final class SetupWizard {

    private static final int TOTAL_STEPS = SetupService.TOTAL_STEPS;
    private static final String DEFAULT_DATABASE = "safefood";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_MYSQL_PORT = 3306;
    private static final int DEFAULT_SOCKET_PORT = 5000;

    private SetupWizard() {

    }

    public static void main(String[] args) {
        Ui.banner();
        try {
            run();
        } catch (IOException e) {
            Ui.blank();
            Ui.fail("파일을 쓰지 못했습니다: " + e.getMessage());
            Ui.info("프로젝트 폴더에 쓰기 권한이 있는지 확인해 주세요.");
        } catch (RuntimeException e) {
            Ui.blank();
            Ui.fail("예기치 못한 문제로 중단됐습니다: " + e);
            Ui.info("이 메시지를 팀 채널에 공유하면 함께 원인을 찾을 수 있습니다.");
        } finally {
            Ui.close();
        }
    }

    private static void run() throws IOException {
        Path projectRoot = ConfigFileWriter.findProjectRoot();
        Ui.blank();
        Ui.info("프로젝트 경로: " + projectRoot);

        // [1/7] 없으면 lib/에 받고 런타임에 로드. 실패해도 설정 파일·data/는 만들 수 있습니다.
        DriverState driverState = checkDriver(projectRoot);
        if (driverState == DriverState.ABORT) {
            Ui.blank();
            Ui.info("마법사를 종료합니다. 네트워크를 확인하거나 jar를 lib/에 넣은 뒤 다시 실행해 주세요.");
            return;
        }

        // [2/7]
        Ui.step(2, TOTAL_STEPS, "MySQL 접속 정보");
        Connection connection = askAndConnect(driverState == DriverState.READY);

        // 3~7단계는 도중에 묻지 않으므로, 남은 값은 여기서 미리 모읍니다.
        SetupRequest request = askRemaining(projectRoot, connection);

        SetupResult result = new SetupService(Ui.reporter()).run(request);
        summary(request, result);
    }

    // ── [1/7] 드라이버 ───────────────────────────────────

    private enum DriverState {
        /** 드라이버가 있어 DB 작업까지 진행 가능 */
        READY,
        /** 드라이버는 없지만 설정 파일·data/만 만들기로 함 */
        CONFIG_ONLY,
        /** 사용자가 종료를 선택 */
        ABORT
    }

    private static DriverState checkDriver(Path projectRoot) {
        Ui.step(1, TOTAL_STEPS, "JDBC 드라이버 확인");
        if (JdbcDriverSetup.ensureReady(projectRoot, Ui.reporter())) {
            return DriverState.READY;
        }

        Ui.blank();
        Ui.warn("자동 설치에 실패했습니다. 수동으로 lib/에 jar를 넣거나 네트워크를 확인하세요.");
        Ui.detail("https://dev.mysql.com/downloads/connector/j/ (Platform Independent)");
        Ui.blank();

        boolean configOnly = Ui.confirm("지금은 설정 파일과 data/ 폴더만 준비할까요? (n = 마법사 종료)", true);
        return configOnly ? DriverState.CONFIG_ONLY : DriverState.ABORT;
    }

    // ── [2/7] 접속 정보 ──────────────────────────────────

    /** 입력받은 접속 정보와, 실제로 연결에 성공했는지 여부 */
    private record Connection(DbConfig config, boolean connected) {
    }

    private static Connection askAndConnect(boolean testConnection) {
        DbConfig previous = null;
        while (true) {
            // 재입력일 때는 직전 값을 기본값으로 보여 줍니다. 보통 비밀번호만 틀리기 때문입니다.
            DbConfig config = askDbConfig(previous);
            previous = config;

            if (!testConnection) {
                Ui.warn("드라이버가 없어 연결 테스트는 건너뜁니다.");
                return new Connection(config, false);
            }

            Ui.info("연결을 확인하는 중...");
            try {
                new DatabaseInitializer(config, Ui.reporter()).testConnection();
                Ui.ok("연결 성공 — " + config);
                return new Connection(config, true);
            } catch (SQLException e) {
                Ui.fail("연결 실패 — " + DatabaseInitializer.explain(e));
                if (!Ui.confirm("접속 정보를 다시 입력할까요?", true)) {
                    Ui.warn("데이터베이스 단계는 건너뛰고, data/ 폴더와 설정 파일만 준비합니다.");
                    return new Connection(config, false);
                }
            }
        }
    }

    /** @param previous 직전 입력값. 첫 시도라 없으면 null */
    private static DbConfig askDbConfig(DbConfig previous) {
        boolean retry = previous != null;
        String host = Ui.ask("MySQL 호스트", retry ? previous.host() : DEFAULT_HOST);
        int port = Ui.askPort("MySQL 포트", retry ? previous.port() : DEFAULT_MYSQL_PORT);
        String database = Ui.askIdentifier("데이터베이스 이름", retry ? previous.database() : DEFAULT_DATABASE);
        String user = Ui.ask("MySQL 계정", retry ? previous.user() : "root");

        String password = Ui.askPassword(retry ? "MySQL 비밀번호 (Enter = 직전 값 유지)" : "MySQL 비밀번호");
        if (retry && password.isEmpty()) {
            password = previous.password();
        }
        return new DbConfig(host, port, database, user, password);
    }

    // ── 나머지 입력값 ────────────────────────────────────

    /** 3~7단계 실행에 필요한 값을 모두 모아 둡니다. (실행 중에는 다시 묻지 않습니다) */
    private static SetupRequest askRemaining(Path projectRoot, Connection connection) {
        Ui.section("앱 설정");
        String mapApiKey = Ui.askOptional("지도 API 키");
        String socketHost = Ui.ask("소켓 서버 호스트", DEFAULT_HOST);
        int socketPort = Ui.askPort("소켓 서버 포트", DEFAULT_SOCKET_PORT);

        boolean overwriteConfig = false;
        if (Files.exists(ConfigFileWriter.configPath(projectRoot))) {
            Ui.blank();
            Ui.warn("config.properties 파일이 이미 있습니다.");
            overwriteConfig = Ui.confirm("덮어쓸까요? (기존 파일은 .bak으로 백업합니다)", false);
        }

        return new SetupRequest(projectRoot, connection.config(), connection.connected(),
                mapApiKey, socketHost, socketPort, overwriteConfig);
    }

    // ── 마무리 ───────────────────────────────────────────

    private static void summary(SetupRequest request, SetupResult result) {
        Ui.section("설정 결과");
        Ui.info((result.databaseDone() ? "[완료] " : "[미완] ")
                + "데이터베이스 " + request.db().database() + " (테이블 + 기본 데이터)");
        Ui.info("[완료] data/ 폴더"
                + (result.dataFilesMade() == 0 ? " (이미 준비돼 있었음)" : " (새 파일 " + result.dataFilesMade() + "개)"));
        Ui.info((result.configWritten() ? "[완료] " : "[유지] ") + "config.properties");

        Ui.blank();
        Ui.info("다음 단계");
        if (!result.databaseDone()) {
            Ui.detail("- 지금은 data/ 폴더에 정보를 저장합니다.");
            Ui.detail("- MySQL 서버를 켜고 마법사를 다시 실행하면 남은 준비를 이어서 합니다.");
        }
        Ui.detail("- map.api.key를 비워 뒀다면 발급 후 config.properties에 채워 넣으세요.");
        Ui.detail("- config.properties와 data/private/는 절대 커밋하지 마세요.");
        Ui.detail("- 자세한 설명: docs/SetupWizard.md");
        Ui.blank();
    }
}
