package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 10: adds or replaces a waypoint. */
@Stable
public final class WaypointSetPacket extends AbstractPacket {

    /** Flag bit 0: show a beam. */
    public static final int FLAG_BEAM = 1;

    private final String id;
    private final String name;
    private final int x;
    private final int y;
    private final int z;
    private final String world;
    private final int argb;
    private final int flags;
    private final long durationMs;

    /**
     * @param id 1-32 bytes
     * @param name at most 32 bytes
     * @param x block x
     * @param y block y
     * @param z block z
     * @param world dimension id, at most 64 bytes, empty for any
     * @param argb colour, alpha 0 for the client's default
     * @param flags {@link #FLAG_BEAM} or 0
     * @param durationMs 0 (until removed) to 24 h
     */
    public WaypointSetPacket(String id, String name, int x, int y, int z, String world, int argb, int flags, long durationMs) {
        super(PacketKind.WAYPOINT_SET);
        this.id = Validate.id(id, Protocol.MAX_ID, "waypoint id");
        this.name = Validate.str(name, Protocol.MAX_NAME, "waypoint name");
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = Validate.str(world, Protocol.MAX_WORLD, "waypoint world");
        this.argb = argb;
        this.flags = Validate.range(flags, 0, FLAG_BEAM, "waypoint flags");
        this.durationMs = Validate.duration(durationMs, "waypoint duration ms");
    }

    /** @return the id */
    public String id() {
        return id;
    }

    /** @return the name */
    public String name() {
        return name;
    }

    /** @return block x */
    public int x() {
        return x;
    }

    /** @return block y */
    public int y() {
        return y;
    }

    /** @return block z */
    public int z() {
        return z;
    }

    /** @return the world, empty for any */
    public String world() {
        return world;
    }

    /** @return the ARGB colour */
    public int argb() {
        return argb;
    }

    /** @return the flags */
    public int flags() {
        return flags;
    }

    /** @return the duration in ms, 0 until removed */
    public long durationMs() {
        return durationMs;
    }

    @Override
    public void write(WireWriter out) {
        out.str(id).str(name).i32(x).i32(y).i32(z).str(world).i32(argb).u8(flags).u32(durationMs);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static WaypointSetPacket read(PacketKind kind, WireReader in) {
        return new WaypointSetPacket(in.str(), in.str(), in.i32(), in.i32(), in.i32(), in.str(), in.i32(), in.u8(), in.u32());
    }
}
