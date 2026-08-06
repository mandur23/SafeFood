package SetupWizard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 설정 파일(src/config.properties)과 .gitignore 처리.
 *
 * <p>비밀번호가 담기는 파일을 만들기 때문에, .gitignore에 등록됐는지도 함께 확인합니다.
 * 팀원 공유용으로는 값이 비어 있는 config.properties.example을 따로 만듭니다.
 */
final class ConfigFileWriter {

    private static final String CONFIG_NAME = "config.properties";
    private static final String EXAMPLE_NAME = "config.properties.example";
    private static final String IGNORE_ENTRY = "src/config.properties";

    private ConfigFileWriter() {
    }

    /**
     * 프로젝트 최상위 경로를 찾습니다.
     *
     * <p>실행 위치(user.dir)에서 시작해 위로 올라가며 src 폴더와 README.md가 같이 있는 곳을 찾습니다.
     * IntelliJ ▶ 버튼으로 실행하면 대개 첫 번째 시도에 바로 찾습니다.
     */
    static Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int depth = 0; depth < 5 && current != null; depth++) {
            if (Files.isDirectory(current.resolve("src")) && Files.exists(current.resolve("README.md"))) {
                return current;
            }
            current = current.getParent();
        }
        // 못 찾으면 실행 위치를 그대로 씁니다.
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    }

    static Path configPath(Path projectRoot) {
        return projectRoot.resolve("src").resolve(CONFIG_NAME);
    }

    static Path examplePath(Path projectRoot) {
        return projectRoot.resolve("src").resolve(EXAMPLE_NAME);
    }

    /** 기존 파일을 config.properties.bak으로 백업합니다. */
    static Path backup(Path configFile) throws IOException {
        Path backup = configFile.resolveSibling(CONFIG_NAME + ".bak");
        Files.copy(configFile, backup, StandardCopyOption.REPLACE_EXISTING);
        return backup;
    }

    static void write(Path configFile,
                      DbConfig db,
                      String mapApiKey,
                      String socketHost,
                      int socketPort) throws IOException {

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String content = """
                # Safefood 로컬 설정 파일
                # Setup Wizard가 %s에 생성했습니다.
                #
                # !! 이 파일에는 개인 계정 정보가 들어 있습니다. 절대 커밋하지 마세요.
                # !! 팀원과 공유할 때는 값이 비어 있는 %s를 쓰세요.

                # 데이터베이스
                db.url=%s
                db.user=%s
                db.password=%s

                # 지도 API 키 (Kakao Developers 등에서 발급)
                map.api.key=%s

                # 소켓 서버 주소 (그룹 실시간 통신)
                socket.host=%s
                socket.port=%d
                """.formatted(
                stamp,
                EXAMPLE_NAME,
                escape(db.databaseUrl()),
                escape(db.user()),
                escape(db.password()),
                escape(mapApiKey),
                escape(socketHost),
                socketPort);

        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, content, StandardCharsets.UTF_8);
    }

    /** 값이 비어 있는 공유용 예시 파일. 비밀이 없으므로 커밋해도 됩니다. */
    static void writeExample(Path exampleFile, String databaseName, int socketPort) throws IOException {
        String content = """
                # FoodMate 설정 파일 예시 (이 파일은 커밋해도 됩니다)
                #
                # 이 파일을 config.properties로 복사한 뒤 본인 환경에 맞게 채우거나,
                # SetupWizard를 실행하면 자동으로 만들어집니다.

                db.url=jdbc:mysql://localhost:3306/%s?%s
                db.user=
                db.password=

                map.api.key=

                socket.host=localhost
                socket.port=%d
                """.formatted(databaseName, DbConfig.PARAMS, socketPort);

        Files.createDirectories(exampleFile.getParent());
        Files.writeString(exampleFile, content, StandardCharsets.UTF_8);
    }

    /**
     * .gitignore에 config.properties가 등록돼 있는지 확인하고, 없으면 추가합니다.
     *
     * @return 파일을 실제로 고쳤으면 true
     */
    static boolean ensureGitignore(Path projectRoot) throws IOException {
        Path gitignore = projectRoot.resolve(".gitignore");
        String existing = Files.exists(gitignore)
                ? Files.readString(gitignore, StandardCharsets.UTF_8)
                : "";

        if (existing.contains(CONFIG_NAME)) {
            return false;
        }

        StringBuilder updated = new StringBuilder(existing);
        if (!existing.isEmpty() && !existing.endsWith("\n")) {
            updated.append(System.lineSeparator());
        }
        updated.append(System.lineSeparator())
                .append("### FoodMate ###").append(System.lineSeparator())
                .append("# 개인 DB 계정·API 키가 들어 있는 파일 (공유용 예시는 .example)")
                .append(System.lineSeparator())
                .append(IGNORE_ENTRY).append(System.lineSeparator());

        Files.writeString(gitignore, updated.toString(), StandardCharsets.UTF_8);
        return true;
    }

    /**
     * .properties 값에 그대로 쓸 수 없는 문자를 escape 합니다.
     * 역슬래시는 이어쓰기 기호라서, 비밀번호에 들어 있으면 값이 깨집니다.
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
