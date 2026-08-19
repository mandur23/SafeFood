package com.safefood.network;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * 소켓 계층 콘솔 시험용 — UI 없이 서버·클라이언트 동작을 확인합니다.
 *
 * <pre>
 * 방장:   mvn compile exec:java -Dexec.mainClass=com.safefood.network.SocketMain -Dexec.args="host 민수"
 * 참여자: mvn compile exec:java -Dexec.mainClass=com.safefood.network.SocketMain -Dexec.args="join localhost:5000 초대코드 동현"
 * </pre>
 *
 * <p>접속 후 입력 — {@code /ready} 준비 완료, {@code /vote 번호} 투표, {@code /exit} 나가기,
 * 그 외 입력은 채팅으로 전송됩니다. 같은 PC에서 터미널을 여러 개 띄워 localhost로 시험하세요.
 */
public final class SocketMain {

    public static void main(String[] args) throws IOException {
        GroupSession session = GroupSession.get();

        if (args.length >= 1 && args[0].equals("host")) {
            session.setDisplayName(args.length >= 2 ? args[1] : "방장");
            String inviteCode = GroupServer.newInviteCode();
            int port = SocketConfig.port();
            session.hostRoom(port, inviteCode);
            System.out.println("접속 주소: " + GroupServer.hostAddress() + ":" + port
                    + " / 초대 코드: " + inviteCode);
            session.client().join(inviteCode, session.displayName());
        } else if (args.length >= 3 && args[0].equals("join")) {
            session.setDisplayName(args.length >= 4 ? args[3] : "참여자");
            String[] address = args[1].split(":");
            int port = address.length > 1 ? Integer.parseInt(address[1]) : SocketConfig.DEFAULT_PORT;
            session.joinRoom(address[0], port);
            session.client().join(args[2], session.displayName());
        } else {
            System.out.println("사용법: host [이름] | join 주소:포트 초대코드 [이름]");
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
}
