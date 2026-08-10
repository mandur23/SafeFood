package com.safefood.setup.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
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

/**
 * MySQL Connector/J 확인 및 (Maven을 쓰지 않는 경우의) 대비책.
 *
 * <p><b>Maven으로 실행하면 이 클래스는 사실상 하는 일이 없습니다.</b>
 * pom.xml의 {@code com.mysql:mysql-connector-j} 의존성이 이미 클래스패스에 드라이버를
 * 올려 두므로 첫 단계에서 바로 통과합니다.
 *
 * <p>아래 대비책은 Maven 없이 {@code javac}로 직접 컴파일해 돌리는 경우를 위해 남겨 둡니다.
 * <ol>
 *   <li>이미 로드돼 있으면 통과 (Maven 실행 시 여기서 끝)</li>
 *   <li>{@code lib/mysql-connector-j*.jar}가 있으면 런타임에 로드</li>
 *   <li>없으면 Maven Central에서 받아 {@code lib/}에 저장 후 로드</li>
 * </ol>
 */
public final class JdbcDriverSetup {

    static final String VERSION = "8.4.0";
    private static final String JAR_NAME = "mysql-connector-j-" + VERSION + ".jar";
    private static final String JAR_ALIAS = "mysql-connector-j.jar";
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String DOWNLOAD_URL =
            "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/"
                    + VERSION + "/" + JAR_NAME;

    /** DriverShim으로만 등록된 경우 Class.forName은 실패하므로 별도 플래그로 추적 */
    private static volatile boolean registeredViaShim;

    private JdbcDriverSetup() {
    }

    /**
     * 드라이버를 쓸 수 있는 상태로 만듭니다.
     *
     * @param out 진행 상황을 알릴 화면 ({@code System.out}을 직접 쓰지 않기 위해 받습니다)
     * @return 성공하면 true (이미 있었거나 방금 설치·로드함)
     */
    public static boolean ensureReady(Path projectRoot, SetupReporter out) {
        if (isReady()) {
            out.ok("MySQL Connector/J 확인 완료 (이미 로드됨)");
            return true;
        }

        Path libDir = projectRoot.resolve("lib");
        Path jar = findExistingJar(libDir);

        if (jar == null) {
            out.info("드라이버가 없어 Maven Central에서 받습니다...");
            out.detail(DOWNLOAD_URL);
            try {
                jar = download(libDir);
                out.ok("다운로드 완료: lib/" + jar.getFileName());
            } catch (IOException e) {
                out.fail("드라이버 자동 설치 실패: " + e.getMessage());
                out.detail("네트워크·방화벽을 확인하거나, 직접 jar를 lib/에 넣어 주세요.");
                return false;
            }
        } else {
            out.info("lib/" + jar.getFileName() + " 발견 — 런타임에 로드합니다.");
        }

        try {
            loadIntoRuntime(jar);
        } catch (Exception e) {
            out.fail("드라이버 로드 실패: " + e.getMessage());
            return false;
        }

        if (!isReady()) {
            out.fail("드라이버를 로드했지만 사용할 수 없습니다.");
            return false;
        }

        out.ok("MySQL Connector/J 준비 완료 (" + jar.getFileName() + ")");
        out.detail("Maven으로 실행하면(mvn compile exec:java) 이 단계 없이 바로 통과합니다.");
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
