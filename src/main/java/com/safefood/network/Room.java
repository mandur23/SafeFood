package com.safefood.network;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 접속자 목록 · 브로드캐스트 · 투표 집계 — 방장 앱 안에만 존재하는 방 상태.
 *
 * <p>여러 {@link ClientHandler} 스레드가 동시에 호출하므로,
 * 상태를 읽고 쓰는 모든 메서드는 {@code synchronized}로 보호합니다.
 */
public final class Room {

    /** 참여자 1명의 서버 쪽 상태 (INFO로 받은 조건 포함) */
    private static final class Member {
        final String name;
        List<String> allergies = List.of();
        int spicyLevel = 5;   // 병합이 최솟값이라 초기값은 결과에 영향 없는 최대치
        int budgetMax = 0;    // 0 = 제한없음
        boolean ready;

        Member(String name) {
            this.name = name;
        }
    }

    /** 추천 후보 임시 목록 — RecommendService(개인 추천 담당)가 생기면 병합 조건을 넘겨 교체합니다. */
    private static final List<String[]> PLACEHOLDER_MENUS = List.of(
            new String[]{"김치찌개", "할머니손맛"},
            new String[]{"제육볶음", "백반집"},
            new String[]{"비빔밥", "한그릇"});

    private final String inviteCode;

    // 입장 순서 유지(첫 번째 = 방장). synchronized 밖에서는 절대 만지지 않습니다.
    private final Map<ClientHandler, Member> members = new LinkedHashMap<>();
    private final Map<ClientHandler, Integer> votes = new LinkedHashMap<>();

    private List<String> mergedLines = List.of();
    private List<String[]> candidates = List.of();   // {메뉴, 가게} — 번호는 순서 + 1
    private Message resultMessage;   // 확정 후에는 고정 — 재확정·재투표를 막는 기준
    private boolean closed;

