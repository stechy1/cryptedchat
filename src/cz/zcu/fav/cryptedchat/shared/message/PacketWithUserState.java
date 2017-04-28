package cz.zcu.fav.cryptedchat.shared.message;

import cz.zcu.fav.cryptedchat.shared.BitUtils;
import cz.zcu.fav.cryptedchat.shared.ClientState;
import cz.zcu.fav.cryptedchat.shared.MyPacket;
import cz.zcu.fav.cryptedchat.shared.Pair;
import java.util.List;

public class PacketWithUserState {

    private PacketWithUserState() {}

    public static Pair<Long, ClientState> getState(List<MyPacket> packets) {
        MyPacket packetWithState = packets.get(0);
        final byte[] data = new byte[MyPacket.DATA_SIZE];
        packetWithState.getData(data);
        return new Pair(BitUtils.longFromBytes(data, 1), ClientState.values()[data[0]]);
    }

}
