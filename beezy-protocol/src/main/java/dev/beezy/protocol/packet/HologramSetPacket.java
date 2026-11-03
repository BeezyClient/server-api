package dev.beezy.protocol.packet;

import dev.beezy.protocol.JsonText;
import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Kind 110: adds or replaces floating text, 1-8 component lines, top first. */
@Stable
public final class HologramSetPacket extends AbstractPacket {

    private final String id;
    private final double x;
    private final double y;
    private final double z;
    private final String world;
    private final List<JsonText> lines;
    private final long durationMs;

    /**
     * @param id 1-32 bytes
     * @param x finite
     * @param y finite
     * @param z finite
     * @param world dimension id, empty for any
     * @param lines 1-8 components
     * @param durationMs 0 to 24 h
     */
    public HologramSetPacket(String id, double x, double y, double z, String world, List<JsonText> lines, long durationMs) {
        super(PacketKind.HOLOGRAM_SET);
        this.id = Validate.id(id, Protocol.MAX_ID, "hologram id");
        this.x = Validate.finite(x, "hologram x");
        this.y = Validate.finite(y, "hologram y");
        this.z = Validate.finite(z, "hologram z");
        this.world = Validate.str(world, Protocol.MAX_WORLD, "hologram world");
        Validate.notNull(lines, "hologram lines");
        Validate.range(lines.size(), 1, Protocol.MAX_HOLOGRAM_LINES, "hologram line count");
        for (JsonText line : lines) {
            Validate.notNull(line, "hologram line");
        }
        this.lines = Collections.unmodifiableList(new ArrayList<JsonText>(lines));
        this.durationMs = Validate.duration(durationMs, "hologram duration ms");
    }

    /** @return the id */
    public String id() {
        return id;
    }

    /** @return x */
    public double x() {
        return x;
    }

    /** @return y */
    public double y() {
        return y;
    }

    /** @return z */
    public double z() {
        return z;
    }

    /** @return the world */
    public String world() {
        return world;
    }

    /** @return the lines */
    public List<JsonText> lines() {
        return lines;
    }

    /** @return the duration */
    public long durationMs() {
        return durationMs;
    }

    @Override
    public void write(WireWriter out) {
        out.str(id).f64(x).f64(y).f64(z).str(world).u8(lines.size());
        for (JsonText line : lines) {
            out.component(line);
        }
        out.u32(durationMs);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static HologramSetPacket read(PacketKind kind, WireReader in) {
        String id = in.str();
        double x = in.f64();
        double y = in.f64();
        double z = in.f64();
        String world = in.str();
        int count = Validate.range(in.u8(), 1, Protocol.MAX_HOLOGRAM_LINES, "hologram line count");
        List<JsonText> lines = new ArrayList<JsonText>(count);
        for (int i = 0; i < count; i++) {
            lines.add(in.component());
        }
        return new HologramSetPacket(id, x, y, z, world, lines, in.u32());
    }
}
