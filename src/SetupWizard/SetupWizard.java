package SetupWizard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * FoodMate 개발 환경 설정 마법사.
 *
 * <p>저장소를 클론한 팀원이 <b>한 번 실행</b>하면 아래 작업을 순서대로 처리합니다.
 * <ol>
 *   <li>JDBC 드라이버 확인</li>
 *   <li>MySQL 접속 정보 입력 및 연결 테스트</li>
 *   <li>데이터베이스 생성</li>
 *   <li>테이블 생성</li>
 *   <li>기본 데이터(알레르기·기분 태그) 삽입</li>
 *   <li>src/config.properties 작성</li>
 * </ol>
 *
 * <p>중간에 실패해도 되는 만큼은 진행하고, 무엇이 남았는지 마지막에 알려 줍니다.
 * 여러 번 실행해도 기존 데이터는 지워지지 않습니다.
 *
 * <p>실행 방법은 {@code src/SetupWizard/SetupWizard.md} 참고.
 */
public final class SetupWizard {

    private static final int TOTAL_STEPS = 6;
    private static final String DEFAULT_DATABASE = "foodmate";
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

        // [1/6] 드라이버가 없으면 DB 작업은 못 하지만 설정 파일은 만들어 둘 수 있습니다.
        DriverState driverState = checkDriver();
        if (driverState == DriverState.ABORT) {
            Ui.blank();
            Ui.info("마법사를 종료합니다. 드라이버를 추가한 뒤 다시 실행해 주세요.");
            return;
        }

        // [2/6]
        Ui.step(2, TOTAL_STEPS, "MySQL 접속 정보");
        Connection connection = askAndConnect(driverState == DriverState.READY);
        DbConfig db = connection.config();
        DatabaseInitializer initializer = new DatabaseInitializer(db);
        boolean dbReady = connection.connected();

        // [3/6]
        Ui.step(3, TOTAL_STEPS, "데이터베이스 생성");
        if (dbReady) {
            try {
                initializer.createDatabase();
                Ui.ok("데이터베이스 `" + db.database() + "` 준비 완료");
            } catch (SQLException e) {
                Ui.fail(DatabaseInitializer.explain(e));
                dbReady = false;
            }
        } else {
            Ui.warn("MySQL에 연결되지 않아 건너뜁니다.");
        }

        // [4/6]
        Ui.step(4, TOTAL_STEPS, "테이블 생성");
        boolean tablesReady = false;
        if (dbReady) {
            try {
                DatabaseInitializer.Result result = initializer.createTables();
                tablesReady = true;
                Ui.ok("새로 만든 테이블 " + result.done() + "개 / 이미 있던 테이블 " + result.skipped() + "개");
            } catch (SQLException e) {
                Ui.fail(DatabaseInitializer.explain(e));
            }
        } else {
            Ui.warn("데이터베이스가 준비되지 않아 건너뜁니다.");
        }

        // [5/6]
        Ui.step(5, TOTAL_STEPS, "기본 데이터 삽입 (알레르기 · 기분 태그)");
        if (tablesReady) {
            try {
                DatabaseInitializer.Result result = initializer.insertMasterData();
                Ui.ok("새로 넣은 항목 " + result.done() + "개 / 이미 있던 항목 " + result.skipped() + "개");
            } catch (SQLException e) {
                Ui.fail(DatabaseInitializer.explain(e));
            }
        } else {
            Ui.warn("테이블이 준비되지 않아 건너뜁니다.");
        }

        // [6/6]
        Ui.step(6, TOTAL_STEPS, "설정 파일 작성");
        boolean configWritten = writeConfigFiles(projectRoot, db);

