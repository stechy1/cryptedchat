package cz.zcu.fav.cryptedchat.crypto;


public interface Cypher {

    byte[] encrypt(byte[] src);

    byte[] decrypt(byte[] src);

}
