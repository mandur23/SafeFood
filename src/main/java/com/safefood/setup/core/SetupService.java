package com.safefood.setup.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * 마법사의 실제 처리 부분. 화면이 없어도 돌아갑니다(headless).
 *
 * <p>{@code SetupWizard}(콘솔)와 {@code SetupWizardFx}(JavaFX)는 값을 모아 {@link SetupRequest}를
 * 만들고 이 클래스에 넘기기만 합니다. 덕분에 <b>두 화면의 동작이 항상 같습니다.</b>
 *
 * <p>실행 중에는 사용자에게 아무것도 묻지 않습니다. 물어볼 것은 모두 {@link SetupRequest}에
 * 담겨 있어야 합니다. (JavaFX에서 이 작업을 백그라운드 스레드로 돌리기 위한 조건이기도 합니다.)
 *
 * <p>단계 1·2(드라이버 확인, 접속 정보 입력)는 사용자와 주고받아야 해서 각 화면이 담당합니다.
 * 이 클래스는 <b>3~7단계</b>를 처리합니다.
 *
 * <p>모든 단계는 <b>여러 번 실행해도 안전(멱등)</b>합니다. 이미 있는 DB·테이블·데이터·파일은
 * 그대로 두고 건너뜁니다. 중간에 실패해도 가능한 단계까지는 계속 진행합니다.
 */
public final class SetupService {

    /** 화면에 표시할 전체 단계 수. 단계를 늘리면 docs/SetupWizard.md의 표도 같이 고쳐 주세요. */
    public static final int TOTAL_STEPS = 7;

    private final SetupReporter out;

    public SetupService(SetupReporter out) {
        this.out = out;
    }

    /**
     * 3~7단계를 순서대로 실행합니다.
     *
     * @throws IOException 설정 파일을 쓰지 못한 경우 (폴더 권한 문제 등)
     */
    public SetupResult run(SetupRequest request) throws IOException {
        DatabaseInitializer initializer = new DatabaseInitializer(request.db(), out);

        boolean databaseReady = createDatabase(request, initializer);
        boolean tablesReady = createTables(databaseReady, initializer);
        boolean masterDataDone = insertMasterData(tablesReady, initializer);
        int dataFilesMade = prepareDataFolder(request);
        boolean configWritten = writeConfigFiles(request);

        return new SetupResult(databaseReady, tablesReady, masterDataDone, dataFilesMade, configWritten);
    }

    // ── [3/7] 데이터베이스 ───────────────────────────────

    private boolean createDatabase(SetupRequest request, DatabaseInitializer initializer) {
        out.step(3, TOTAL_STEPS, "데이터베이스 생성");
        if (!request.useDatabase()) {
            out.warn("MySQL을 쓰지 않으므로 건너뜁니다. (데이터는 data/ 폴더에 저장합니다)");
            return false;
        }
        try {
            initializer.createDatabase();
            out.ok("데이터베이스 `" + request.db().database() + "` 준비 완료");
            return true;
        } catch (SQLException e) {
            out.fail(DatabaseInitializer.explain(e));
            return false;
        }
    }

    // ── [4/7] 테이블 ─────────────────────────────────────

    private boolean createTables(boolean databaseReady, DatabaseInitializer initializer) {
        out.step(4, TOTAL_STEPS, "테이블 생성");
        if (!databaseReady) {
            out.warn("데이터베이스가 준비되지 않아 건너뜁니다.");
            return false;
        }
        try {
            DatabaseInitializer.Result result = initializer.createTables();
            out.ok("새로 만든 테이블 " + result.done() + "개 / 이미 있던 테이블 " + result.skipped() + "개");
            return true;
        } catch (SQLException e) {
            out.fail(DatabaseInitializer.explain(e));
            return false;
        }
    }

    // ── [5/7] 기본 데이터 ────────────────────────────────

    private boolean insertMasterData(boolean tablesReady, DatabaseInitializer initializer) {
        out.step(5, TOTAL_STEPS, "기본 데이터 삽입 (알레르기 · 기분 태그)");
        if (!tablesReady) {
            out.warn("테이블이 준비되지 않아 건너뜁니다. (기본 목록은 data/public/에도 만들어 둡니다)");
            return false;
        }
        try {
            DatabaseInitializer.Result result = initializer.insertMasterData();
            out.ok("새로 넣은 항목 " + result.done() + "개 / 이미 있던 항목 " + result.skipped() + "개");
            return true;
        } catch (SQLException e) {
            out.fail(DatabaseInitializer.explain(e));
            return false;
        }
    }

    // ── [6/7] data/ 폴더 ─────────────────────────────────

    /**
     * DB를 쓰든 안 쓰든 폴더는 항상 만들어 둡니다.
     * 나중에 MySQL을 끄고 파일 모드로 바꿔도 바로 쓸 수 있게 하기 위함입니다.
     */
    private int prepareDataFolder(SetupRequest request) {
        out.step(6, TOTAL_STEPS, "data/ 폴더 준비 (DB 미사용 시 저장소)");
        try {
            int created = new DataStoreInitializer(request.projectRoot(), out).prepare();
            if (created == 0) {
                out.ok("이미 준비돼 있습니다. (건드리지 않았습니다)");
            } else {
                out.ok("새로 만든 파일 " + created + "개 — data/public/(공유) · data/private/(커밋 금지)");
            }
            return created;
        } catch (IOException e) {
            // 설정 파일 작성은 계속할 수 있으므로 여기서 멈추지 않습니다.
            out.fail("data/ 폴더를 만들지 못했습니다: " + e.getMessage());
            return 0;
        }
    }

    // ── [7/7] 설정 파일 ──────────────────────────────────

    private boolean writeConfigFiles(SetupRequest request) throws IOException {
        out.step(7, TOTAL_STEPS, "설정 파일 작성");

        Path projectRoot = request.projectRoot();
        Path configFile = ConfigFileWriter.configPath(projectRoot);
        boolean written = false;

        if (!Files.exists(configFile)) {
            ConfigFileWriter.write(configFile, request.db(), request.mapApiKey(),
                    request.socketHost(), request.socketPort());
            written = true;
        } else if (request.overwriteConfig()) {
            Path backup = ConfigFileWriter.backup(configFile);
            out.detail("백업 완료: " + backup.getFileName());
            ConfigFileWriter.write(configFile, request.db(), request.mapApiKey(),
                    request.socketHost(), request.socketPort());
            written = true;
        } else {
            out.info("config.properties가 이미 있어 그대로 둡니다.");
        }

        if (written) {
            out.ok("작성 완료: " + configFile);
        }

        Path exampleFile = ConfigFileWriter.examplePath(projectRoot);
        if (ConfigFileWriter.writeExample(exampleFile)) {
            out.ok("공유용 예시 파일 갱신: " + exampleFile.getFileName());
        } else {
            out.ok("공유용 예시 파일 확인 완료 (내용이 같아 그대로 둠)");
        }

        int added = ConfigFileWriter.ensureGitignore(projectRoot);
        if (added == 0) {
            out.ok(".gitignore 확인 완료 (이미 등록돼 있음)");
        } else {
            out.ok(".gitignore에 커밋 금지 항목 " + added + "개를 추가했습니다.");
        }
        return written;
    }
}
