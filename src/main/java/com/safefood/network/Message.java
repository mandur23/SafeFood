package com.safefood.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 소켓 프로토콜 메시지 — 한 줄이 한 메시지, {@code 타입|본문} 형식 (구분자 {@code |}).
 *
 * <p>README '메시지 프로토콜' 표가 기준입니다. 표의 타입에 더해, 구현하며 필요해진
 * 서버→클라이언트 타입을 추가했습니다.
 * <ul>
 *   <li>{@code CHAT|이름|내용} — 채팅 중계 (클라이언트가 보낸 CHAT을 전원에게)</li>
 *   <li>{@code READY|이름} — 누가 준비를 마쳤는지 알림</li>
 *   <li>{@code MERGED|줄1|줄2|…} — 조건 병합 결과 (대기실 표시용)</li>
 *   <li>{@code CLOSED|사유} — 방장이 방을 종료함</li>
 * </ul>
 */
public final class Message {

    public enum Type {
        // 클라이언트 → 서버
        JOIN, INFO, READY, VOTE, CHAT, EXIT,
        // 서버 → 클라이언트 (READY·CHAT은 중계할 때 서버도 보냅니다)
        JOINED, CANDIDATES, VOTE_STATUS, RESULT, LEFT, ERROR, MERGED, CLOSED
    }

    private final Type type;
    private final List<String> body;

    private Message(Type type, List<String> body) {
        this.type = type;
        this.body = List.copyOf(body);
    }

    public static Message of(Type type, String... body) {
        return new Message(type, Arrays.asList(body));
    }

    public static Message of(Type type, List<String> body) {
        return new Message(type, body);
    }

    /** 수신한 한 줄을 파싱합니다. 모르는 타입이거나 빈 줄이면 null (버리고 계속). */
    public static Message parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        // limit -1: "INFO||2|10000"처럼 비어 있는 칸도 자리를 지키게
        String[] parts = line.trim().split("\\|", -1);
        Type type;
        try {
            type = Type.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            return null;
        }
        List<String> body = new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));
        return new Message(type, body);
    }

    public Type type() {
        return type;
    }

    public List<String> body() {
        return body;
    }

    /** index번째 본문 칸. 없으면 빈 문자열 — 호출부의 널 검사를 줄입니다. */
    public String part(int index) {
        return index < body.size() ? body.get(index) : "";
    }

    /** index부터 끝까지를 다시 |로 이어 붙입니다 — 채팅처럼 본문에 |가 들어갈 수 있는 칸용. */
    public String rest(int index) {
        if (index >= body.size()) {
            return "";
        }
        return String.join("|", body.subList(index, body.size()));
    }

    /** 전송용 한 줄. 예) {@code JOIN|ABC123|홍길동} */
    public String serialize() {
        return body.isEmpty() ? type.name() : type.name() + "|" + String.join("|", body);
    }

    @Override
    public String toString() {
        return serialize();
    }
}
