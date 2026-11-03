package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 160: the TNT fuse length the TNT Timer counts from. */
@Stable
public final class TntCountdownPacket extends AbstractPacket {

    private final int ticks;

    /** @param ticks 1-1200 (vanilla 80) */
    public TntCountdownPacket(int ticks) {
        super(PacketKind.TNT_COUNTDOWN);
        this.ticks = Validate.range(ticks, Protocol.MIN_TNT_TICKS, Protocol.MAX_TNT_TICKS, "tnt ticks");
    }

    /** @return the ticks */
    public int ticks() {
        return ticks;
    }

    @Override
    public void write(WireWriter out) {
        out.u16(ticks);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static TntCountdownPacket read(PacketKind kind, WireReader in) {
        return new TntCountdownPacket(in.u16());
    }
}
