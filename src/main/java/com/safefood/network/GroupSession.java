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
    private List<String> mergedLines = List.of();
    private List<CandidateView> candidates = List.of();
    private Map<Integer, Integer> voteCounts = Map.of();
    private String resultText;
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
        appendLog("서버 시작 (:" + port + ") — 초대 코드 " + inviteCode);
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
        mergedLines = List.of();
        candidates = List.of();
        voteCounts = Map.of();
        resultText = null;
        remoteClosed = false;
        disconnected = false;
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
