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
import java.util.UUID;

/** Kind 130: replaces the nametag above a player, 0-5 component lines (0 hides it). */
@Stable
public final class NametagSetPacket extends AbstractPacket {

    private final UUID player;
    private final List<JsonText> lines;

    /**
     * @param player whose nametag
     * @param lines 0-5 components, top first
     */
    public NametagSetPacket(UUID player, List<JsonText> lines) {
        super(PacketKind.NAMETAG_SET);
        this.player = Validate.notNull(player, "nametag player");
        Validate.notNull(lines, "nametag lines");
        Validate.range(lines.size(), 0, Protocol.MAX_NAMETAG_LINES, "nametag line count");
        for (JsonText line : lines) {
            Validate.notNull(line, "nametag line");
        }
        this.lines = Collections.unmodifiableList(new ArrayList<JsonText>(lines));
    }

    /** @return the player */
    public UUID player() {
        return player;
    }

    /** @return the lines */
    public List<JsonText> lines() {
        return lines;
    }

    @Override
    public void write(WireWriter out) {
        out.uuid(player).u8(lines.size());
        for (JsonText line : lines) {
            out.component(line);
        }
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static NametagSetPacket read(PacketKind kind, WireReader in) {
        UUID player = in.uuid();
        int count = Validate.range(in.u8(), 0, Protocol.MAX_NAMETAG_LINES, "nametag line count");
        List<JsonText> lines = new ArrayList<JsonText>(count);
        for (int i = 0; i < count; i++) {
            lines.add(in.component());
        }
        return new NametagSetPacket(player, lines);
    }
}
