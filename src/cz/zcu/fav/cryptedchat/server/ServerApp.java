package cz.zcu.fav.cryptedchat.server;

import cz.zcu.fav.cryptedchat.crypto.Cypher;
import cz.zcu.fav.cryptedchat.crypto.RSA;
import cz.zcu.fav.cryptedchat.crypto.RSA.PublicKey;
import cz.zcu.fav.cryptedchat.server.Server.ServerHandler;
import cz.zcu.fav.cryptedchat.shared.BitUtils;
import cz.zcu.fav.cryptedchat.shared.MyPacket;
import cz.zcu.fav.cryptedchat.shared.MyPacket.Status;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ServerApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final String ACTION_EXIT = "exit";
    private final ConcurrentHashMap<Long, List<MyPacket>> packets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, PublicKey> clientsKeys = new ConcurrentHashMap<>();
    private final Cypher cypher = new RSA(1024);

    private final Server.ServerHandler handler = new ServerHandler() {
        @Override
        public void onClientConnected(long clientId) {
            System.out.println("Připojil se nový klient: " + clientId);
            packets.put(clientId, new ArrayList<>());
        }

        @Override
        public void onClientDisconnected(long clientId) {
            System.out.println("Klient " + clientId + " se odpojil");
            packets.remove(clientId);
            clientsKeys.remove(clientId);
        }

        @Override
        public void onDataReceived(MyPacket packet, long clientId) {
            System.out.println("Byla přijata data od klienta: " + clientId);
            List<MyPacket> clientPackets = packets.get(clientId);
            clientPackets.add(packet);

            if (packet.getStatus() == Status.END) {
                byte messageId = clientPackets.get(0).getMessageId();
                processPackets(clientPackets, messageId, clientId);
                clientPackets.clear();
            }
        }
    };

    private Server server;

    public static void main(String[] args) {
        new ServerApp().go();
    }

    private void exit() {
        System.out.println("Ukončuji aplikaci...");
        server.kill();
        try {
            server.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Aplikace ukončena");
    }

    private void go() {
        server = new Server(handler);
        server.start();

        boolean running = true;
        while (running) {
            String action = scanner.nextLine();

            switch (action) {
                case ACTION_EXIT:
                    exit();
                    running = false;
                    break;
                default:
                    System.out.println("Neznámý příkaz");
            }
        }
    }

    private void processPackets(final List<MyPacket> packets, final byte messageId, final long clientId) {
        switch (messageId) {
            case MyPacket.MESSAGE_ECHO:
                final byte[] dataCrypted = BitUtils.packetToDataArray(packets);
                final byte[] dataEncrypt = cypher.decrypt(dataCrypted);
                System.out.println(new String(dataEncrypt));
                break;
            case MyPacket.MESSAGE_PUBLIC_KEY_N:
                final List<MyPacket> packetWithKeyE = packets.stream()
                    .filter(packet -> packet.hasMessageId(MyPacket.MESSAGE_PUBLIC_KEY_E))
                    .collect(Collectors.toList());
                final List<MyPacket> packetWithKeyN = packets.stream()
                    .filter(packet -> packet.hasMessageId(MyPacket.MESSAGE_PUBLIC_KEY_N))
                    .collect(Collectors.toList());

                final byte[] keyE = BitUtils.packetToDataArray(packetWithKeyE);
                final byte[] keyN = BitUtils.packetToDataArray(packetWithKeyN);

                PublicKey publicKey = new PublicKey(new BigInteger(keyE), new BigInteger(keyN));
                clientsKeys.put(clientId, publicKey);
                server.assignPublicKey(clientId, publicKey);

                if (cypher instanceof RSA) {
                    PublicKey serverPublicKey = ((RSA) cypher).getPublicKey();
                    byte[][] data = serverPublicKey.getRawData();
                    List<MyPacket> packetN = MyPacket
                        .buildPackets(data[PublicKey.INDEX_N], MyPacket.MESSAGE_PUBLIC_KEY_N);
                    packetN.get(packetN.size() - 1).setStatus(Status.CONTINUE);
                    List<MyPacket> packetE = MyPacket
                        .buildPackets(data[PublicKey.INDEX_E], MyPacket.MESSAGE_PUBLIC_KEY_E);

                    server.writeTo(clientId, packetN);
                    server.writeTo(clientId, packetE);
                }

                break;
            default:
                System.out.printf("Nebyl rozpoznán typ packetu");
        }
    }
}
