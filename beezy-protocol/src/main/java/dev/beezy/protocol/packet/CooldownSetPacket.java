package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 30: adds or replaces a cooldown bar. */
@Stable
public final class CooldownSetPacket extends AbstractPacket {

    private final String id;
    private final String label;
    private final String itemIcon;
    private final long durationMs;

    /**
     * @param id 1-32 bytes
     * @param label at most 32 bytes
     * @param itemIcon item id, at most 64 bytes, empty for none
     * @param durationMs at most one hour
     */
    public CooldownSetPacket(String id, String label, String itemIcon, long durationMs) {
        super(PacketKind.COOLDOWN_SET);
        this.id = Validate.id(id, Protocol.MAX_ID, "cooldown id");
        this.label = Validate.str(label, Protocol.MAX_LABEL, "cooldown label");
        this.itemIcon = Validate.str(itemIcon, Protocol.MAX_ITEM, "cooldown item icon");
        this.durationMs = Validate.range(durationMs, 0, Protocol.MAX_COOLDOWN_MS, "cooldown duration ms");
    }

    /** @return the id */
    public String id() {
        return id;
    }

    /** @return the label */
    public String label() {
        return label;
    }

    /** @return the item icon, empty for none */
    public String itemIcon() {
        return itemIcon;
    }

    /** @return the duration */
    public long durationMs() {
        return durationMs;
    }

    @Override
    public void write(WireWriter out) {
        out.str(id).str(label).str(itemIcon).u32(durationMs);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static CooldownSetPacket read(PacketKind kind, WireReader in) {
        return new CooldownSetPacket(in.str(), in.str(), in.str(), in.u32());
    }
}
