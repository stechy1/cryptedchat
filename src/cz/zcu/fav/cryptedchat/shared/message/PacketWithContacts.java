package cz.zcu.fav.cryptedchat.shared.message;


import cz.zcu.fav.cryptedchat.shared.BitUtils;
import cz.zcu.fav.cryptedchat.shared.MyPacket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class PacketWithContacts {

    private PacketWithContacts() {}

    public static List<MyPacket> getPackets(Enumeration<Long> users, long clientId) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while(users.hasMoreElements()) {
            long next = users.nextElement();
            if (next == clientId) {
                continue;
            }
            byte[] dest = new byte[8];
            BitUtils.longToBytes(next, dest);
            try {
                outputStream.write(dest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return MyPacket.buildPackets(outputStream.toByteArray(), MyPacket.MESSAGE_CONTACTS);
    }

    public static List<Long> getUsers(List<MyPacket> packets) {
        final List<Long> users = new ArrayList<>();
        final byte[] data = MyPacket.packetToDataArray(packets);
        assert data.length % 8 == 0;

        for (int i = 0; i < data.length; i+= 8) {
            users.add(BitUtils.longFromBytes(data, i));
        }

        return users;
    }
}
