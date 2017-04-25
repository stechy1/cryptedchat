package cz.zcu.fav.cryptedchat.shared.message;

import cz.zcu.fav.cryptedchat.crypto.RSA.PublicKey;
import cz.zcu.fav.cryptedchat.shared.BitUtils;
import cz.zcu.fav.cryptedchat.shared.MyPacket;
import cz.zcu.fav.cryptedchat.shared.MyPacket.Status;
import cz.zcu.fav.cryptedchat.shared.Pair;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

public class PacketWithPublicKey {

    private PacketWithPublicKey() {}

    public static PublicKey getPublicKey(final List<MyPacket> packets) {
        final List<MyPacket> packetWithKeyN = packets.stream()
            .filter(packet -> packet.hasMessageId(MyPacket.MESSAGE_PUBLIC_KEY_N))
            .collect(Collectors.toList());
        final List<MyPacket> packetWithKeyE = packets.stream()
            .filter(packet -> packet.hasMessageId(MyPacket.MESSAGE_PUBLIC_KEY_E))
            .collect(Collectors.toList());

        final byte[] keyN = BitUtils.packetToDataArray(packetWithKeyN);
        final byte[] keyE = BitUtils.packetToDataArray(packetWithKeyE);

        return new PublicKey(new BigInteger(keyN), new BigInteger(keyE));
    }

    public static Pair<List<MyPacket>, List<MyPacket>> getPackets(final PublicKey publicKey) {
        byte[][] data = publicKey.getRawData();
        List<MyPacket> packetN = MyPacket
            .buildPackets(data[PublicKey.INDEX_N], MyPacket.MESSAGE_PUBLIC_KEY_N);
        packetN.get(packetN.size() - 1).setStatus(Status.CONTINUE);
        List<MyPacket> packetE = MyPacket
            .buildPackets(data[PublicKey.INDEX_E], MyPacket.MESSAGE_PUBLIC_KEY_E);

        return new Pair(packetN, packetE);
    }
}
