package cz.zcu.fav.cryptedchat.client;

import cz.zcu.fav.cryptedchat.client.Client.OnDataReceiver;
import cz.zcu.fav.cryptedchat.crypto.Cypher;
import cz.zcu.fav.cryptedchat.crypto.RSA;
import cz.zcu.fav.cryptedchat.crypto.RSA.PublicKey;
import cz.zcu.fav.cryptedchat.crypto.SimpleCypher;
import cz.zcu.fav.cryptedchat.shared.BitUtils;
import cz.zcu.fav.cryptedchat.shared.MyPacket;
import cz.zcu.fav.cryptedchat.shared.MyPacket.Status;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Communicator implements OnDataReceiver {

    private final List<MyPacket> cache = new ArrayList<>();
    private final String ip;
    private final int port;
    private final Cypher cypherInput;
    private Cypher cypherOutput = new SimpleCypher();
    private Client client;

    private OnDisconnectListener disconnectListener;
    private OnConnectedListener connectedListener;
    private final Client.OnConnectedListener clientConnectedListener;

    private final Client.OnLostConnectionListener clientLostConnectionListener = () -> {
        if (disconnectListener != null) {
            disconnectListener.onDisconnect();
        }
    };
    private final Client.OnDisconnectListener clientDisconnectListener = () -> {
        if (disconnectListener != null) {
            disconnectListener.onDisconnect();
        }
    };

    public Communicator(String ip, int port) {
        this(ip, port, new SimpleCypher());
    }

    public Communicator(String ip, int port, Cypher cypher) {
        System.out.println("Vytvářím komunikátor");
        this.ip = ip;
        this.port = port;
        this.cypherInput = cypher;

        clientConnectedListener = () -> {
            if (cypher instanceof RSA) {
                PublicKey publicKey = ((RSA) cypher).getPublicKey();
                byte[][] data = publicKey.getRawData();
                List<MyPacket> packetN = MyPacket.buildPackets(data[PublicKey.INDEX_N], MyPacket.MESSAGE_PUBLIC_KEY_N);
                packetN.get(packetN.size() - 1).setStatus(Status.CONTINUE);
                List<MyPacket> packetE = MyPacket.buildPackets(data[PublicKey.INDEX_E], MyPacket.MESSAGE_PUBLIC_KEY_E);

                packetN.stream().forEach(packet -> sendPlainMessage(packet.toByteArray()));
                packetE.stream().forEach(packet -> sendPlainMessage(packet.toByteArray()));
            }
        };
    }

    private void processPacket(final List<MyPacket> packets, final byte messageId) {
        switch (messageId) {
            case MyPacket.MESSAGE_PUBLIC_KEY_N:
                final List<MyPacket> packetWithKeyE = packets.stream()
                    .filter(packet -> packet.hasMessageId(MyPacket.MESSAGE_PUBLIC_KEY_E))
                    .collect(Collectors.toList());
                final List<MyPacket> packetWithKeyN = packets.stream()
                    .filter(packet -> packet.hasMessageId(MyPacket.MESSAGE_PUBLIC_KEY_N))
                    .collect(Collectors.toList());

                final byte[] keyE = BitUtils.packetToDataArray(packetWithKeyE);
                final byte[] keyN = BitUtils.packetToDataArray(packetWithKeyN);

                cypherOutput = new RSA(new PublicKey(new BigInteger(keyN), new BigInteger(keyE)));

                System.out.println("Bylo přijato echo od serveru");
                break;
            case MyPacket.MESSAGE_SEND:

                break;
            default:
                System.out.println("Nebyl rozpoznán typ packetu");
        }
    }

    public void connect() {
        client = new Client(ip, port, this);
        client.setConnectedListener(clientConnectedListener);
        client.setLostConnectionListener(clientLostConnectionListener);
        client.setDisconnectListener(clientDisconnectListener);
        client.start();
    }

    public void disconnect() {
        client.kill();
        try {
            client.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendPlainMessage(byte[] message) {
        client.write(message);
    }

    public void sendMessage(byte[] message) {
        client.write(new MyPacket(cypherOutput.encrypt(message))
            .setMessageId(MyPacket.MESSAGE_SEND)
            .setLength(message.length)
            .toByteArray());

    }

    public void setConnectedListener(OnConnectedListener connectedListener) {
        this.connectedListener = connectedListener;
    }

    public void setDisconnectListener(OnDisconnectListener disconnectListener) {
        this.disconnectListener = disconnectListener;
    }

    @Override
    public void onReceive(final MyPacket packet) {
        cache.add(packet);

        if (packet.getStatus() == Status.END) {
            processPacket(cache, cache.get(0).getMessageId());
            cache.clear();
        }
    }

    public interface OnConnectedListener {
        void onConnected();
    }

    public interface OnDisconnectListener {
        void onDisconnect();
    }
}
