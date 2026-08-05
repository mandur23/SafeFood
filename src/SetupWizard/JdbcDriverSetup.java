package SetupWizard;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * MySQL Connector/J 자동 확인·설치·클래스패스 등록.
 *
 * <ol>
 *   <li>이미 로드돼 있으면 통과</li>
 *   <li>{@code lib/mysql-connector-j*.jar}가 있으면 런타임에 로드</li>
 *   <li>없으면 Maven Central에서 받아 {@code lib/}에 저장 후 로드</li>
 *   <li>가능하면 {@code SafeFood.iml}에 라이브러리로 등록 (다음 IntelliJ 실행용)</li>
 * </ol>
 */
final class JdbcDriverSetup {

    static final String VERSION = "8.4.0";
    private static final String JAR_NAME = "mysql-connector-j-" + VERSION + ".jar";
    private static final String JAR_ALIAS = "mysql-connector-j.jar";
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String DOWNLOAD_URL =
            "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/"
                    + VERSION + "/" + JAR_NAME;

    private static final Pattern IML_JAR_ENTRY = Pattern.compile(
            "mysql-connector-j[^\"']*\\.jar", Pattern.CASE_INSENSITIVE);

    /** DriverShim으로만 등록된 경우 Class.forName은 실패하므로 별도 플래그로 추적 */
    private static volatile boolean registeredViaShim;

    private JdbcDriverSetup() {
    }

    /**
     * 드라이버를 쓸 수 있는 상태로 만듭니다.
     *
     * @return 성공하면 true (이미 있었거나 방금 설치·로드함)
     */
    static boolean ensureReady(Path projectRoot) {
        if (isReady()) {
            Ui.ok("MySQL Connector/J 확인 완료 (이미 로드됨)");
            return true;
        }

        Path libDir = projectRoot.resolve("lib");
        Path jar = findExistingJar(libDir);

        if (jar == null) {
            Ui.info("드라이버가 없어 Maven Central에서 받습니다...");
            Ui.detail(DOWNLOAD_URL);
            try {
                jar = download(libDir);
                Ui.ok("다운로드 완료: lib/" + jar.getFileName());
            } catch (IOException e) {
                Ui.fail("드라이버 자동 설치 실패: " + e.getMessage());
                Ui.detail("네트워크·방화벽을 확인하거나, 직접 jar를 lib/에 넣어 주세요.");
                return false;
            }
        } else {
            Ui.info("lib/" + jar.getFileName() + " 발견 — 런타임에 로드합니다.");
        }

        try {
            loadIntoRuntime(jar);
        } catch (Exception e) {
            Ui.fail("드라이버 로드 실패: " + e.getMessage());
            return false;
        }

        if (!isReady()) {
            Ui.fail("드라이버를 로드했지만 사용할 수 없습니다.");
            return false;
        }

        Ui.ok("MySQL Connector/J 준비 완료 (" + jar.getFileName() + ")");
        registerInIdeaModule(projectRoot, jar);
        return true;
    }

    /** 클래스패스에 있거나, 이번 실행에서 shim으로 등록됐으면 true */
    static boolean isReady() {
        if (registeredViaShim) {
            return true;
        }
        try {
            Class.forName(DRIVER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                return true;
            } catch (ClassNotFoundException ignored) {
                return false;
            }
        }
    }

    private static Path findExistingJar(Path libDir) {
        if (!Files.isDirectory(libDir)) {
            return null;
        }
        Path preferred = libDir.resolve(JAR_NAME);
        if (Files.isRegularFile(preferred)) {
            return preferred;
        }
        Path alias = libDir.resolve(JAR_ALIAS);
        if (Files.isRegularFile(alias)) {
            return alias;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(libDir, "mysql-connector*.jar")) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    return path;
                }
            }
        } catch (IOException ignored) {
            // 없으면 null
        }
        return null;
    }

    private static Path download(Path libDir) throws IOException {
        Files.createDirectories(libDir);
        Path target = libDir.resolve(JAR_NAME);
        Path temp = libDir.resolve(JAR_NAME + ".part");

        try (InputStream in = URI.create(DOWNLOAD_URL).toURL().openStream()) {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);

        Path alias = libDir.resolve(JAR_ALIAS);
        Files.copy(target, alias, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    /**
     * 시스템 클래스패스에 jar를 붙이거나, 안 되면 URLClassLoader + DriverShim으로 등록합니다.
     * (Java 9+ 에서는 시스템 ClassLoader에 addURL이 막혀 있는 경우가 많습니다.)
     */
    private static void loadIntoRuntime(Path jar) throws Exception {
        URL url = jar.toUri().toURL();

        if (tryAddUrlToSystemClassLoader(url)) {
            try {
                Class.forName(DRIVER_CLASS);
                return;
            } catch (ClassNotFoundException ignored) {
                // Java 9+ 모듈 제한으로 addURL이 무시된 경우 → shim으로 진행
            }
        }

        URLClassLoader loader = new URLClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader());
        Class<?> clazz = Class.forName(DRIVER_CLASS, true, loader);
        Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();
        DriverManager.registerDriver(new DriverShim(driver));
        registeredViaShim = true;
    }

    private static boolean tryAddUrlToSystemClassLoader(URL url) {
        ClassLoader system = ClassLoader.getSystemClassLoader();
        if (!(system instanceof URLClassLoader)) {
            return false;
        }
        try {
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            addUrl.invoke(system, url);
            return true;
        } catch (ReflectiveOperationException | SecurityException e) {
            return false;
        }
    }

    /** IntelliJ 다음 실행을 위해 SafeFood.iml에 jar를 라이브러리로 넣습니다. */
    private static void registerInIdeaModule(Path projectRoot, Path jar) {
        Path iml = projectRoot.resolve("SafeFood.iml");
        if (!Files.isRegularFile(iml)) {
            return;
        }
        try {
            String xml = Files.readString(iml, StandardCharsets.UTF_8);
            String jarFileName = jar.getFileName().toString();
            if (IML_JAR_ENTRY.matcher(xml).find()) {
                Ui.detail("IntelliJ 모듈에 드라이버가 이미 등록돼 있습니다.");
                return;
            }

            String entry = """
                        <orderEntry type="module-library">
                          <library>
                            <CLASSES>
                              <root url="jar://$MODULE_DIR$/lib/%s!/" />
                            </CLASSES>
                            <JAVADOC />
                            <SOURCES />
                          </library>
                        </orderEntry>
                    """.formatted(jarFileName);

            String marker = "</component>";
            int idx = xml.lastIndexOf(marker);
            if (idx < 0) {
                return;
            }
            String updated = xml.substring(0, idx) + entry + "  " + xml.substring(idx);
            Files.writeString(iml, updated, StandardCharsets.UTF_8);
            Ui.ok("IntelliJ 모듈(SafeFood.iml)에 드라이버를 등록했습니다.");
            Ui.detail("에디터가 바로 반영하지 않으면 프로젝트를 다시 열어 주세요.");
        } catch (IOException e) {
            Ui.warn("SafeFood.iml 자동 등록 실패 (실행에는 영향 없음): " + e.getMessage());
        }
    }

    /**
     * 자식 ClassLoader에서 만든 Driver를 DriverManager가 쓰게 해주는 래퍼.
     * DriverManager는 자신을 로드한 ClassLoader의 드라이버만 직접 받습니다.
     */
    private static final class DriverShim implements Driver {
        private final Driver delegate;

        private DriverShim(Driver delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return delegate.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return delegate.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return delegate.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return delegate.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }
}
