package com.safefood.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 참여자 1명당 스레드 1개 — 소켓에서 줄 단위로 읽어 {@link Room}에 위임합니다.
 *
 * <p>첫 메시지는 반드시 {@code JOIN|초대코드|이름}이어야 하고, 실패하면 연결을 끊습니다.
 * 창을 그냥 닫는 등 비정상 종료는 {@link IOException}으로 감지해 LEFT 처리로 이어집니다.
 */
final class ClientHandler implements Runnable {

    private final Socket socket;
    private final Room room;
    private final BufferedReader in;
    private final BufferedWriter out;
    private boolean joined;

    ClientHandler(Socket socket, Room room) throws IOException {
        this.socket = socket;
        this.room = room;
        // 인코딩을 명시하지 않으면 OS 기본값에 따라 한글이 깨집니다
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message message = Message.parse(line);
                if (message == null) {
                    continue;
                }
                if (!joined) {
                    if (message.type() != Message.Type.JOIN) {
                        send(Message.of(Message.Type.ERROR, "먼저 JOIN|초대코드|이름 을 보내야 합니다."));
                        return;
                    }
                    joined = room.join(this, message.part(0), message.part(1).trim());
                    if (!joined) {
                        return;
                    }
                    continue;
                }
                switch (message.type()) {
                    case INFO -> room.updateInfo(this,
                            splitCsv(message.part(0)),
                            parseInt(message.part(1), 5),
                            parseInt(message.part(2), 0));
                    case READY -> room.markReady(this);
                    case VOTE -> room.vote(this, parseInt(message.part(0), 0));
                    case CHAT -> room.chat(this, message.rest(0));
                    case EXIT -> {
                        return;
                    }
                    default -> { }   // 서버가 받을 일 없는 타입은 무시
                }
            }
        } catch (IOException e) {
            // 비정상 끊김 — finally의 leave()가 LEFT 브로드캐스트로 처리
        } finally {
            if (joined) {
                room.leave(this);
            }
            close();
        }
    }

    /** 이 참여자에게 한 줄 전송. 끊긴 상대면 조용히 무시합니다 — 정리는 읽기 쪽에서. */
    void send(Message message) {
        synchronized (out) {
            try {
                out.write(message.serialize());
                out.newLine();
                out.flush();
            } catch (IOException ignored) {
            }
        }
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private static List<String> splitCsv(String csv) {
        List<String> values = new ArrayList<>();
        for (String token : csv.split(",")) {
            if (!token.isBlank()) {
                values.add(token.trim());
            }
        }
        return values;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
