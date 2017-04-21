package cz.zcu.fav.cryptedchat.server;

import cz.zcu.fav.cryptedchat.shared.BitUtils;
import cz.zcu.fav.cryptedchat.shared.MyPacket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class Server extends Thread {

    private final ArrayList<ClientThread> clients = new ArrayList<>();

    private boolean running = true;

    private ServerSocket serverSocket;
    private ServerHandler receiver;

    public Server(ServerHandler receiver) {
        this.receiver = receiver;
    }

    private void setUp() throws IOException {
        serverSocket = new ServerSocket(16958);
    }

    public void writeTo(final long clientId, final MyPacket packet) {
        clients.stream()
            .filter(clientThread -> clientThread.id == clientId)
            .findFirst()
            .ifPresent(clientThread -> {
                try {
                    clientThread.write(packet.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }

    @Override
    public void run() {
        try {
            setUp();
        } catch (IOException e) {
            System.out.println("Chyba při vytváření serveru");
            return;
        }

        System.out.println("Server naslouchá na portu: " + serverSocket.getLocalPort());
        while (running) {
            try {
                Socket client = serverSocket.accept();
                System.out.println("Indikuji připojení od nového klienta");
                ClientThread clientThread = new ClientThread(System.currentTimeMillis(), client);
                clients.add(clientThread);
                clientThread.start();
            } catch (IOException e) {
                System.out.println("Server socket byl uzavřen");
            }
        }

        clients.stream().forEach(clientThread -> {
            clientThread.kill();
            try {
                clientThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void kill() {
        System.out.println("Ukončuji server");
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientThread extends Thread {

        private final long id;
        private final InputStream reader;
        private final OutputStream writer;
        private final Socket socket;

        private boolean connected = true;

        private ClientThread(long id, Socket socket) throws IOException {
            this.id = id;
            this.socket = socket;
            this.reader = socket.getInputStream();
            this.writer = socket.getOutputStream();

            receiver.onClientConnected(id);
        }

        public void kill() {
            System.out.println("Zabíjím klienta s id: " + id);
            connected = false;
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void write(byte[] data) throws IOException {
            writer.write(data);
        }

        @Override
        public void run() {
            final byte[] message = new byte[MyPacket.SIZE];

            while (connected) {
                final byte[] buffer = new byte[MyPacket.SIZE / 2];
                int count;
                int totalSize = 0;
                try {
                    while (reader.available() != 0) {
                        count = reader.read(buffer);
                        int freeBytes = MyPacket.SIZE - totalSize;
                        int byteCount = count > freeBytes ? freeBytes : count;

                        System.arraycopy(buffer, 0, message, totalSize, byteCount);
                        totalSize += count;

                        if (totalSize >= MyPacket.SIZE) {
                            System.out.println("Received: " + BitUtils.byteArrayToHex(message));
                            MyPacket packet = new MyPacket();
                            packet.setRawData(message);
                            receiver.onDataReceived(packet, id);

                            totalSize %= MyPacket.SIZE;
                            Arrays.fill(message, totalSize, message.length, (byte) 0);
                            System.arraycopy(buffer, count - totalSize, message, 0, totalSize);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            receiver.onClientDisconnected(id);
        }
    }
    
    public interface ServerHandler {
        void onClientConnected(long clientId);
        void onClientDisconnected(long clientId);
        void onDataReceived(MyPacket packet, long clientId);
    }
    
}