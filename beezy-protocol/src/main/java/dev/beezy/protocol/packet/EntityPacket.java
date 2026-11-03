package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;
import java.util.UUID;

/** A message whose body is one {@code uuid}: Glow remove and Nametag reset. */
@Stable
public final class EntityPacket extends AbstractPacket {

    private final UUID entity;

    /**
     * @param kind {@link PacketKind#GLOW_REMOVE} or {@link PacketKind#NAMETAG_RESET}
     * @param entity the entity or player
     */
    public EntityPacket(PacketKind kind, UUID entity) {
        super(kind);
        if (kind != PacketKind.GLOW_REMOVE && kind != PacketKind.NAMETAG_RESET) {
            throw new IllegalArgumentException(kind + " is not keyed by entity");
        }
        this.entity = Validate.notNull(entity, "entity");
    }

    /** @return the entity */
    public UUID entity() {
        return entity;
    }

    @Override
    public void write(WireWriter out) {
        out.uuid(entity);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static EntityPacket read(PacketKind kind, WireReader in) {
        return new EntityPacket(kind, in.uuid());
    }
}
