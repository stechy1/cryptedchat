package cz.zcu.fav.cryptedchat.shared;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Třída představující zákadní stavební kámen pro komunikaci
 * Packet se skládá z hlavíčky, ID zprávy a samotných dat
 * Hlavička - velikost: 2byty,
 *          první bit ukazuje, jestli další packet, který příjde bude navazující,
 *              nebo tento je poslední
 *          další 4 bity jsou nepoužity,
 *          zbylých 11 obsahuje počet datových bitů ve zprávě
 * ID zprávy - velikost: 1byte,
 *          obsahuje typ zprávy/události
 * data - zbytek bytů
 */
public class MyPacket {

    public static final int SIZE = 128;

    public static final byte MESSAGE_PUBLIC_KEY_E = 0x01;
    public static final byte MESSAGE_PUBLIC_KEY_N = 0x02;
    public static final byte MESSAGE_ECHO = 0x03;
    public static final byte MESSAGE_SEND = 0x04;
    public static final byte MESSAGE_CONTACTS = 0x05;
    public static final byte MESSAGE_USER_STATE_CHANGED = 0x06;

    public static final int DATA_SIZE = SIZE - 3;
    private static final int INDEX_LENGTH = 0;
    private static final int INDEX_STATE = 0;
    private static final int INDEX_MESSAGE_ID = 2;
    private static final int INDEX_DATA = 3;

    private static final int FLAG_STATE = 0x80;

    private final byte[] data;
    private int dataOffset = INDEX_DATA;

    public static List<MyPacket> buildPackets(byte[] src, byte messageId) {
        if (src.length == 0) {
            return Collections.singletonList(new MyPacket().setMessageId(messageId));
        }
        final int iterations = (int) Math.round(Math.ceil(src.length / (double) MyPacket.DATA_SIZE));
        final List<MyPacket> packets = new ArrayList<>(iterations);
        int offset = 0;
        int remaining = src.length;

        for (int i = 0; i < iterations; i++) {
            final int count = (remaining > DATA_SIZE) ? DATA_SIZE : remaining;
            final MyPacket packet = new MyPacket()
                .setMessageId(messageId)
                .setLength(count)
                .setStatus(Status.CONTINUE);
            packet.setData(src, offset, count);
            packets.add(packet);

            offset += count;
            remaining -= count;
        }

        packets.get(packets.size() - 1).setStatus(Status.END);

        return packets;
    }

    public static byte[] packetToDataArray(final List<MyPacket> packets) {
        return packetToDataArray(packets, 0);
    }

    public static byte[] packetToDataArray(final List<MyPacket> packets, int listOffset) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream(packets.size() * SIZE);
        for (int i = listOffset; i < packets.size(); i++) {
            MyPacket packet = packets.get(i);
            final byte[] bytes = new byte[packet.getLength()];
            packet.getData(bytes);
            try {
                stream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return stream.toByteArray();
    }

    public MyPacket() {
        this(new byte[SIZE]);
    }

    public MyPacket(byte[] data) {
        this.data = data;
    }

    public MyPacket setStatus(Status status) {
        data[INDEX_STATE] = (byte) BitUtils.setBit(data[INDEX_STATE], FLAG_STATE, status == Status.CONTINUE);

        return this;
    }

    public Status getStatus() {
        return Status.values()[((data[INDEX_STATE] & 0xFF) >> 7) % Status.values().length];
    }

    public MyPacket setLength(int length) {
        data[INDEX_LENGTH] |= (byte) ((length >>> 8) & ~0x07); // První byte
        data[INDEX_LENGTH + 1] = (byte) (length & 0xFF);     // Druhý byte

        return this;
    }

    public int getLength() {
        int result = 0;
        result |= ((data[INDEX_LENGTH]) & 0x07); // První byte
        result |= (data[INDEX_LENGTH + 1] & 0xFF);      // Druhý byte

        return result;
    }

    public MyPacket setMessageId(byte id) {
        data[INDEX_MESSAGE_ID] |= id;

        return this;
    }

    public boolean hasMessageId(byte id) {
        return (data[INDEX_MESSAGE_ID] & id) == id;
    }

    public byte getMessageId() {
        return data[INDEX_MESSAGE_ID];
    }

    public void getData(byte[] dest) {
        getData(dest, 0);
    }

    public void getData(byte[] dest, int offset) {
        final int dataLength = getLength();
        System.arraycopy(data, INDEX_DATA, dest, offset, dest.length < dataLength ? dest.length : dataLength);
    }

    public int setData(byte[] src) {
        return setData(src, src.length);
    }

    public int setData(byte[] src, int length) {
        return setData(src, 0, length);
    }

    public int setData(byte[] src, int offset, int length) {
        final int overflow = length - DATA_SIZE;
        final int copyCount = (overflow > 0) ? DATA_SIZE : length;
        System.arraycopy(src, offset, data, INDEX_DATA, copyCount);

        dataOffset = INDEX_DATA + copyCount;
        setLength(dataOffset - INDEX_DATA);

        return overflow;
    }

    public int addData(byte[] src) {
        return addData(src, 0);
    }

    public int addData(byte[] src, int offset) {
        final int dataLength = src.length - offset;
        final int freeSpace = DATA_SIZE - dataOffset;
        final int overflow = dataLength - freeSpace;
        final int copyCount = (overflow > 0) ? freeSpace : dataLength;
        System.arraycopy(src, offset, data, dataOffset, copyCount);

        dataOffset += copyCount;
        setLength(dataOffset - INDEX_DATA);


        return overflow;
    }

    public byte[] toByteArray() {
        return data;
    }

    public MyPacket setRawData(byte[] src) {
        assert src.length <= SIZE;
        System.arraycopy(src, 0, data, 0, src.length);

        return this;
    }

    public enum Status {
        END, CONTINUE
    }
}
