package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 80: adds or replaces a beacon-like beam. */
@Stable
public final class BeamSetPacket extends AbstractPacket {

    private final String id;
    private final int x;
    private final int y;
    private final int z;
    private final String world;
    private final int argb;
    private final long durationMs;

    /**
     * @param id 1-32 bytes
     * @param x block x
     * @param y block y
     * @param z block z
     * @param world dimension id, empty for any
     * @param argb colour
     * @param durationMs 0 to 24 h
     */
    public BeamSetPacket(String id, int x, int y, int z, String world, int argb, long durationMs) {
        super(PacketKind.BEAM_SET);
        this.id = Validate.id(id, Protocol.MAX_ID, "beam id");
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = Validate.str(world, Protocol.MAX_WORLD, "beam world");
        this.argb = argb;
        this.durationMs = Validate.duration(durationMs, "beam duration ms");
    }

    /** @return the id */
    public String id() {
        return id;
    }

    /** @return x */
    public int x() {
        return x;
    }

    /** @return y */
    public int y() {
        return y;
    }

    /** @return z */
    public int z() {
        return z;
    }

    /** @return the world */
    public String world() {
        return world;
    }

    /** @return the colour */
    public int argb() {
        return argb;
    }

    /** @return the duration */
    public long durationMs() {
        return durationMs;
    }

    @Override
    public void write(WireWriter out) {
        out.str(id).i32(x).i32(y).i32(z).str(world).i32(argb).u32(durationMs);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static BeamSetPacket read(PacketKind kind, WireReader in) {
        return new BeamSetPacket(in.str(), in.i32(), in.i32(), in.i32(), in.str(), in.i32(), in.u32());
    }
}
