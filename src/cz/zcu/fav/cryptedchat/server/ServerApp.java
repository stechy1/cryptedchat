package cz.zcu.fav.cryptedchat.server;

import cz.zcu.fav.cryptedchat.crypto.Cypher;
import cz.zcu.fav.cryptedchat.crypto.RSA;
import cz.zcu.fav.cryptedchat.crypto.RSA.PublicKey;
import cz.zcu.fav.cryptedchat.server.Server.ServerHandler;
import cz.zcu.fav.cryptedchat.shared.BitUtils;
import cz.zcu.fav.cryptedchat.shared.ClientState;
import cz.zcu.fav.cryptedchat.shared.MyPacket;
import cz.zcu.fav.cryptedchat.shared.MyPacket.Status;
import cz.zcu.fav.cryptedchat.shared.Pair;
import cz.zcu.fav.cryptedchat.shared.message.PacketWithContacts;
import cz.zcu.fav.cryptedchat.shared.message.PacketWithPublicKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

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
            MyPacket packet = new MyPacket().setMessageId(MyPacket.MESSAGE_USER_STATE_CHANGED);
            packet.addData(new byte[]{(byte) ClientState.ONLINE.ordinal()});
            final byte[] longData = new byte[Long.BYTES];
            BitUtils.longToBytes(clientId, longData, 0);
            packet.addData(longData);
            server.sendBroadcast(clientId, packet);
        }

        @Override
        public void onClientDisconnected(long clientId) {
            System.out.println("Klient " + clientId + " se odpojil");
            packets.remove(clientId);
            clientsKeys.remove(clientId);
            MyPacket packet = new MyPacket().setMessageId(MyPacket.MESSAGE_USER_STATE_CHANGED);
            packet.addData(new byte[]{(byte) ClientState.OFFLINE.ordinal()});
            final byte[] longData = new byte[Long.BYTES];
            BitUtils.longToBytes(clientId, longData, 0);
            packet.addData(longData);
            server.sendBroadcast(clientId, packet);
        }

        @Override
        public void onDataReceived(MyPacket packet, long clientId) {
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
            case MyPacket.MESSAGE_SEND:
                final byte[] dataCrypted = MyPacket.packetToDataArray(packets);
                final byte[] dataEncrypt = cypher.decrypt(dataCrypted);
                System.out.println(new String(dataEncrypt));
                break;
            case MyPacket.MESSAGE_PUBLIC_KEY_N:
                final PublicKey publicKey = PacketWithPublicKey.getPublicKey(packets);
                clientsKeys.put(clientId, publicKey);
                server.assignPublicKey(clientId, publicKey);

                if (cypher instanceof RSA) {
                    PublicKey serverPublicKey = ((RSA) cypher).getPublicKey();
                    Pair<List<MyPacket>, List<MyPacket>> packetsWithPublicKey = PacketWithPublicKey.getPackets(serverPublicKey);

                    server.writeTo(clientId, packetsWithPublicKey.first);
                    server.writeTo(clientId, packetsWithPublicKey.second);
                }
                break;
            case MyPacket.MESSAGE_CONTACTS:
                Enumeration<Long> users = this.packets.keys();
                List<MyPacket> ids = PacketWithContacts.getPackets(users, clientId);

                server.writeTo(clientId, ids);
                break;
            default:
                System.out.printf("Nebyl rozpoznán typ packetu");
        }
    }
}
