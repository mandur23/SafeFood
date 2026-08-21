package com.safefood.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Scanner;

/**
 * 소켓 계층 콘솔 시험용 — UI 없이 서버·클라이언트 동작을 확인합니다.
 *
 * <pre>
 * 방장:   mvn compile exec:java -Dexec.mainClass=com.safefood.network.SocketMain -Dexec.args="host 민수"
 * 참여자: mvn compile exec:java -Dexec.mainClass=com.safefood.network.SocketMain -Dexec.args="join 6WMK4-2A0P8-Q3XN 동현"
 * </pre>
 *
 * <p>방장이 찍어 주는 초대 코드에 접속 주소가 들어 있으므로, 참여자는 주소를 따로 넘기지 않습니다.
 *
 * <p>접속 후 입력 — {@code /ready} 준비 완료, {@code /vote 번호} 투표, {@code /exit} 나가기,
 * 그 외 입력은 채팅으로 전송됩니다. 같은 PC에서 터미널을 여러 개 띄워 시험하세요.
 */
public final class SocketMain {

    public static void main(String[] args) throws IOException {
        GroupSession session = GroupSession.get();

        if (args.length >= 1 && args[0].equals("host")) {
            session.setDisplayName(args.length >= 2 ? args[1] : "방장");
            int port = SocketConfig.port();
            String inviteCode = InviteCode.issue(GroupServer.lanAddress(), port);
            session.hostRoom(port, inviteCode);
            System.out.println("초대 코드: " + InviteCode.format(inviteCode)
                    + "  (담긴 주소: " + GroupServer.lanAddress().getHostAddress() + ":" + port + ")");
            if (!session.server().discoverable()) {
                System.out.println("(자동 탐색이 꺼졌습니다 — 이 PC의 IP가 바뀌면 코드가 무효가 됩니다)");
            }
            session.client().join(inviteCode, session.displayName());
        } else if (args.length >= 2 && args[0].equals("join")) {
            session.setDisplayName(args.length >= 3 ? args[2] : "참여자");
            InviteCode invite;
            try {
                invite = InviteCode.parse(args[1]);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println("접속 시도: " + invite.address());
            if (!connect(session, invite)) {
                return;
            }
            session.client().join(invite.code(), session.displayName());
        } else {
            System.out.println("사용법: host [이름] | join 초대코드 [이름]");
            return;
        }

        session.setUiListener(new GroupClient.Listener() {
            @Override
            public void onMessage(Message message) {
                System.out.println("<< " + message.serialize());
            }

            @Override
            public void onDisconnected() {
                System.out.println("<< 서버와의 연결이 끊겼습니다.");
                System.exit(0);
            }
        });

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                GroupClient client = session.client();
                if (client == null || line.equals("/exit")) {
                    break;
                }
                if (line.equals("/ready")) {
                    client.sendInfo(List.of(), 5, 0);
                    client.sendReady();
                } else if (line.startsWith("/vote ")) {
                    try {
                        client.sendVote(Integer.parseInt(line.substring(6).trim()));
                    } catch (NumberFormatException e) {
                        System.out.println("사용법: /vote 후보번호");
                    }
                } else {
                    client.sendChat(line);
                }
            }
        }
        session.shutdown();
    }

    /**
     * 코드의 주소로 붙어 보고, 안 되면 같은 네트워크에서 방을 찾습니다 —
     * 참여 화면({@code GroupOptionController})과 같은 순서입니다.
     *
     * @return 접속했으면 true
     */
    private static boolean connect(GroupSession session, InviteCode invite) throws IOException {
        try {
            session.joinRoom(invite.host(), invite.port());
            return true;
        } catch (IOException direct) {
            System.out.println("코드의 주소에 닿지 않습니다. 같은 네트워크에서 방을 찾는 중…");
        }

        InetSocketAddress found = RoomFinder.find(invite.code()).orElse(null);
        if (found == null) {
            System.out.println("방을 찾지 못했습니다. 방이 열려 있는지, 같은 와이파이인지 확인하세요.");
            return false;
        }
        String host = found.getAddress().getHostAddress();
        System.out.println("방을 찾았습니다: " + host + ":" + found.getPort());
        session.joinRoom(host, found.getPort());
        return true;
    }
}
