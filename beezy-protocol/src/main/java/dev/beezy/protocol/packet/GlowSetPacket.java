package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;
import java.util.UUID;

/** Kind 100: outlines an entity for this player only. */
@Stable
public final class GlowSetPacket extends AbstractPacket {

    private final UUID entity;
    private final int argb;

    /**
     * @param entity the entity
     * @param argb colour, alpha 0 for the client's default
     */
    public GlowSetPacket(UUID entity, int argb) {
        super(PacketKind.GLOW_SET);
        this.entity = Validate.notNull(entity, "glow entity");
        this.argb = argb;
    }

    /** @return the entity */
    public UUID entity() {
        return entity;
    }

    /** @return the colour */
    public int argb() {
        return argb;
    }

    @Override
    public void write(WireWriter out) {
        out.uuid(entity).i32(argb);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static GlowSetPacket read(PacketKind kind, WireReader in) {
        return new GlowSetPacket(in.uuid(), in.i32());
    }
}
