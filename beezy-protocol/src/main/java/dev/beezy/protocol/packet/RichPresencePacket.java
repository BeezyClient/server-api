package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 70: the player's Discord rich presence while on this server. */
@Stable
public final class RichPresencePacket extends AbstractPacket {

    private final String game;
    private final String mode;
    private final String map;
    private final String state;
    private final int teamSize;
    private final int teamMax;

    /**
     * @param game at most 32 bytes
     * @param mode at most 32 bytes
     * @param map at most 32 bytes
     * @param state at most 64 bytes
     * @param teamSize 0-255
     * @param teamMax 0-255, 0 for none
     */
    public RichPresencePacket(String game, String mode, String map, String state, int teamSize, int teamMax) {
        super(PacketKind.RICH_PRESENCE);
        this.game = Validate.str(game, Protocol.MAX_PRESENCE_FIELD, "presence game");
        this.mode = Validate.str(mode, Protocol.MAX_PRESENCE_FIELD, "presence mode");
        this.map = Validate.str(map, Protocol.MAX_PRESENCE_FIELD, "presence map");
        this.state = Validate.str(state, Protocol.MAX_PRESENCE_STATE, "presence state");
        this.teamSize = Validate.range(teamSize, 0, 255, "presence team size");
        this.teamMax = Validate.range(teamMax, 0, 255, "presence team max");
    }

    /** @return the game */
    public String game() {
        return game;
    }

    /** @return the mode */
    public String mode() {
        return mode;
    }

    /** @return the map */
    public String map() {
        return map;
    }

    /** @return the state */
    public String state() {
        return state;
    }

    /** @return the team size */
    public int teamSize() {
        return teamSize;
    }

    /** @return the team max */
    public int teamMax() {
        return teamMax;
    }

    @Override
    public void write(WireWriter out) {
        out.str(game).str(mode).str(map).str(state).u8(teamSize).u8(teamMax);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static RichPresencePacket read(PacketKind kind, WireReader in) {
        return new RichPresencePacket(in.str(), in.str(), in.str(), in.str(), in.u8(), in.u8());
    }
}
