package com.safefood.setup.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 설정 파일(프로젝트 루트의 config.properties)과 .gitignore 처리.
 *
 * <p>비밀번호가 담기는 파일을 만들기 때문에, .gitignore에 등록됐는지도 함께 확인합니다.
 * 팀원 공유용으로는 값이 비어 있는 config.properties.example을 따로 만듭니다.
 *
 * <p>이 파일은 마법사가 <b>실행 중에</b> 만들고 고치므로 src/main/resources가 아니라
 * 프로젝트 루트에 둡니다. resources에 두면 빌드로 target/classes에 복사된 사본을 읽게 되어,
 * 값을 고쳐도 다시 빌드하기 전까지 반영되지 않습니다.
 */
public final class ConfigFileWriter {

    private static final String CONFIG_NAME = "config.properties";
    private static final String EXAMPLE_NAME = "config.properties.example";

    /** 공유용 예시 파일에 쓸 프로젝트 기본값. 개인이 고른 값이 아닙니다. */
    private static final String DEFAULT_DATABASE = "safefood";
    private static final int DEFAULT_SOCKET_PORT = 5000;

    /**
     * .gitignore에 반드시 있어야 하는 항목.
     * <ul>
     *   <li>{@code /config.properties} — 개인 DB 계정·API 키</li>
     *   <li>{@code /config.properties.bak} — 덮어쓰기 전 백업본 (같은 내용이 들어 있음)</li>
     *   <li>{@code data/private/} — 개인 데이터 · 팀 서버 데이터</li>
     * </ul>
     */
    private static final List<String> REQUIRED_IGNORES =
            List.of("/config.properties", "/config.properties.bak", "data/private/");

    private ConfigFileWriter() {
    }

    /**
     * 프로젝트 최상위 경로를 찾습니다.
     *
     * <p>실행 위치(user.dir)에서 시작해 위로 올라가며 pom.xml과 README.md가 같이 있는 곳을 찾습니다.
     * IntelliJ ▶ 버튼이나 mvn exec:java로 실행하면 대개 첫 번째 시도에 바로 찾습니다.
     */
    public static Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int depth = 0; depth < 5 && current != null; depth++) {
            if (Files.isRegularFile(current.resolve("pom.xml")) && Files.exists(current.resolve("README.md"))) {
                return current;
            }
            current = current.getParent();
        }
        // 못 찾으면 실행 위치를 그대로 씁니다.
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    }

    /** 화면에서 "이미 있는지" 확인할 때도 쓰므로 열어 둡니다. */
    public static Path configPath(Path projectRoot) {
        return projectRoot.resolve(CONFIG_NAME);
    }

    static Path examplePath(Path projectRoot) {
        return projectRoot.resolve(EXAMPLE_NAME);
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

        writeFile(configFile, content);
    }

    /**
     * 값이 비어 있는 공유용 예시 파일. 비밀이 없으므로 커밋해도 됩니다.
     *
     * <p><b>내가 입력한 값(DB 이름·포트)은 여기에 넣지 않습니다.</b>
     * 이 파일은 팀이 공유하는 커밋 대상이라, 각자 고른 값이 들어가면
     * 마법사를 돌릴 때마다 서로 다른 내용으로 바뀌어 충돌합니다.
     * 그래서 항상 프로젝트 기본값으로만 씁니다.
     *
     * @return 내용이 달라져서 실제로 파일을 고쳤으면 true (같으면 손대지 않고 false)
     */
    static boolean writeExample(Path exampleFile) throws IOException {
        String content = """
                # SafeFood 설정 파일 예시 (이 파일은 커밋해도 됩니다)
                #
                # 같은 폴더(프로젝트 루트)에 config.properties로 복사한 뒤 본인 환경에 맞게 채우거나,
                # Setup Wizard를 실행하면 자동으로 만들어집니다: mvn compile exec:java -Pwizard
                # (창으로 된 화면: mvn javafx:run -Pwizard-fx)

                db.url=jdbc:mysql://localhost:3306/%s?%s
                db.user=
                db.password=

                map.api.key=

                socket.host=localhost
                socket.port=%d
                """.formatted(DEFAULT_DATABASE, DbConfig.PARAMS, DEFAULT_SOCKET_PORT);

        // 줄바꿈 방식(CRLF/LF)만 다른 경우까지 "바뀐 것"으로 보지 않도록 맞춰서 비교합니다.
        if (Files.exists(exampleFile)
                && normalizeNewlines(Files.readString(exampleFile, StandardCharsets.UTF_8))
                        .equals(normalizeNewlines(content))) {
            return false;
        }

        writeFile(exampleFile, content);
        return true;
    }

    private static String normalizeNewlines(String text) {
        return text.replaceAll("\\R", "\n");
    }

    /**
     * 상위 폴더를 만든 뒤 UTF-8로 씁니다.
     * 경로가 파일 이름 하나뿐이면 {@code getParent()}가 null이라 그 경우는 폴더 생성을 건너뜁니다.
     */
    private static void writeFile(Path file, String content) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    /**
     * 커밋되면 안 되는 항목이 .gitignore에 있는지 확인하고, 빠진 것만 추가합니다.
     *
     * <p>"파일 전체에 문자열이 들어 있는지"로 확인하면 안 됩니다.
     * 예를 들어 {@code config.properties.example}만 적혀 있어도
     * {@code contains("config.properties")}는 true라서, 정작 막아야 할
     * {@code config.properties}가 그대로 커밋될 수 있습니다.
     * 그래서 <b>줄 단위로</b> 비교합니다.
     *
     * @return 새로 추가한 항목 수 (0이면 이미 다 등록돼 있던 것)
     */
    static int ensureGitignore(Path projectRoot) throws IOException {
        Path gitignore = projectRoot.resolve(".gitignore");
        String existing = Files.exists(gitignore)
                ? Files.readString(gitignore, StandardCharsets.UTF_8)
                : "";

        List<String> missing = new ArrayList<>();
        for (String entry : REQUIRED_IGNORES) {
            if (!hasEntry(existing, entry)) {
                missing.add(entry);
            }
        }
        if (missing.isEmpty()) {
            return 0;
        }

        String newLine = System.lineSeparator();
        StringBuilder updated = new StringBuilder(existing);
        if (!existing.isEmpty() && !existing.endsWith("\n")) {
            updated.append(newLine);
        }
        updated.append(newLine)
                .append("### SafeFood — Setup Wizard가 추가한 항목 ###").append(newLine)
                .append("# 개인 DB 계정·API 키, 개인·팀 서버 데이터 (공유용은 .example과 data/public/)")
                .append(newLine);
        for (String entry : missing) {
            updated.append(entry).append(newLine);
        }

        Files.writeString(gitignore, updated.toString(), StandardCharsets.UTF_8);
        return missing.size();
    }

    /**
     * .gitignore 본문에 해당 항목이 <b>한 줄로</b> 등록돼 있는지 확인합니다.
     * 주석(#)과 빈 줄은 건너뛰고, {@code /config.properties}와 {@code config.properties}처럼
     * 같은 뜻인 표기는 같은 것으로 봅니다.
     */
    private static boolean hasEntry(String gitignoreContent, String entry) {
        String target = normalizeEntry(entry);
        for (String line : gitignoreContent.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (normalizeEntry(trimmed).equals(target)) {
                return true;
            }
        }
        return false;
    }

    /** 앞뒤의 {@code /}를 떼어 같은 뜻의 표기를 하나로 맞춥니다. */
    private static String normalizeEntry(String entry) {
        String value = entry.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
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
