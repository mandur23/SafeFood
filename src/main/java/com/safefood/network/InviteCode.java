package com.safefood.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;

/**
 * 접속 주소를 품고 있는 초대 코드 — 코드 하나만 전달하면 참여자가 방장을 찾아갑니다.
 *
 * <p>중앙 서버가 없어서 "코드 → 주소"를 대신 조회해 줄 곳이 없습니다.
 * 그래서 조회하는 대신 <b>주소를 코드 안에 접어 넣습니다.</b>
 * 참여자는 코드를 풀어 방장 주소를 얻고, 코드 전체는 그대로 {@code JOIN}으로 보내
 * {@link Room}의 암호 대조까지 받습니다.
 *
 * <pre>
 *   6WMK4-2A0P8-Q3XN
 *   └──── 10자 ────┘ └4자┘
 *      주소 + 검사비트   시크릿
 *
 *   앞 10자 = 5비트 × 10 = 50비트
 *            ├ 검사 2비트  (오타 감지)
 *            ├ IPv4 32비트
 *            └ 포트 16비트
 * </pre>
 *
 * <p>글자는 <b>Crockford Base32</b>를 씁니다 — 눈으로 옮겨 적다 헷갈리는 {@code I·L·O·U}를 뺀 32자.
 * 입력할 때 {@code O}는 {@code 0}으로, {@code I·L}은 {@code 1}로 자동 교정하므로
 * 사람이 헷갈려 적어도 같은 코드로 해석됩니다.
 *
 * <p>주소 부분의 오타는 <b>검사 2비트</b>가 여기서 바로 잡아냅니다(약 75%).
 * 시크릿 부분의 오타는 서버가 {@code ERROR|존재하지 않는 초대 코드}로 알려 줍니다.
 */
public final class InviteCode {

    /** Crockford Base32 — I·L·O·U를 뺀 32자. 32 = 2⁵이라 한 글자가 정확히 5비트입니다. */
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    /** 주소부 길이 — (검사 2 + IPv4 32 + 포트 16)비트 = 50비트 = 5비트 × 10자 */
    private static final int ADDRESS_LENGTH = 10;

    /**
     * 시크릿 길이 — 32⁴ ≈ 105만 가지.
     * 방을 여는 동안만 유효하고 시도할 때마다 TCP 접속이 필요해 이 정도로 잡았습니다.
     * 더 강하게 하려면 이 값만 6으로 올리면 됩니다(코드는 16자가 됩니다).
     */
    private static final int SECRET_LENGTH = 4;

    /** 사람이 입력하는 전체 코드 길이 (구분선 {@code -} 제외) */
    public static final int LENGTH = ADDRESS_LENGTH + SECRET_LENGTH;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String code;
    private final InetAddress host;
    private final int port;

    private InviteCode(String code, InetAddress host, int port) {
        this.code = code;
        this.host = host;
        this.port = port;
    }

    // ---- 발급 (방장) ----

    /**
     * 방장 주소 + 랜덤 시크릿으로 새 초대 코드를 만듭니다.
     *
     * @throws IllegalArgumentException IPv4 주소가 아니거나 포트가 범위를 벗어난 경우
     */
    public static String issue(InetAddress host, int port) {
        byte[] ip = host.getAddress();
        if (ip.length != 4) {
            throw new IllegalArgumentException(
                    "초대 코드에는 IPv4 주소만 담을 수 있습니다: " + host.getHostAddress());
        }
        if (port < 1 || port > 0xFFFF) {
            throw new IllegalArgumentException("포트 범위를 벗어났습니다: " + port);
        }

        long payload = 0;
        for (byte octet : ip) {
            payload = (payload << 8) | (octet & 0xFF);
        }
        payload = (payload << 16) | port;              // 48비트: IPv4 32 + 포트 16
        long value = (checksum(payload) << 48) | payload;   // 앞에 검사 2비트를 얹어 50비트

        char[] chars = new char[ADDRESS_LENGTH];
        for (int i = ADDRESS_LENGTH - 1; i >= 0; i--) {     // 뒤에서부터 5비트씩 꺼냅니다
            chars[i] = ALPHABET.charAt((int) (value & 0x1F));
            value >>>= 5;
        }
        return new String(chars) + newSecret();
    }

