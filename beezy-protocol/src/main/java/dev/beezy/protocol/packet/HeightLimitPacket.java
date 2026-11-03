package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 50: the build height of one dimension, for the Height Limit HUD. */
@Stable
public final class HeightLimitPacket extends AbstractPacket {

    private final String world;
    private final int minY;
    private final int maxY;

    /**
     * @param world dimension id, empty for the one the player is in when it arrives
     * @param minY lowest y
     * @param maxY highest y, not below minY
     */
    public HeightLimitPacket(String world, int minY, int maxY) {
        super(PacketKind.HEIGHT_LIMIT);
        this.world = Validate.str(world, Protocol.MAX_WORLD, "height limit world");
        if (minY > maxY) {
            throw new IllegalArgumentException("height limit min y " + minY + " is above max y " + maxY);
        }
        this.minY = minY;
        this.maxY = maxY;
    }

    /** @return the world */
    public String world() {
        return world;
    }

    /** @return min y */
    public int minY() {
        return minY;
    }

    /** @return max y */
    public int maxY() {
        return maxY;
    }

    @Override
    public void write(WireWriter out) {
        out.str(world).i32(minY).i32(maxY);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static HeightLimitPacket read(PacketKind kind, WireReader in) {
        return new HeightLimitPacket(in.str(), in.i32(), in.i32());
    }
}
