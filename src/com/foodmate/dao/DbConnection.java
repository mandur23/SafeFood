package com.foodmate.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DbConnection {
    private static final String CONFIG_NAME = "config.properties";
    private static Properties config;

    private DbConnection() {
    }

    public static Connection get() throws SQLException {
        Properties settings = config();
        String url = settings.getProperty("db.url", "").trim();
        if (url.isEmpty()) {
            throw new SQLException("db.url이 비어 있습니다. src/" + CONFIG_NAME + "을 확인하세요. "
                    + "(SetupWizard를 실행하면 자동으로 채워집니다)");
        }
        return DriverManager.getConnection(url,
                settings.getProperty("db.user", ""),
                settings.getProperty("db.password", ""));
    }

    public static synchronized void reload() {
        config = null;
    }

    private static synchronized Properties config() throws SQLException {
        if (config != null) {
            return config;
        }
        Properties loaded = new Properties();
        try (Reader reader = openConfig()) {
            loaded.load(reader);
        } catch (IOException e) {
            throw new SQLException("설정 파일을 읽지 못했습니다: " + e.getMessage(), e);
        }
        config = loaded;
        return config;
    }

    private static Reader openConfig() throws IOException {
        InputStream stream = DbConnection.class.getResourceAsStream("/" + CONFIG_NAME);
        if (stream != null) {
            return new InputStreamReader(stream, StandardCharsets.UTF_8);
        }
        Path file = findConfigFile();
        if (file == null) {
            throw new IOException("src/" + CONFIG_NAME + "을 찾지 못했습니다. SetupWizard를 먼저 실행하세요.");
        }
        return Files.newBufferedReader(file, StandardCharsets.UTF_8);
    }

    private static Path findConfigFile() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int depth = 0; depth < 5 && current != null; depth++) {
            Path candidate = current.resolve("src").resolve(CONFIG_NAME);
            if (Files.isReadable(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }
}
