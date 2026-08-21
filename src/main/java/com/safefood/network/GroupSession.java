package com.safefood.network;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 지금 참여 중인 그룹 세션 — 앱 전역 1개.
 *
 * <p>화면(다이얼로그)이 바뀌어도 접속·명단·로그·후보 상태가 유지되도록 여기에 모아 둡니다.
 * 수신 스레드가 이 클래스의 상태를 갱신하고, 화면은 {@link #setUiListener}로 알림만 받아
 * 스냅샷을 다시 그립니다. JavaFX에 의존하지 않으므로 콘솔 시험({@link SocketMain})에서도 그대로 씁니다.
 */
public final class GroupSession implements GroupClient.Listener {

    /** 화면 표시용 참여자 한 줄 — 입장 순서 첫 번째가 방장입니다. */
    public record MemberView(String name, boolean owner, boolean ready) {
    }

    /** 추천 후보 한 개 — CANDIDATES 메시지를 파싱한 것. */
    public record CandidateView(int no, String menu, String restaurant) {
    }

    /**
     * 대기실 화면에 흐르는 사건 한 줄 — 사람이 읽는 문장으로 바꿔 둔 것.
     *
     * <p>{@link #logLines()} 는 프로토콜 원문({@code JOINED|길동현})이라 개발용 콘솔 몫이고,
     * 이쪽은 말풍선·시스템 알림으로 그리기 위한 것입니다.
     */
    public record Event(EventKind kind, String sender, String text) {
    }

    public enum EventKind {
        /** 가운데 정렬 알림 — 참여·퇴장·준비 */
        SYSTEM,
        /** 좋은 소식 — 후보 도착, 최종 확정 */
        SYSTEM_GOOD,
        /** 조건 병합 완료 — 화면은 여기서 {@link #mergedLines()} 를 카드로 펼칩니다 */
        MERGED,
        /** 나쁜 소식 — 오류, 방 종료, 연결 끊김 */
        SYSTEM_BAD,
        /** 사람이 보낸 채팅 — 말풍선 */
        CHAT
    }

    private static final GroupSession INSTANCE = new GroupSession();
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static GroupSession get() {
        return INSTANCE;
    }

    private String displayName = "게스트";
    private boolean guest = true;   // 로그인 방식 — 방을 나간 뒤 돌아갈 화면을 정할 때 씁니다

    private GroupServer server;   // 방장일 때만 존재
    private GroupClient client;
    private volatile GroupClient.Listener uiListener;

    private final List<String> memberOrder = new ArrayList<>();
    private final Set<String> readyNames = new HashSet<>();
    private final List<String> logs = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private List<String> mergedLines = List.of();
    private List<CandidateView> candidates = List.of();
    private Map<Integer, Integer> voteCounts = Map.of();
    private String resultText;
    private String inviteCode = "";   // 대기실 머리에 다시 보여 주기 위해 들고 있습니다
    private boolean remoteClosed;
    private boolean disconnected;

    private GroupSession() {
    }

    // ---- 시작 · 종료 ----

    /** 방장: 서버를 열고, 자기 서버에 클라이언트로 접속합니다 (README — 방장도 자기 서버에 접속). */
    public synchronized void hostRoom(int port, String inviteCode) throws IOException {
        shutdown();
        server = GroupServer.open(port, inviteCode);
        try {
            client = GroupClient.connect("localhost", port);
        } catch (IOException e) {
            server.stop();
            server = null;
            throw e;
        }
        client.setListener(this);
        this.inviteCode = inviteCode;
        appendLog("서버 시작 (:" + port + ") — 초대 코드 " + inviteCode);
        events.add(good("방이 열렸어요 — 초대 코드를 일행에게 보내세요"));
    }

    /** 참여자: 방장 주소로 접속만 합니다. JOIN 전송은 {@code client().join(...)}으로 이어서. */
    public synchronized void joinRoom(String host, int port) throws IOException {
        shutdown();
        client = GroupClient.connect(host, port);
        client.setListener(this);
    }

    /** 서버·클라이언트를 정리하고 상태를 비웁니다. 여러 번 불러도 안전합니다. */
    public synchronized void shutdown() {
        uiListener = null;
        if (server != null) {
            server.stop();   // 참여자에게 CLOSED를 알린 뒤 소켓 정리
            server = null;
        }
        if (client != null) {
            client.close();
            client = null;
        }
        memberOrder.clear();
        readyNames.clear();
        logs.clear();
        events.clear();
        mergedLines = List.of();
        candidates = List.of();
        voteCounts = Map.of();
        resultText = null;
        inviteCode = "";
        remoteClosed = false;
        disconnected = false;
    }

    /** 참여자 쪽에서 방에 붙을 때 쓴 코드를 기록해 둡니다 (방장은 {@link #hostRoom}이 채웁니다). */
    public synchronized void setInviteCode(String code) {
        inviteCode = code == null ? "" : code;
    }

    /** 이 방의 초대 코드. 모르면 빈 문자열. */
    public synchronized String inviteCode() {
        return inviteCode;
    }

    // ---- 수신 처리 (GroupClient 수신 스레드에서 호출됩니다) ----

    @Override
    public void onMessage(Message message) {
        synchronized (this) {
            appendLog(message.serialize());
            switch (message.type()) {
                case JOINED -> {
                    String name = message.part(0);
                    if (!memberOrder.contains(name)) {
                        memberOrder.add(name);
                    }
                }
                case LEFT -> {
                    memberOrder.remove(message.part(0));
                    readyNames.remove(message.part(0));
                }
                case READY -> readyNames.add(message.part(0));
                case MERGED -> mergedLines = List.copyOf(message.body());
                case CANDIDATES -> candidates = parseCandidates(message.part(0));
                case VOTE_STATUS -> voteCounts = parseVoteStatus(message.part(0));
                case RESULT -> resultText = message.part(0)
                        + (message.part(1).isEmpty() ? "" : " (" + message.part(1) + ")");
                case CLOSED -> remoteClosed = true;
                default -> { }
            }
            recordEvent(message);   // 명단·후보가 갱신된 뒤라 "(3명)" 같은 문구를 바로 쓸 수 있습니다
        }
        GroupClient.Listener current = uiListener;
        if (current != null) {
            current.onMessage(message);
        }
    }

    @Override
    public void onDisconnected() {
        boolean notify;
        synchronized (this) {
            disconnected = true;
            // CLOSED를 이미 받았으면 곧이어 오는 소켓 종료는 중복 알림이라 생략
            notify = !remoteClosed;
            if (notify) {
                appendLog("서버와의 연결이 끊겼습니다.");
                events.add(bad("서버와의 연결이 끊겼어요"));
            }
        }
        if (!notify) {
            return;
        }
        GroupClient.Listener current = uiListener;
        if (current != null) {
            current.onDisconnected();
        }
    }

    // ---- 화면이 읽어 가는 스냅샷 ----

    public synchronized List<MemberView> members() {
        List<MemberView> list = new ArrayList<>();
        for (int i = 0; i < memberOrder.size(); i++) {
            String name = memberOrder.get(i);
            list.add(new MemberView(name, i == 0, readyNames.contains(name)));
        }
        return list;
    }

    public synchronized List<String> logLines() {
        return List.copyOf(logs);
    }

    /** 대기실 말풍선·시스템 알림용 사건 목록. */
    public synchronized List<Event> events() {
        return List.copyOf(events);
    }

    public synchronized List<String> mergedLines() {
        return mergedLines;
    }

    public synchronized List<CandidateView> candidates() {
        return candidates;
    }

    public synchronized int votesFor(int candidateNo) {
        return voteCounts.getOrDefault(candidateNo, 0);
    }

    public synchronized int totalVotes() {
        int total = 0;
        for (int count : voteCounts.values()) {
            total += count;
        }
        return total;
    }

    /** 확정된 최종 메뉴. 아직 확정 전이면 null. */
    public synchronized String result() {
        return resultText;
    }

    // ---- 접속 상태 ----

    public synchronized GroupClient client() {
        return client;
    }

    public synchronized GroupServer server() {
        return server;
    }

    public synchronized boolean isOwner() {
        return server != null;
    }

    public synchronized boolean isConnected() {
        return client != null;
    }

    public synchronized boolean isRemoteClosed() {
        return remoteClosed;
    }

    /**
     * 방이 이미 끝난 세션인지 — 화면 전환 사이에 CLOSED·끊김이 도착해 알림을 놓친 경우를
     * 대기실이 입장 시점에 감지할 때 씁니다. 접속한 적 없는 상태(화면 미리보기)는 false.
     */
    public synchronized boolean isDead() {
        return remoteClosed || disconnected || (client != null && !client.isAlive());
    }

    public synchronized String displayName() {
        return displayName;
    }

    /** 로그인(회원 닉네임) 또는 게스트 이름 입력 때 설정합니다. */
    public synchronized void setDisplayName(String name) {
        if (name != null && !name.isBlank()) {
            displayName = name.trim();
        }
    }

    public synchronized boolean isGuest() {
        return guest;
    }

    /** 회원 로그인 시 false, 게스트 입장 시 true — 방을 나간 뒤 돌아갈 화면이 달라집니다. */
    public synchronized void setGuest(boolean guest) {
        this.guest = guest;
    }

    public void setUiListener(GroupClient.Listener listener) {
        uiListener = listener;
    }

    private void appendLog(String text) {
        logs.add("[" + LocalTime.now().format(LOG_TIME) + "] " + text);
    }

    /**
     * 프로토콜 메시지 한 줄을 사람이 읽는 문장으로 옮겨 {@link #events} 에 쌓습니다.
     *
     * <p>화면에 흐름으로 보여 줄 값이 없는 타입(내가 보낸 JOIN·INFO·VOTE 등)은 건너뜁니다 —
     * 원문이 필요하면 개발용 콘솔이 {@link #logLines()} 를 그립니다.
     */
    private void recordEvent(Message message) {
        switch (message.type()) {
            case CHAT -> events.add(
                    new Event(EventKind.CHAT, message.part(0), message.rest(1)));
            case JOINED -> events.add(system(
                    message.part(0) + "님이 참여했어요 (" + memberOrder.size() + "명)"));
            case LEFT -> events.add(system(message.part(0) + "님이 나갔어요"));
            case READY -> events.add(system(
                    message.part(0) + "님이 준비를 마쳤어요 ("
                            + readyNames.size() + "/" + memberOrder.size() + ")"));
            case MERGED -> events.add(new Event(
                    EventKind.MERGED, "", "✓ 전원 준비 완료 — 조건을 합쳐 후보를 뽑았어요"));
            case CANDIDATES -> events.add(good(
                    "추천 후보 " + candidates.size() + "개가 도착했어요"));
            case VOTE_STATUS -> {
                // 프로토콜이 '누가' 투표했는지는 싣지 않아 총계로만 알립니다
                int total = 0;
                for (int count : voteCounts.values()) {
                    total += count;
                }
                if (total > 0) {
                    events.add(system("투표가 접수되었어요 (" + total + "/" + memberOrder.size() + ")"));
                }
            }
            case RESULT -> events.add(good("최종 메뉴 확정 — " + resultText));
            case ERROR -> events.add(bad(message.part(0)));
            case CLOSED -> events.add(bad(message.part(0).isEmpty()
                    ? "방장이 방을 종료했어요" : message.part(0)));
            default -> { }
        }
    }

    private static Event system(String text) {
        return new Event(EventKind.SYSTEM, "", text);
    }

    private static Event good(String text) {
        return new Event(EventKind.SYSTEM_GOOD, "", text);
    }

    private static Event bad(String text) {
        return new Event(EventKind.SYSTEM_BAD, "", text);
    }

    /** {@code "1.김치찌개(할머니손맛),2.제육볶음(백반집)"} → 후보 목록 */
    private static List<CandidateView> parseCandidates(String csv) {
        List<CandidateView> list = new ArrayList<>();
        for (String token : csv.split(",")) {
            token = token.trim();
            int dot = token.indexOf('.');
            if (dot < 1) {
                continue;
            }
            int no;
            try {
                no = Integer.parseInt(token.substring(0, dot));
            } catch (NumberFormatException e) {
                continue;
            }
            String rest = token.substring(dot + 1);
            String menu = rest;
            String restaurant = "";
            int open = rest.lastIndexOf('(');
            if (open > 0 && rest.endsWith(")")) {
                menu = rest.substring(0, open);
                restaurant = rest.substring(open + 1, rest.length() - 1);
            }
            list.add(new CandidateView(no, menu, restaurant));
        }
        return List.copyOf(list);
    }

    /** {@code "1:2,2:1,3:0"} → 후보 번호별 득표 수 */
    private static Map<Integer, Integer> parseVoteStatus(String csv) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (String token : csv.split(",")) {
            String[] pair = token.trim().split(":");
            if (pair.length != 2) {
                continue;
            }
            try {
                counts.put(Integer.parseInt(pair[0].trim()), Integer.parseInt(pair[1].trim()));
            } catch (NumberFormatException e) {
                // 형식이 틀린 항목은 건너뜀
            }
        }
        return counts;
    }
}
