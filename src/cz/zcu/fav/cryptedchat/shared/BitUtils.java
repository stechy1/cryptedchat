package cz.zcu.fav.cryptedchat.shared;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by petr on 21.4.17.
 */
public final class BitUtils {

    private BitUtils() {}

    public static int setBit(int original, int flag, boolean value) {
        if (value) {
            original |= flag;
        } else {
            original &= ~flag;
        }

        return original;
    }

    public static boolean isBitSet(int original, int value) {
        return (original & value) == value;
    }

    public static int clearBit(int original, int value) {
        return setBit(original, value, false);
    }

    public static byte[] packetToDataArray(final List<MyPacket> packets) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream(packets.size() * MyPacket.SIZE);
        packets.stream().forEach(packet -> {
            final byte[] bytes = new byte[packet.getLength()];
            packet.getData(bytes);
            try {
                stream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return stream.toByteArray();
    }

    public static String byteArrayToHex(byte[] array) {
        StringBuilder sb = new StringBuilder(array.length * 2);
        for(byte b : array) {
            sb.append(String.format("%02x ", b));
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

}
