package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;
import java.util.EnumMap;
import java.util.Map;

/** A kind with an empty body: Reset and every "clear". */
@Stable
public final class EmptyPacket extends AbstractPacket {

    private static final Map<PacketKind, EmptyPacket> INSTANCES = new EnumMap<PacketKind, EmptyPacket>(PacketKind.class);

    static {
        for (PacketKind k : new PacketKind[] {
            PacketKind.RESET, PacketKind.WAYPOINTS_CLEAR, PacketKind.COOLDOWNS_CLEAR, PacketKind.TIMERS_CLEAR,
            PacketKind.HEIGHT_LIMIT_CLEAR, PacketKind.TEAM_CLEAR, PacketKind.RICH_PRESENCE_CLEAR,
            PacketKind.BEAMS_CLEAR, PacketKind.BORDERS_CLEAR, PacketKind.GLOW_CLEAR, PacketKind.HOLOGRAMS_CLEAR,
            PacketKind.NAMETAGS_CLEAR, PacketKind.NICK_HIDER_CLEAR, PacketKind.FOOD_VALUES_CLEAR,
            PacketKind.TNT_COUNTDOWN_CLEAR,
        }) {
            INSTANCES.put(k, new EmptyPacket(k));
        }
    }

    private EmptyPacket(PacketKind kind) {
        super(kind);
    }

    /**
     * @param kind Reset or a clear kind
     * @return the packet
     * @throws IllegalArgumentException if that kind has a body
     */
    public static EmptyPacket of(PacketKind kind) {
        EmptyPacket p = INSTANCES.get(kind);
        if (p == null) {
            throw new IllegalArgumentException(kind + " is not an empty-body kind");
        }
        return p;
    }

    /** @return the Reset message */
    public static EmptyPacket reset() {
        return of(PacketKind.RESET);
    }

    @Override
    public void write(WireWriter out) {
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static EmptyPacket read(PacketKind kind, WireReader in) {
        return of(kind);
    }
}
