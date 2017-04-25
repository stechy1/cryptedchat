package cz.zcu.fav.cryptedchat.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

public class RSA implements Cypher {

    public final BigInteger n;
    public final BigInteger e;
    public final BigInteger d;

    public RSA(int bitLength) {
        final Random r = new Random();
        final BigInteger p = BigInteger.probablePrime(bitLength, r);
        final BigInteger q = BigInteger.probablePrime(bitLength, r);
        n = p.multiply(q);
        final BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        e = BigInteger.probablePrime(bitLength / 2, r);
        while (phi.gcd(e).compareTo(BigInteger.ONE) > 0 && e.compareTo(phi) < 0) {
            e.add(BigInteger.ONE);
        }
        d = e.modInverse(phi);

        System.out.println("E: " + e.toString());
        System.out.println("D: " + d.toString());
        System.out.println("N: " + n.toString());
    }

    public RSA(PublicKey publicKey) {
        n = publicKey.n;
        e = publicKey.e;
        d = null;
    }

    @Override
    public byte[] encrypt(byte[] message) {
        final int bitLength = n.bitLength();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(message.length);
        final int iteration = (int) Math.round(Math.ceil(message.length / (double) bitLength));

        int offset = 0;
        int remaining = message.length;

        for (int i = 0; i < iteration; i++) {
            final int count = (remaining > bitLength) ? bitLength : remaining;
            final byte[] data = new byte[count];
            System.arraycopy(message, offset, data, 0, count);
            try {
                outputStream.write(new BigInteger(data).modPow(e, n).toByteArray());
            } catch (IOException ex) {
                System.out.println("Data se nepodařilo zašifrovat");
            }

            offset += count;
            remaining -= count;
        }

        return outputStream.toByteArray();
    }

    @Override
    public byte[] decrypt(byte[] message) {
        final int bitLength = 64;
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(message.length);
        final int iteration = (int) Math.round(Math.ceil(message.length / (double) bitLength));

        int offset = 0;
        int remaining = message.length;

        for (int i = 0; i < iteration; i++) {
            final int count = (remaining > bitLength) ? bitLength : remaining;
            final byte[] data = new byte[count];
            System.arraycopy(message, offset, data, 0, count);
            try {
                outputStream.write(new BigInteger(data).modPow(d, n).toByteArray());
            } catch (IOException ex) {
                System.out.println("Data se nepodařilo zašifrovat");
            }

            offset += count;
            remaining -= count;
        }

        return outputStream.toByteArray();
    }

    public final PublicKey getPublicKey() {
        return new PublicKey(n, e);
    }

    public static class PublicKey {
        public static final int INDEX_N = 0;
        public static final int INDEX_E = 1;

        public final BigInteger n;
        public final BigInteger e;

        public PublicKey(BigInteger n, BigInteger e) {
            this.n = n;
            this.e = e;
        }

        public byte[][] getRawData() {
            final byte[][] result = new byte[2][];
            result[INDEX_N] = n.toByteArray();
            result[INDEX_E] = e.toByteArray();

            return result;
        }
    }
}