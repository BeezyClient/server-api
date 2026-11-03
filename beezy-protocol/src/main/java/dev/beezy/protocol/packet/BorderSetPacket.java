package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 90: adds or replaces a wall around a rectangle. */
@Stable
public final class BorderSetPacket extends AbstractPacket {

    /** Flag bit 0: the client stops the player walking through it. */
    public static final int FLAG_SOLID = 1;
    /** Flag bit 1: only drawn within 16 blocks. */
    public static final int FLAG_ONLY_WHEN_NEAR = 2;

    private final String id;
    private final String world;
    private final double minX;
    private final double minZ;
    private final double maxX;
    private final double maxZ;
    private final int argb;
    private final int flags;
    private final long durationMs;

    /**
     * @param id 1-32 bytes
     * @param world dimension id, empty for any
     * @param minX finite, at most maxX
     * @param minZ finite, at most maxZ
     * @param maxX finite
     * @param maxZ finite
     * @param argb colour
     * @param flags {@link #FLAG_SOLID} | {@link #FLAG_ONLY_WHEN_NEAR}
     * @param durationMs 0 to 24 h
     */
    public BorderSetPacket(String id, String world, double minX, double minZ, double maxX, double maxZ,
            int argb, int flags, long durationMs) {
        super(PacketKind.BORDER_SET);
        this.id = Validate.id(id, Protocol.MAX_ID, "border id");
        this.world = Validate.str(world, Protocol.MAX_WORLD, "border world");
        this.minX = Validate.finite(minX, "border min x");
        this.minZ = Validate.finite(minZ, "border min z");
        this.maxX = Validate.finite(maxX, "border max x");
        this.maxZ = Validate.finite(maxZ, "border max z");
        if (minX > maxX || minZ > maxZ) {
            throw new IllegalArgumentException("border min is above max");
        }
        this.argb = argb;
        this.flags = Validate.range(flags, 0, FLAG_SOLID | FLAG_ONLY_WHEN_NEAR, "border flags");
        this.durationMs = Validate.duration(durationMs, "border duration ms");
    }

    /** @return the id */
    public String id() {
        return id;
    }

    /** @return the world */
    public String world() {
        return world;
    }

    /** @return min x */
    public double minX() {
        return minX;
    }

    /** @return min z */
    public double minZ() {
        return minZ;
    }

    /** @return max x */
    public double maxX() {
        return maxX;
    }

    /** @return max z */
    public double maxZ() {
        return maxZ;
    }

    /** @return the colour */
    public int argb() {
        return argb;
    }

    /** @return the flags */
    public int flags() {
        return flags;
    }

    /** @return the duration */
    public long durationMs() {
        return durationMs;
    }

    @Override
    public void write(WireWriter out) {
        out.str(id).str(world).f64(minX).f64(minZ).f64(maxX).f64(maxZ).i32(argb).u8(flags).u32(durationMs);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static BorderSetPacket read(PacketKind kind, WireReader in) {
        return new BorderSetPacket(in.str(), in.str(), in.f64(), in.f64(), in.f64(), in.f64(), in.i32(), in.u8(), in.u32());
    }
}
