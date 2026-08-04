package SetupWizard;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 데이터베이스 준비 작업 담당.
 *
 * <p>연결 테스트 → 데이터베이스 생성 → 테이블 생성 → 기본 데이터 삽입 순서로 쓰입니다.
 * 모든 단계가 <b>여러 번 실행해도 안전(멱등)</b>하도록 작성했습니다.
 */
final class DatabaseInitializer {

    /** 작업 결과 (처리한 개수 / 이미 있어서 건너뛴 개수) */
    record Result(int done, int skipped) {
    }

    private final DbConfig config;

    DatabaseInitializer(DbConfig config) {
        this.config = config;
    }

    /**
     * MySQL Connector/J가 클래스패스에 있는지 확인합니다.
     * JDBC 4.0부터는 자동 등록이라 Class.forName이 필수는 아니지만,
     * "드라이버가 없다"는 사실을 미리 알려 주려고 명시적으로 확인합니다.
     */
    static boolean loadDriver() {
        String[] candidates = {"com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver"};
        for (String name : candidates) {
            try {
                Class.forName(name);
                return true;
            } catch (ClassNotFoundException ignored) {
                // 다음 후보 이름으로 재시도
            }
        }
        return false;
    }

    /** 데이터베이스가 아직 없어도 되도록, 서버에만 붙어서 연결을 확인합니다. */
    void testConnection() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                config.serverUrl(), config.user(), config.password())) {
            Ui.detail("MySQL 버전: " + connection.getMetaData().getDatabaseProductVersion());
        }
    }

    /** 데이터베이스 생성. 이미 있으면 아무 일도 하지 않습니다. */
    void createDatabase() throws SQLException {
        // 이름은 Ui.askIdentifier에서 영문·숫자·밑줄만 통과시켰습니다.
        // (식별자는 PreparedStatement의 ? 로 넘길 수 없어 직접 이어 붙여야 하므로 검사가 필수입니다)
        String sql = "CREATE DATABASE IF NOT EXISTS `" + config.database() + "` "
                + "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        try (Connection connection = DriverManager.getConnection(
                     config.serverUrl(), config.user(), config.password());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    /**
     * 테이블 생성. 외래 키 순서 때문에 {@link Schema#TABLES}에 적힌 순서대로 만듭니다.
     * 이미 있는 테이블은 그대로 두므로 데이터가 지워지지 않습니다.
     */
    Result createTables() throws SQLException {
        int created = 0;
        int skipped = 0;
        try (Connection connection = DriverManager.getConnection(
                     config.databaseUrl(), config.user(), config.password());
             Statement statement = connection.createStatement()) {

            for (Schema.Table table : Schema.TABLES) {
                boolean existed = tableExists(connection, table.name());
                statement.executeUpdate(table.ddl());
                if (existed) {
                    skipped++;
                } else {
                    created++;
                    Ui.detail("+ " + table.name());
                }
            }
        }
        return new Result(created, skipped);
    }

    /**
     * 알레르기·기분 마스터 데이터를 채웁니다.
     * 두 테이블 모두 name이 UNIQUE라서 INSERT IGNORE로 중복 삽입을 막을 수 있습니다.
     */
    Result insertMasterData() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                config.databaseUrl(), config.user(), config.password())) {

            int allergies = insertNames(connection, "allergy", Schema.ALLERGIES);
            int moods = insertNames(connection, "mood", Schema.MOODS);

            Ui.detail("알레르기 " + allergies + "건 추가 (전체 " + Schema.ALLERGIES.size() + "건)");
            Ui.detail("기분 태그 " + moods + "건 추가 (전체 " + Schema.MOODS.size() + "건)");

            int total = Schema.ALLERGIES.size() + Schema.MOODS.size();
            int inserted = allergies + moods;
            return new Result(inserted, total - inserted);
        }
    }

    private int insertNames(Connection connection, String table, List<String> names) throws SQLException {
        int inserted = 0;
        // table은 이 클래스 안에서만 넘기는 고정 문자열이라 안전합니다.
        String sql = "INSERT IGNORE INTO " + table + " (name) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String name : names) {
                statement.setString(1, name);
                inserted += statement.executeUpdate();
            }
        }
        return inserted;
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet tables = connection.getMetaData()
                .getTables(config.database(), null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    /** SQLException을 팀원이 바로 조치할 수 있는 한글 안내로 바꿔 줍니다. */
    static String explain(SQLException e) {
        String sqlState = e.getSQLState() == null ? "" : e.getSQLState();
        if (sqlState.startsWith("08")) {
            return "MySQL 서버에 연결하지 못했습니다. 서버가 켜져 있는지, 호스트·포트가 맞는지 확인하세요.";
        }
        return switch (e.getErrorCode()) {
            case 1045 -> "계정 또는 비밀번호가 맞지 않습니다.";
            case 1044 -> "이 계정에는 해당 데이터베이스를 다룰 권한이 없습니다.";
            case 1049 -> "데이터베이스가 존재하지 않습니다.";
            case 1142, 1227 -> "권한이 부족합니다. 권한이 있는 계정으로 다시 시도해 보세요.";
            default -> e.getMessage();
        };
    }
}
