package cz.zcu.fav.cryptedchat.client;

import cz.zcu.fav.cryptedchat.shared.BitUtils;
import cz.zcu.fav.cryptedchat.shared.MyPacket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class Client extends Thread {

    private final String ip;
    private final int port;
    private final OnDataReceiver receiver;
    private OnConnectedListener connectedListener;
    private OnDisconnectListener disconnectListener;
    private OnLostConnectionListener lostConnectionListener;

    private boolean running = true;

    private InputStream reader;
    private OutputStream writer;
    private Socket socket;

    public Client(String ip, int port, OnDataReceiver receiver) {
        System.out.println("Vytvářím nového síťového klienta");
        this.ip = ip;
        this.port = port;
        this.receiver = receiver;
    }

    public void kill() {
        running = false;
        if (socket == null) {
            return;
        }

        try {
            socket.close();
            System.out.println(socket.isClosed());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] bytes) {
        try {
            System.out.println("Posílám, length: " + bytes.length + "; data: " + BitUtils.byteArrayToHex(bytes));
            writer.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUp() throws IOException {
        System.out.println("Vytvářím komunikační socket");
        Thread socketThread = new Thread(() -> {
            try {
                socket = new Socket(InetAddress.getByName(ip), port);
            } catch (Exception ex) {
                System.out.println("Nepodařilo se navázat spojení");
            }
        });
        socketThread.start();
        try {
            socketThread.join(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (socket == null) {
            throw new IOException("Připojení selhalo");
        }
        System.out.println("Komunikační socket vytvořen");
        reader = socket.getInputStream();
        writer = socket.getOutputStream();
    }

    @Override
    public void run() {
        final byte[] message = new byte[MyPacket.SIZE];
        try {
            setUp();
        } catch (IOException e) {
            System.out.println("Klient se nemohl připojit");
            if (disconnectListener != null) {
                disconnectListener.onDisconnect();
            }
            return;
        }

        System.out.println("Jdu do nekonečné smyčky");
        if (connectedListener != null) {
            connectedListener.onConnected();
        }

        while(running) {
            final byte[] buffer = new byte[MyPacket.SIZE / 2];
            int count;
            int totalSize = 0;
            try {
                while(reader.available() != 0) {
                    count = reader.read(buffer);
                    int freeBytes = MyPacket.SIZE - totalSize;
                    int byteCount = count > freeBytes ? freeBytes : count;

                    System.arraycopy(buffer, 0, message, totalSize, byteCount);
                    totalSize += count;

                    if (totalSize >= MyPacket.SIZE) {
                        MyPacket packet = new MyPacket();
                        packet.setRawData(message);
                        System.out.println("Received: " + BitUtils.byteArrayToHex(message));
                        receiver.onReceive(packet);

                        totalSize %= MyPacket.SIZE;
                        Arrays.fill(message, totalSize, message.length, (byte) 0);
                        System.arraycopy(buffer, count - totalSize, message, 0, totalSize);
                    }
                }
            } catch (IOException e) {
                if (lostConnectionListener != null) {
                    lostConnectionListener.onLostConnection();
                }
            }
        }

        if (disconnectListener != null) {
            disconnectListener.onDisconnect();
        }
    }

    public void setConnectedListener(
        OnConnectedListener connectedListener) {
        this.connectedListener = connectedListener;
    }

    public void setDisconnectListener(
        OnDisconnectListener disconnectListener) {
        this.disconnectListener = disconnectListener;
    }

    public void setLostConnectionListener(
        OnLostConnectionListener lostConnectionListener) {
        this.lostConnectionListener = lostConnectionListener;
    }

    public interface OnDataReceiver {
        void onReceive(MyPacket packet);
    }

    public interface OnConnectedListener {
        void onConnected();
    }

    public interface OnDisconnectListener {
        void onDisconnect();
    }

    public interface OnLostConnectionListener {
        void onLostConnection();
    }
}
