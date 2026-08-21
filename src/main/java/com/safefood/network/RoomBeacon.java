package com.safefood.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 방 자동 탐색 응답기 — <b>"이 코드의 방, 지금 어디 있나요?"</b> 라는 질의에 방장이 대답합니다.
 *
 * <p>초대 코드에는 <b>발급하던 순간의 주소</b>가 굳어 있습니다({@link InviteCode}).
 * 방장이 와이파이를 다시 잡거나 DHCP 임대가 갱신돼 IP가 바뀌면 그 주소는 낡은 값이 되고,
 * 참여자가 코드를 제대로 받아 적었는데도 접속에 실패합니다.
 * 이 비콘은 <b>그 상황의 보험</b>입니다 — 코드는 그대로 두고 주소만 다시 찾게 해 줍니다.
 *
 * <pre>
 *   참여자                            (같은 네트워크)                     방장
 *     │                                                                   │
 *     ├─ SAFEFOOD1-FIND|6WMK42A0P8 ──▶ 멀티캐스트 + 브로드캐스트 ────────▶ │  모든 방장이 수신
 *     │                                                                   ├─ 내 코드의 주소부와 대조
 *     │                                                                   │   다르면 → 무응답
 *     │◀──── SAFEFOOD1-HERE|5000 ────── 물어본 사람에게만 유니캐스트 ──────┤   같으면 → 응답
 *     │                                                                   │
 *     └─ 응답 패킷의 출발지 IP = 방장의 '지금' 주소
 * </pre>
 *
 * <p>설계에서 지킨 두 가지
 * <ul>
 *   <li><b>시크릿은 절대 싣지 않습니다.</b> 방을 가리키는 값으로 코드의 <b>주소부 10자만</b> 씁니다.
 *       주소부는 IP 헤더에 어차피 드러나는 정보라 도청해도 새로 얻는 게 없고,
 *       암호 역할을 하는 뒤 4자는 TCP {@code JOIN}에서 {@link Room}이 따로 검사합니다.</li>
 *   <li><b>응답 본문에 IP를 넣지 않습니다.</b> UDP 응답의 출발지 주소가 곧 방장의 현재 주소라,
 *       참여자는 {@code DatagramPacket.getAddress()}로 바로 알아냅니다. 포트만 실어 보냅니다.</li>
 * </ul>
 *
 * <p>주소부는 {@code IP:포트}를 그대로 인코딩한 값이라 <b>방을 유일하게 가리킵니다</b> —
 * 같은 주소·포트로 방을 두 개 열 수는 없기 때문입니다. 코드 충돌을 따로 걱정하지 않아도 됩니다.
 */
final class RoomBeacon implements Closeable {

    /**
     * 탐색용 UDP 포트 — 방장과 참여자가 <b>반드시 같은 값</b>을 써야 하므로
     * {@code config.properties}로 빼지 않고 상수로 고정했습니다.
     * (TCP 포트와 번호가 겹쳐도 프로토콜이 달라 충돌하지 않습니다.)
     */
    static final int DISCOVERY_PORT = 5001;

    /** 관리용으로 열려 있는 로컬 멀티캐스트 대역(239.0.0.0/8). TTL 1이라 공유기를 넘지 않습니다. */
    static final String GROUP_ADDRESS = "239.255.42.99";

    /** 프로토콜 이름 + 판 번호 — 나중에 형식을 바꿔도 옛 버전과 섞이지 않게. */
    static final String ASK = "SAFEFOOD1-FIND";
    static final String ANSWER = "SAFEFOOD1-HERE";

    /** 주고받는 줄이 짧아서 넉넉합니다. 넘치는 패킷은 잘린 채로 대조에 실패해 무시됩니다. */
    static final int MAX_PACKET = 128;

    private final MulticastSocket socket;
    private final String addressPart;
    private final int tcpPort;

    private RoomBeacon(MulticastSocket socket, String addressPart, int tcpPort) {
        this.socket = socket;
        this.addressPart = addressPart;
        this.tcpPort = tcpPort;
    }

    /**
     * 탐색 포트를 열고 응답 스레드를 시작합니다.
     *
     * @param inviteCode 이 방의 초대 코드 — 주소부만 꺼내 쓰고 시크릿은 보관하지 않습니다
     * @param tcpPort    응답에 실어 보낼 실제 접속 포트
     * @throws IOException 탐색 포트를 열지 못한 경우. <b>방 자체는 정상 동작하므로</b>
     *                     호출한 쪽에서 안내만 하고 계속 진행하세요.
     */
    static RoomBeacon start(String inviteCode, int tcpPort) throws IOException {
        // MulticastSocket 생성자가 SO_REUSEADDR을 켜 줍니다 —
        // 한 PC에서 방장 앱을 여러 개 띄우는 시험(README)에서도 포트가 겹치지 않습니다
        MulticastSocket socket = new MulticastSocket(DISCOVERY_PORT);
        try {
            InetSocketAddress group =
                    new InetSocketAddress(InetAddress.getByName(GROUP_ADDRESS), DISCOVERY_PORT);
            for (NetworkInterface nic : discoveryInterfaces()) {
                try {
                    socket.joinGroup(group, nic);
                } catch (IOException ignored) {
                    // 이 어댑터로는 멀티캐스트를 못 받습니다 — 다른 어댑터나 브로드캐스트가 커버합니다
                }
            }
        } catch (IOException e) {
            socket.close();   // 열어 둔 포트를 물고 늘어지지 않게
            throw e;
        }

        RoomBeacon beacon = new RoomBeacon(socket, InviteCode.addressPart(inviteCode), tcpPort);
        Thread responder = new Thread(beacon::listenLoop, "room-beacon");
        responder.setDaemon(true);
        responder.start();
        return beacon;
    }

    /** 질의를 기다렸다가 내 방을 찾는 것만 골라 답합니다. {@code receive()}는 올 때까지 블로킹됩니다. */
    private void listenLoop() {
        byte[] buffer = new byte[MAX_PACKET];
        while (!socket.isClosed()) {
            try {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String asked = new String(request.getData(), request.getOffset(),
                        request.getLength(), StandardCharsets.UTF_8).trim();
                if (!asked.equals(ASK + "|" + addressPart)) {
                    continue;   // 다른 방을 찾는 질의 — 응답조차 하지 않아 방의 존재를 숨깁니다
                }

                byte[] answer = (ANSWER + "|" + tcpPort).getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(answer, answer.length,
                        request.getAddress(), request.getPort()));
            } catch (IOException e) {
                if (socket.isClosed()) {
                    return;   // close()로 닫힘 — 정상 종료
                }
                System.err.println("[beacon] 탐색 응답 실패: " + e.getMessage());
            }
        }
    }

    /** 멀티캐스트를 쓸 수 있는 실제 어댑터 목록 — 방장(참여)과 참여자(질의)가 함께 씁니다. */
    static List<NetworkInterface> discoveryInterfaces() {
        List<NetworkInterface> usable = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (nic.isUp() && !nic.isLoopback() && nic.supportsMulticast()) {
                    usable.add(nic);
                }
            }
        } catch (SocketException e) {
            System.err.println("[discovery] 네트워크 어댑터를 읽지 못했습니다: " + e.getMessage());
        }
        return usable;
    }

    /** 소켓을 닫으면 {@code receive()}가 깨어나 응답 스레드가 끝납니다. 여러 번 불러도 안전합니다. */
    @Override
    public void close() {
        socket.close();
    }
}
