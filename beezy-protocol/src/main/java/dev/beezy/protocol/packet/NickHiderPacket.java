package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 140: the name Nick Hider shows for the player themselves. */
@Stable
public final class NickHiderPacket extends AbstractPacket {

    private final String nick;

    /** @param nick 1-16 bytes */
    public NickHiderPacket(String nick) {
        super(PacketKind.NICK_HIDER);
        this.nick = Validate.id(nick, Protocol.MAX_NICK, "nick");
    }

    /** @return the nick */
    public String nick() {
        return nick;
    }

    @Override
    public void write(WireWriter out) {
        out.str(nick);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static NickHiderPacket read(PacketKind kind, WireReader in) {
        return new NickHiderPacket(in.str());
    }
}
