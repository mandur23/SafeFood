package com.safefood.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 소켓 클라이언트 — 접속 후 <b>수신 전용 데몬 스레드</b>를 띄웁니다.
 * (입력을 기다리는 동안에도 서버가 보낸 메시지를 받아야 하므로)
 *
 * <p>방장도 자기 서버에 이 클래스로 접속합니다 — 방장과 참여자의 화면 로직이 같아집니다.
 */
public final class GroupClient implements Closeable {

    /** 수신 스레드에서 호출됩니다 — JavaFX 화면을 고칠 때는 {@code Platform.runLater}로 감싸세요. */
    public interface Listener {

        void onMessage(Message message);

        /** CLOSED 안내 없이 연결이 끊겼을 때 (서버 강제 종료, 네트워크 단절 등) */
        void onDisconnected();
    }

    private static final int CONNECT_TIMEOUT_MS = 3_000;

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private volatile Listener listener;
    private volatile boolean closedByMe;
    private volatile boolean disconnected;

    private GroupClient(Socket socket) throws IOException {
        this.socket = socket;
        // 인코딩을 명시하지 않으면 OS 기본값에 따라 한글이 깨집니다
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    /** 방장 주소로 접속하고 수신 스레드를 시작합니다. {@value #CONNECT_TIMEOUT_MS}ms 안에 못 붙으면 실패. */
    public static GroupClient connect(String host, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
        GroupClient client = new GroupClient(socket);
        Thread receiver = new Thread(client::receiveLoop, "group-client-recv");
        receiver.setDaemon(true);
        receiver.start();
        return client;
    }

    private void receiveLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message message = Message.parse(line);
                Listener current = listener;
                if (message != null && current != null) {
                    current.onMessage(message);
                }
            }
        } catch (IOException e) {
            // 아래 공통 끊김 처리로
        }
        disconnected = true;
        Listener current = listener;
        if (!closedByMe && current != null) {
            current.onDisconnected();
        }
    }

    /** 수신 루프가 살아 있고 소켓도 열려 있는지 — 화면이 죽은 연결을 감지할 때 씁니다. */
    public boolean isAlive() {
        return !disconnected && !socket.isClosed();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    // ---- 보내기 (README 프로토콜의 클라이언트 → 서버) ----

    public void join(String inviteCode, String name) {
        send(Message.of(Message.Type.JOIN, inviteCode, name));
    }

    public void sendInfo(List<String> allergies, int spicyLevel, int budgetMax) {
        send(Message.of(Message.Type.INFO,
                String.join(",", allergies),
                String.valueOf(spicyLevel),
                String.valueOf(budgetMax)));
    }

    public void sendReady() {
        send(Message.of(Message.Type.READY));
    }

    public void sendVote(int candidateNo) {
        send(Message.of(Message.Type.VOTE, String.valueOf(candidateNo)));
    }

    public void sendChat(String text) {
        send(Message.of(Message.Type.CHAT, text));
    }

    private void send(Message message) {
        synchronized (out) {
            try {
                out.write(message.serialize());
                out.newLine();
                out.flush();
            } catch (IOException e) {
                System.err.println("[client] 전송 실패: " + e.getMessage());
            }
        }
    }

    /** EXIT을 보내고 소켓을 닫습니다. 서버가 EXIT을 못 받아도 소켓 종료로 감지됩니다. */
    @Override
    public void close() {
        closedByMe = true;   // 내가 닫은 경우에는 onDisconnected를 부르지 않기 위한 표시
        send(Message.of(Message.Type.EXIT));
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
