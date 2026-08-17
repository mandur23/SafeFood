package com.safefood.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;

/**
 * 방장 앱이 여는 서버 — 방을 만드는 순간 시작되고, 따로 켜 두는 중앙 서버는 없습니다.
 *
 * <p>{@code accept()}는 접속이 올 때까지 블로킹되므로, 방장 화면이 멈추지 않도록
 * 수락 루프는 별도 데몬 스레드에서 돕니다.
 */
public final class GroupServer {

    // 헷갈리는 I·L·O·0·1은 뺐습니다 — 초대 코드는 눈으로 보고 옮겨 적는 값이라서
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ServerSocket serverSocket;
    private final Room room;

    private GroupServer(ServerSocket serverSocket, Room room) {
        this.serverSocket = serverSocket;
        this.room = room;
    }

    /**
     * 서버를 열고 수락 스레드를 시작합니다.
     *
     * @throws IOException 포트가 이미 사용 중이면 {@link java.net.BindException} —
     *                     호출한 쪽에서 안내하고 config.properties의 socket.port를 바꾸게 하세요.
     */
    public static GroupServer open(int port, String inviteCode) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        GroupServer server = new GroupServer(serverSocket, new Room(inviteCode));
        Thread acceptor = new Thread(server::acceptLoop, "group-server-accept");
        acceptor.setDaemon(true);
        acceptor.start();
        return server;
    }

    private void acceptLoop() {
        int connectionNo = 0;
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                Thread handler = new Thread(new ClientHandler(socket, room),
                        "group-client-handler-" + ++connectionNo);
                handler.setDaemon(true);
                handler.start();
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    return;   // stop()으로 닫힘 — 정상 종료
                }
                System.err.println("[server] 접속 수락 실패: " + e.getMessage());
            }
        }
    }

    public Room room() {
        return room;
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    /** 초대 코드 생성 — 아무나 못 들어오게 막는 암호 역할 (README). */
    public static String newInviteCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    /** 화면에 표시할 이 PC의 접속 주소 — 방장이 일행에게 '주소 + 초대 코드'를 알려 줍니다. */
    public static String hostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    /** 참여자 전원에게 알린 뒤 모든 소켓을 정리하고 서버를 내립니다. */
    public void stop() {
        room.close();
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
    }
}