        summary(db, dbReady && tablesReady, configWritten);
    }

    // ── [1/6] 드라이버 ───────────────────────────────────

    private enum DriverState {
        /** 드라이버가 있어 DB 작업까지 진행 가능 */
        READY,
        /** 드라이버는 없지만 설정 파일만 만들기로 함 */
        CONFIG_ONLY,
        /** 사용자가 종료를 선택 */
        ABORT
    }

    private static DriverState checkDriver() {
        Ui.step(1, TOTAL_STEPS, "JDBC 드라이버 확인");
        if (DatabaseInitializer.loadDriver()) {
            Ui.ok("MySQL Connector/J 확인 완료");
            return DriverState.READY;
        }

        Ui.fail("MySQL Connector/J를 클래스패스에서 찾지 못했습니다.");
        Ui.detail("1) dev.mysql.com/downloads/connector/j 에서 Platform Independent 버전을 받습니다.");
        Ui.detail("2) 압축을 풀어 나온 mysql-connector-j-x.x.x.jar를 프로젝트 lib 폴더에 넣습니다.");
        Ui.detail("3) IntelliJ: File > Project Structure > Libraries > + > Java > 해당 jar 선택");
        Ui.blank();

        boolean configOnly = Ui.confirm("지금은 설정 파일만 만들까요? (n = 마법사 종료)", true);
        return configOnly ? DriverState.CONFIG_ONLY : DriverState.ABORT;
    }

    // ── [2/6] 접속 정보 ──────────────────────────────────

    /** 입력받은 접속 정보와, 실제로 연결에 성공했는지 여부 */
    private record Connection(DbConfig config, boolean connected) {
    }

    private static Connection askAndConnect(boolean testConnection) {
        while (true) {
            DbConfig config = askDbConfig();
            if (!testConnection) {
                Ui.warn("드라이버가 없어 연결 테스트는 건너뜁니다.");
                return new Connection(config, false);
            }

            Ui.info("연결을 확인하는 중...");
            try {
                new DatabaseInitializer(config).testConnection();
                Ui.ok("연결 성공 — " + config);
                return new Connection(config, true);
            } catch (SQLException e) {
                Ui.fail("연결 실패 — " + DatabaseInitializer.explain(e));
                if (!Ui.confirm("접속 정보를 다시 입력할까요?", true)) {
                    Ui.warn("데이터베이스 단계는 건너뛰고 설정 파일만 만듭니다.");
                    return new Connection(config, false);
                }
            }
        }
    }

    private static DbConfig askDbConfig() {
        String host = Ui.ask("MySQL 호스트", DEFAULT_HOST);
        int port = Ui.askPort("MySQL 포트", DEFAULT_MYSQL_PORT);
        String database = Ui.askIdentifier("데이터베이스 이름", DEFAULT_DATABASE);
        String user = Ui.ask("MySQL 계정", "root");
        String password = Ui.askPassword("MySQL 비밀번호");
        return new DbConfig(host, port, database, user, password);
    }

    // ── [6/6] 설정 파일 ──────────────────────────────────

    private static boolean writeConfigFiles(Path projectRoot, DbConfig db) throws IOException {
        String mapApiKey = Ui.askOptional("지도 API 키");
        String socketHost = Ui.ask("소켓 서버 호스트", DEFAULT_HOST);
        int socketPort = Ui.askPort("소켓 서버 포트", DEFAULT_SOCKET_PORT);
        Ui.blank();

        Path configFile = ConfigFileWriter.configPath(projectRoot);
        boolean written = false;

        if (Files.exists(configFile)) {
            Ui.warn("config.properties 파일이 이미 있습니다.");
            if (Ui.confirm("덮어쓸까요? (기존 파일은 .bak으로 백업합니다)", false)) {
                Path backup = ConfigFileWriter.backup(configFile);
                Ui.detail("백업 완료: " + backup.getFileName());
                ConfigFileWriter.write(configFile, db, mapApiKey, socketHost, socketPort);
                written = true;
            } else {
                Ui.info("기존 파일을 그대로 둡니다.");
            }
        } else {
            ConfigFileWriter.write(configFile, db, mapApiKey, socketHost, socketPort);
            written = true;
        }

        if (written) {
            Ui.ok("작성 완료: " + configFile);
        }

        Path exampleFile = ConfigFileWriter.examplePath(projectRoot);
        ConfigFileWriter.writeExample(exampleFile, db.database(), socketPort);
        Ui.ok("공유용 예시 파일 갱신: " + exampleFile.getFileName());

        if (ConfigFileWriter.ensureGitignore(projectRoot)) {
            Ui.ok(".gitignore에 src/config.properties를 추가했습니다.");
        } else {
            Ui.ok(".gitignore 확인 완료 (이미 등록돼 있음)");
        }
        return written;
    }

    // ── 마무리 ───────────────────────────────────────────

    private static void summary(DbConfig db, boolean databaseDone, boolean configWritten) {
        Ui.section("설정 결과");
        Ui.info((databaseDone ? "[완료] " : "[미완] ") + "데이터베이스 " + db.database() + " (테이블 + 기본 데이터)");
        Ui.info((configWritten ? "[완료] " : "[유지] ") + "src/config.properties");

        Ui.blank();
        Ui.info("다음 단계");
        if (!databaseDone) {
            Ui.detail("- MySQL 서버를 켜고 마법사를 다시 실행하면 남은 준비를 이어서 합니다.");
        }
        Ui.detail("- map.api.key를 비워 뒀다면 발급 후 src/config.properties에 채워 넣으세요.");
        Ui.detail("- config.properties는 절대 커밋하지 마세요. 공유는 .example 파일로 합니다.");
        Ui.detail("- 자세한 설명: src/SetupWizard/SetupWizard.md");
        Ui.blank();
    }
}