    /** 시크릿 {@value #SECRET_LENGTH}자 — 아무나 못 들어오게 막는 암호 부분입니다. */
    private static String newSecret() {
        StringBuilder secret = new StringBuilder(SECRET_LENGTH);
        for (int i = 0; i < SECRET_LENGTH; i++) {
            secret.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return secret.toString();
    }

    // ---- 해석 (참여자) ----

    /**
     * 사람이 입력한 코드를 풀어 방장 주소를 꺼냅니다.
     * 구분선({@code -})·공백·대소문자·{@code O/I/L} 혼동은 알아서 정리합니다.
     *
     * @throws IllegalArgumentException 형식이 틀린 경우 — 메시지를 화면에 그대로 보여 주면 됩니다
     */
    public static InviteCode parse(String raw) {
        String code = normalize(raw);
        if (code.isEmpty()) {
            throw new IllegalArgumentException("초대 코드를 입력해 주세요.");
        }
        if (code.length() != LENGTH) {
            throw new IllegalArgumentException(
                    "초대 코드는 " + LENGTH + "자입니다. (지금 " + code.length() + "자)");
        }

        long value = 0;
        for (int i = 0; i < ADDRESS_LENGTH; i++) {
            int index = ALPHABET.indexOf(code.charAt(i));
            if (index < 0) {
                throw new IllegalArgumentException(
                        "초대 코드에 쓸 수 없는 글자가 있습니다: " + code.charAt(i));
            }
            value = (value << 5) | index;
        }
        for (int i = ADDRESS_LENGTH; i < LENGTH; i++) {
            if (ALPHABET.indexOf(code.charAt(i)) < 0) {
                throw new IllegalArgumentException(
                        "초대 코드에 쓸 수 없는 글자가 있습니다: " + code.charAt(i));
            }
        }

        long payload = value & 0xFFFF_FFFF_FFFFL;
        if ((value >>> 48) != checksum(payload)) {
            throw new IllegalArgumentException("잘못 입력된 글자가 있습니다. 초대 코드를 다시 확인해 주세요.");
        }

        int port = (int) (payload & 0xFFFF);
        byte[] ip = new byte[4];
        long ipBits = payload >>> 16;
        for (int i = 3; i >= 0; i--) {
            ip[i] = (byte) (ipBits & 0xFF);
            ipBits >>>= 8;
        }
        if (port < 1 || ip[0] == 0) {
            throw new IllegalArgumentException("초대 코드에 담긴 접속 주소가 올바르지 않습니다.");
        }

        try {
            return new InviteCode(code, InetAddress.getByAddress(ip), port);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("초대 코드에 담긴 접속 주소가 올바르지 않습니다.");
        }
    }

    /**
     * 비교·전송에 쓰는 표준형으로 정리합니다 — 대문자로 올리고, 구분선·공백을 지우고,
     * 헷갈리기 쉬운 {@code O → 0}, {@code I·L → 1}로 바꿉니다.
     *
     * <p>형식 검사는 하지 않습니다(모르는 글자는 그대로 둡니다). 검사는 {@link #parse}가 합니다.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(LENGTH);
        for (int i = 0; i < raw.length(); i++) {
            char c = Character.toUpperCase(raw.charAt(i));
            if (c == '-' || c == '_' || Character.isWhitespace(c)) {
                continue;
            }
            if (c == 'O') {
                c = '0';
            } else if (c == 'I' || c == 'L') {
                c = '1';
            }
            normalized.append(c);
        }
        return normalized.toString();
    }

    /**
     * 코드에서 <b>주소부만</b> 잘라 냅니다 (앞 {@value #ADDRESS_LENGTH}자).
     *
     * <p>같은 {@code IP:포트}로 방을 두 개 열 수는 없으므로, 주소부는 그 자체로 방을 유일하게 가리킵니다.
     * 그래서 {@link RoomFinder}가 방을 찾을 때 이 값을 식별자로 씁니다 —
     * 암호 역할을 하는 시크릿을 네트워크에 흘리지 않으려는 것입니다.
     *
     * <p>형식 검사는 하지 않습니다. 짧은 코드가 들어오면 있는 만큼만 돌려줍니다.
     */
    public static String addressPart(String code) {
        String normalized = normalize(code);
        return normalized.length() <= ADDRESS_LENGTH
                ? normalized : normalized.substring(0, ADDRESS_LENGTH);
    }

    /** 화면·복사용 표기 — {@code 6WMK42A0P8Q3XN} → {@code 6WMK4-2A0P8-Q3XN} */
    public static String format(String code) {
        String normalized = normalize(code);
        if (normalized.length() != LENGTH) {
            return normalized;
        }
        return normalized.substring(0, 5) + "-"
                + normalized.substring(5, ADDRESS_LENGTH) + "-"
                + normalized.substring(ADDRESS_LENGTH);
    }

    /** 남는 2비트를 검사값으로 씁니다 — 6바이트를 더한 나머지. 코드가 길어지지 않습니다. */
    private static long checksum(long payload) {
        long sum = 0;
        for (int shift = 0; shift < 48; shift += 8) {
            sum += (payload >>> shift) & 0xFF;
        }
        return sum & 0b11;
    }

    // ---- 읽기 ----

    /** 서버에 그대로 보낼 표준형 코드 (구분선 없는 {@value #LENGTH}자) */
    public String code() {
        return code;
    }

    /** 이 코드의 주소부 — 방을 가리키는 식별자입니다. 자세한 설명은 {@link #addressPart(String)}. */
    public String addressPart() {
        return code.substring(0, ADDRESS_LENGTH);
    }

    public String host() {
        return host.getHostAddress();
    }

    public int port() {
        return port;
    }

    /** 화면 표시용 — 예) {@code 192.168.0.5:5000} */
    public String address() {
        return host() + ":" + port;
    }

    @Override
    public String toString() {
        return format(code);
    }
}
