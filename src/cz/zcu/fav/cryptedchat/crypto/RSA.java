package cz.zcu.fav.cryptedchat.crypto;

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

    public RSA(BigInteger e, BigInteger d, BigInteger N) {
        this.e = e;
        this.d = d;
        this.n = N;
    }

    public RSA(PublicKey publicKey) {
        n = publicKey.n;
        e = publicKey.e;
        d = null;
    }

    public RSA(PrivateKey privateKey) {
        n = privateKey.n;
        d = privateKey.d;
        e = null;
    }

    public static String byteArrayToHex(byte[] array) {
        StringBuilder sb = new StringBuilder(array.length * 2);
        for(byte b : array) {
            sb.append(String.format("%02x ", b));
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    @Override
    public byte[] encrypt(byte[] message) {
        return new BigInteger(message).modPow(e, n).toByteArray();
    }

    @Override
    public byte[] decrypt(byte[] message) {
        return new BigInteger(message).modPow(d, n).toByteArray();
    }

    public final PublicKey getPublicKey() {
        return new PublicKey(n, e);
    }

    public final PrivateKey getPrivateKey() {
        return new PrivateKey(n, d);
    }

    public static class PublicKey {
        public static final int INDEX_N = 0;
        public static final int INDEX_E = 1;

        public final BigInteger n;
        public final BigInteger e;

        public PublicKey(BigInteger n, BigInteger e) {
            System.out.println("Vytvářím nový veřejný klíč");
            System.out.println("N: " + n.toString());
            System.out.println("E: " + e.toString());
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

    public static class PrivateKey {
        public static final int INDEX_N = 0;
        public static final int INDEX_D = 1;

        public final BigInteger n;
        public final BigInteger d;

        public PrivateKey(BigInteger n, BigInteger d) {
            this.n = n;
            this.d = d;
        }

        public byte[][] getRawData() {
            final byte[][] result = new byte[2][];
            result[INDEX_N] = n.toByteArray();
            result[INDEX_D] = d.toByteArray();

            return result;
        }
    }
}