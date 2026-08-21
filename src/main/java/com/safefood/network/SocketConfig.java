package com.safefood.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 프로젝트 루트 {@code config.properties}의 {@code socket.*} 값.
 *
 * <p>파일이 없거나 값이 비어 있으면 기본값(localhost:5000)을 씁니다.
 * 이 파일은 Setup Wizard가 만들며, 루트에 두는 이유는 README에 정리돼 있습니다.
 */
public final class SocketConfig {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5000;

    private SocketConfig() {
    }

    /**
     * 방장이 초대 코드에 담을 주소를 직접 지정할 때 씁니다 — 어댑터가 여럿이라
     * {@link GroupServer#lanAddress()}가 엉뚱한 주소를 고를 때의 수동 지정용입니다.
     * 기본값({@value #DEFAULT_HOST})이면 자동 판별을 씁니다.
     *
     * <p>참여자 쪽은 이 값을 보지 않습니다 — 접속 주소는 초대 코드에서 꺼냅니다.
     */
    public static String host() {
        String value = load().getProperty("socket.host", "").trim();
        return value.isEmpty() ? DEFAULT_HOST : value;
    }

    public static int port() {
        try {
            return Integer.parseInt(load().getProperty("socket.port", "").trim());
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    private static Properties load() {
        Properties props = new Properties();
        Path file = Path.of("config.properties");
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                System.err.println("[socket] config.properties를 읽지 못했습니다: " + e.getMessage());
            }
        }
        return props;
    }
}