    Room(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    /**
     * JOIN 처리 — 초대 코드가 맞는 사람만 받아들입니다 (README: 아무나 못 들어오게 막는 암호 역할).
     * 늦게 들어온 참여자에게는 현재 명단·병합 결과·후보·득표를 먼저 재전송해 화면을 맞춥니다.
     *
     * @return 입장 성공 여부. 실패하면 ERROR를 보낸 뒤 false — 호출한 쪽에서 연결을 끊습니다.
     */
    synchronized boolean join(ClientHandler handler, String code, String name) {
        if (closed) {
            handler.send(Message.of(Message.Type.ERROR, "이미 종료된 방입니다."));
            return false;
        }
        if (!inviteCode.equals(code)) {
            handler.send(Message.of(Message.Type.ERROR, "존재하지 않는 초대 코드"));
            return false;
        }
        if (name.isBlank()) {
            handler.send(Message.of(Message.Type.ERROR, "이름을 입력해 주세요."));
            return false;
        }
        if (members.values().stream().anyMatch(member -> member.name.equals(name))) {
            handler.send(Message.of(Message.Type.ERROR, "이미 사용 중인 이름입니다: " + name));
            return false;
        }

        int order = 0;
        for (Member member : members.values()) {
            handler.send(Message.of(Message.Type.JOINED, member.name, String.valueOf(++order)));
            if (member.ready) {
                handler.send(Message.of(Message.Type.READY, member.name));
            }
        }
        if (!mergedLines.isEmpty()) {
            handler.send(Message.of(Message.Type.MERGED, mergedLines));
        }
        if (!candidates.isEmpty()) {
            handler.send(candidatesMessage());
            handler.send(voteStatusMessage());
        }
        if (resultMessage != null) {
            handler.send(resultMessage);
        }

        members.put(handler, new Member(name));
        broadcast(Message.of(Message.Type.JOINED, name, String.valueOf(members.size())));
        return true;
    }

    /** INFO 처리 — 알레르기 / 매운맛 / 예산을 참여자 상태에 기록합니다. */
    synchronized void updateInfo(ClientHandler handler, List<String> allergies, int spicyLevel, int budgetMax) {
        Member member = members.get(handler);
        if (member == null) {
            return;
        }
        member.allergies = List.copyOf(allergies);
        member.spicyLevel = Math.max(0, Math.min(5, spicyLevel));
        member.budgetMax = Math.max(0, budgetMax);
    }

    /** READY 처리 — 전원이 준비되면 조건을 병합하고 후보를 발송합니다. */
    synchronized void markReady(ClientHandler handler) {
        Member member = members.get(handler);
        if (member == null || member.ready) {
            return;
        }
        member.ready = true;
        broadcast(Message.of(Message.Type.READY, member.name));
        if (members.values().stream().allMatch(m -> m.ready)) {
            startRecommendation();
        }
    }

    /** 전원 READY → 조건 병합 → 후보 발송. 병합은 README '그룹 추천 조건 병합 규칙'을 따릅니다. */
    private void startRecommendation() {
        if (resultMessage != null) {
            return;   // 이미 확정된 방 — 늦은 READY로 추천을 다시 돌리지 않습니다
        }
        Set<String> allergyUnion = new LinkedHashSet<>();
        int spicyMin = 5;
        int budgetMin = 0;
        for (Member member : members.values()) {
            allergyUnion.addAll(member.allergies);
            spicyMin = Math.min(spicyMin, member.spicyLevel);
            if (member.budgetMax > 0) {
                budgetMin = budgetMin == 0 ? member.budgetMax : Math.min(budgetMin, member.budgetMax);
            }
        }
        mergedLines = List.of(
                "알레르기 (합집합) — " + (allergyUnion.isEmpty() ? "없음"
                        : String.join(" · ", allergyUnion) + " 제외"),
                "매운맛 (최솟값) — " + spicyMin + "단계 이하",
                "예산 (교집합) — " + (budgetMin == 0 ? "제한없음" : String.format("%,d원 이하", budgetMin)));

        // TODO: 병합 조건으로 RecommendService를 호출해 실제 후보를 받도록 교체 (추천 담당 연결 지점)
        candidates = PLACEHOLDER_MENUS;
        votes.clear();

        broadcast(Message.of(Message.Type.MERGED, mergedLines));
        broadcast(candidatesMessage());
        broadcast(voteStatusMessage());
    }

    /** VOTE 처리 — 참여자당 1표, 다시 투표하면 표를 옮깁니다. 확정 후에는 받지 않습니다. */
    synchronized void vote(ClientHandler handler, int candidateNo) {
        if (!members.containsKey(handler) || candidates.isEmpty() || resultMessage != null) {
            return;
        }
        if (candidateNo < 1 || candidateNo > candidates.size()) {
            handler.send(Message.of(Message.Type.ERROR, "없는 후보 번호입니다: " + candidateNo));
            return;
        }
        votes.put(handler, candidateNo);
        broadcast(voteStatusMessage());
    }

    /** CHAT 처리 — 보낸 사람 이름을 붙여 전원(보낸 사람 포함)에게 중계합니다. */
    synchronized void chat(ClientHandler handler, String text) {
        Member member = members.get(handler);
        if (member == null || text.isBlank()) {
            return;
        }
        broadcast(Message.of(Message.Type.CHAT, member.name, text));
    }

    /**
     * 최다 득표 후보를 확정해 전원에게 RESULT를 보냅니다 (동표면 앞 번호 우선).
     * 방장 화면에서 직접 호출합니다 — 서버 객체는 방장 앱 안에만 있습니다.
     *
     * @return 표가 하나도 없으면 false
     */
    public synchronized boolean finishResult() {
        if (candidates.isEmpty() || votes.isEmpty()) {
            return false;
        }
        if (resultMessage != null) {
            return true;   // 이미 확정 — 다시 브로드캐스트하지 않습니다
        }
        int[] counts = tally();
        int best = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[best]) {
                best = i;
            }
        }
        String[] pick = candidates.get(best);
        resultMessage = Message.of(Message.Type.RESULT, pick[0], pick[1]);
        broadcast(resultMessage);
        return true;
    }

    /** 나감·끊김 처리 — 목록에서 제거하고 전원에게 LEFT를 알립니다. 표가 있었으면 집계도 갱신. */
    synchronized void leave(ClientHandler handler) {
        Member member = members.remove(handler);
        if (member == null || closed) {
            return;
        }
        boolean hadVote = votes.remove(handler) != null;
        broadcast(Message.of(Message.Type.LEFT, member.name, String.valueOf(members.size())));
        if (hadVote && resultMessage == null) {
            broadcast(voteStatusMessage());
        }
    }

    /** 방 종료 — 전원에게 알린 뒤 모든 소켓을 정리합니다 (README '방장 종료 처리'). */
    synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        broadcast(Message.of(Message.Type.CLOSED, "방장이 방을 종료했습니다."));
        for (ClientHandler handler : List.copyOf(members.keySet())) {
            handler.close();
        }
        members.clear();
        votes.clear();
    }

    private void broadcast(Message message) {
        for (ClientHandler handler : members.keySet()) {
            handler.send(message);
        }
    }

    /** 예) {@code CANDIDATES|1.김치찌개(할머니손맛),2.제육볶음(백반집),3.비빔밥(한그릇)} */
    private Message candidatesMessage() {
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            String[] candidate = candidates.get(i);
            csv.append(i + 1).append('.').append(candidate[0])
                    .append('(').append(candidate[1]).append(')');
        }
        return Message.of(Message.Type.CANDIDATES, csv.toString());
    }

    /** 예) {@code VOTE_STATUS|1:2,2:1,3:0} */
    private Message voteStatusMessage() {
        int[] counts = tally();
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < counts.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(i + 1).append(':').append(counts[i]);
        }
        return Message.of(Message.Type.VOTE_STATUS, csv.toString());
    }

    private int[] tally() {
        int[] counts = new int[candidates.size()];
        for (int no : votes.values()) {
            counts[no - 1]++;
        }
        return counts;
    }
}
