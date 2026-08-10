package com.safefood.setup.core;

/**
 * MySQL 접속 정보.
 *
 * <p>비밀번호를 들고 있으므로 {@link #toString()}에는 절대 포함하지 않습니다.
 * (로그·예외 메시지에 비밀번호가 찍히는 사고를 막기 위함)
 */
public final class DbConfig {

    /**
     * 로컬 개발용 접속 옵션.
     * <ul>
     *   <li>serverTimezone / characterEncoding — 날짜·한글 깨짐 방지</li>
     *   <li>allowPublicKeyRetrieval — MySQL 8 기본 인증(caching_sha2_password) 사용 시 필요</li>
     *   <li>useSSL=false — 로컬 전용. 실제 서버에 배포한다면 반드시 제거하세요.</li>
     * </ul>
     */
    static final String PARAMS =
            "serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&useSSL=false";

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    public DbConfig(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    /** 데이터베이스를 지정하지 않은 URL — DB를 만들기 전 서버에만 붙을 때 사용 */
    String serverUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/?" + PARAMS;
    }

    /** 애플리케이션이 실제로 쓰는 URL */
    String databaseUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + PARAMS;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String database() {
        return database;
    }

    public String user() {
        return user;
    }

    public String password() {
        return password;
    }

    @Override
    public String toString() {
        return user + "@" + host + ":" + port + " (DB: " + database + ")";
    }
}
