package cz.zcu.fav.cryptedchat.crypto;

public class SimpleCypher implements Cypher {

    @Override
    public byte[] encrypt(byte[] src) {
        return src;
    }

    @Override
    public byte[] decrypt(byte[] src) {
        return src;
    }
}
