package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;
import java.util.EnumSet;
import java.util.Set;

/** Removes one element by id: waypoint, cooldown, timer, beam, border or hologram. */
@Stable
public final class RemovePacket extends AbstractPacket {

    private static final Set<PacketKind> KINDS = EnumSet.of(PacketKind.WAYPOINT_REMOVE, PacketKind.COOLDOWN_REMOVE,
            PacketKind.TIMER_REMOVE, PacketKind.BEAM_REMOVE, PacketKind.BORDER_REMOVE, PacketKind.HOLOGRAM_REMOVE);

    private final String id;

    /**
     * @param kind one of the {@code *_REMOVE} kinds keyed by id
     * @param id the element's id, 1-32 bytes
     */
    public RemovePacket(PacketKind kind, String id) {
        super(kind);
        if (!KINDS.contains(kind)) {
            throw new IllegalArgumentException(kind + " is not removed by id");
        }
        this.id = Validate.id(id, Protocol.MAX_ID, "id");
    }

    /** @return the id */
    public String id() {
        return id;
    }

    @Override
    public void write(WireWriter out) {
        out.str(id);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static RemovePacket read(PacketKind kind, WireReader in) {
        return new RemovePacket(kind, in.str());
    }
}
