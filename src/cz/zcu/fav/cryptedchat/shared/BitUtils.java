package cz.zcu.fav.cryptedchat.shared;

import java.nio.ByteBuffer;

/**
 * Created by petr on 21.4.17.
 */
public final class BitUtils {

    private static final int BYTE_SIZE = 8;
    private static final int INT_LENGTH = 4;
    private static final int LONG_LENGTH = 8;
    private static final int INT_SIZE = 24;
    private static final int LONG_SIZE = 56;

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

    public static String byteArrayToHex(byte[] array) {
        StringBuilder sb = new StringBuilder(array.length * 2);
        for(byte b : array) {
            sb.append(String.format("%02x ", b));
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    public static int intFromBytes(byte[] src, int offset) {
        byte[] data = new byte[Integer.BYTES];
        System.arraycopy(src, offset, data, 0, Integer.BYTES);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.put(data);
        buffer.flip();
        return buffer.getInt();
    }

    public static void intToBytes(int value, byte[] dest, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(value);
        System.arraycopy(buffer.array(), 0, dest, offset, Integer.BYTES);
    }

    public static long longFromBytes(byte[] src, int offset) {
        byte[] data = new byte[Long.BYTES];
        System.arraycopy(src, offset, data, 0, Long.BYTES);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(data);
        buffer.flip();
        return buffer.getLong();
    }

    public static void longToBytes(long value, byte[] dest, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        System.arraycopy(buffer.array(), 0, dest, offset, Long.BYTES);
    }

}
