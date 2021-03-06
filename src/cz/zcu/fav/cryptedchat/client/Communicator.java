package cz.zcu.fav.cryptedchat.client;

import cz.zcu.fav.cryptedchat.client.Client.OnDataReceiver;
import cz.zcu.fav.cryptedchat.crypto.Cypher;
import cz.zcu.fav.cryptedchat.crypto.RSA;
import cz.zcu.fav.cryptedchat.crypto.RSA.PublicKey;
import cz.zcu.fav.cryptedchat.crypto.SimpleCypher;
import cz.zcu.fav.cryptedchat.shared.BitUtils;
import cz.zcu.fav.cryptedchat.shared.ClientState;
import cz.zcu.fav.cryptedchat.shared.MyPacket;
import cz.zcu.fav.cryptedchat.shared.MyPacket.Status;
import cz.zcu.fav.cryptedchat.shared.Pair;
import cz.zcu.fav.cryptedchat.shared.message.PacketWithContacts;
import cz.zcu.fav.cryptedchat.shared.message.PacketWithPublicKey;
import cz.zcu.fav.cryptedchat.shared.message.PacketWithUserState;
import java.util.ArrayList;
import java.util.List;

public class Communicator implements OnDataReceiver {

    private final List<MyPacket> cache = new ArrayList<>();
    private final String ip;
    private final int port;
    private final Cypher cypherInput;
    private Cypher cypherOutput = new SimpleCypher();
    private Client client;

    private OnDisconnectListener disconnectListener;
    private OnConnectedListener connectedListener;
    private OnMessageReceiveListener messageReceiveListener;
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
    private OnClientsChangeListener clientsChangeListener;

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
                Pair<List<MyPacket>, List<MyPacket>> packetsWithPublicKey = PacketWithPublicKey.getPackets(publicKey);

                write(packetsWithPublicKey.first);
                write(packetsWithPublicKey.second);
            }
        };
    }

    private void processPacket(final List<MyPacket> packets, final byte messageId) {
        switch (messageId) {
            case MyPacket.MESSAGE_PUBLIC_KEY_N:
                final PublicKey publicKey = PacketWithPublicKey.getPublicKey(packets);

                cypherOutput = new RSA(publicKey);

                MyPacket requestContacts = new MyPacket().setMessageId(MyPacket.MESSAGE_CONTACTS);
                sendBytes(requestContacts.toByteArray());
                break;
            case MyPacket.MESSAGE_CONTACTS:
                List<Long> users = PacketWithContacts.getUsers(packets);
                if (clientsChangeListener != null) {
                    clientsChangeListener.onListRequest(users);
                }
                break;
            case MyPacket.MESSAGE_USER_STATE_CHANGED:
                Pair<Long, ClientState> clientStatePair = PacketWithUserState.getState(packets);
                if (clientsChangeListener != null) {
                    clientsChangeListener.onClientChangeState(clientStatePair.first, clientStatePair.second);
                }
                break;
            case MyPacket.MESSAGE_SEND:
                MyPacket infoPacket = packets.get(0);
                final byte[] clientSourceIdRaw = new byte[Long.BYTES];
                infoPacket.getData(clientSourceIdRaw);
                final long clientSourceId = BitUtils.longFromBytes(clientSourceIdRaw);
                byte[] crypted = MyPacket.packetToDataArray(packets, 1);
                byte[] encrypted = cypherInput.decrypt(crypted);
                if (messageReceiveListener != null) {
                    messageReceiveListener.onMessageReceive(new String(encrypted), clientSourceId);
                }
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

    public void write(MyPacket packet) {
        client.write(packet.toByteArray());
    }

    public void write(List<MyPacket> packets) {
        packets.forEach(packet -> write(packet));
    }

    public void sendBytes(byte[] bytes) {
        client.write(bytes);
    }

    public void sendMessage(byte[] message, long clientId) {
        MyPacket destinationClientInfo = new MyPacket()
            .setMessageId(MyPacket.MESSAGE_SEND)
            .setStatus(Status.CONTINUE);
        final byte[] destinationClientIdRaw = new byte[Long.BYTES];
        BitUtils.longToBytes(clientId, destinationClientIdRaw);
        destinationClientInfo.addData(destinationClientIdRaw);
        byte[] crypted = cypherOutput.encrypt(message);
        List<MyPacket> packets = MyPacket.buildPackets(crypted, MyPacket.MESSAGE_SEND);
        sendBytes(destinationClientInfo.toByteArray());
        packets.forEach(packet -> sendBytes(packet.toByteArray()));
    }

    public void setConnectedListener(OnConnectedListener connectedListener) {
        this.connectedListener = connectedListener;
    }

    public void setDisconnectListener(OnDisconnectListener disconnectListener) {
        this.disconnectListener = disconnectListener;
    }

    public void setClientsChangeListener(OnClientsChangeListener clientsChangeListener) {
        this.clientsChangeListener = clientsChangeListener;
    }

    public void setMessageReceiveListener(OnMessageReceiveListener messageReceiveListener) {
        this.messageReceiveListener = messageReceiveListener;
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

    public interface OnClientsChangeListener {

        void onListRequest(List<Long> users);

        void onClientChangeState(long clientId, ClientState clientState);

    }

    public interface OnMessageReceiveListener {
        void onMessageReceive(String message, long clientId);
    }
}
