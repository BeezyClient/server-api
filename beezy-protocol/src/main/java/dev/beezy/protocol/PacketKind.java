package dev.beezy.protocol;

import dev.beezy.protocol.packet.BeamSetPacket;
import dev.beezy.protocol.packet.BorderSetPacket;
import dev.beezy.protocol.packet.CooldownSetPacket;
import dev.beezy.protocol.packet.EmptyPacket;
import dev.beezy.protocol.packet.EntityPacket;
import dev.beezy.protocol.packet.FoodValuesPacket;
import dev.beezy.protocol.packet.GlowSetPacket;
import dev.beezy.protocol.packet.HeightLimitPacket;
import dev.beezy.protocol.packet.HelloPacket;
import dev.beezy.protocol.packet.HologramSetPacket;
import dev.beezy.protocol.packet.ModuleSettingsPacket;
import dev.beezy.protocol.packet.ModuleSettingsReleasePacket;
import dev.beezy.protocol.packet.NametagSetPacket;
import dev.beezy.protocol.packet.NickHiderPacket;
import dev.beezy.protocol.packet.NotificationPacket;
import dev.beezy.protocol.packet.RemovePacket;
import dev.beezy.protocol.packet.RichPresencePacket;
import dev.beezy.protocol.packet.TeamPacket;
import dev.beezy.protocol.packet.TimerSetPacket;
import dev.beezy.protocol.packet.TntCountdownPacket;
import dev.beezy.protocol.packet.TransferPacket;
import dev.beezy.protocol.packet.TransferResultPacket;
import dev.beezy.protocol.packet.WaypointSetPacket;
import dev.beezy.stability.Stable;

/**
 * The registry of message kinds: wire id, direction, the Hello feature bit that gates it, and
 * how its body is read.
 */
@Stable
public enum PacketKind {
    /** 1: the client's Hello. */
    HELLO(1, Direction.SERVERBOUND, -1, HelloPacket::read),
    /** 2: clears everything this server sent. */
    RESET(2, Direction.CLIENTBOUND, -1, EmptyPacket::read),
    /** 3: the client's answer to a Transfer. */
    TRANSFER_RESULT(3, Direction.SERVERBOUND, -1, TransferResultPacket::read),
    /** 10. */
    WAYPOINT_SET(10, Direction.CLIENTBOUND, 0, WaypointSetPacket::read),
    /** 11. */
    WAYPOINT_REMOVE(11, Direction.CLIENTBOUND, 0, RemovePacket::read),
    /** 12. */
    WAYPOINTS_CLEAR(12, Direction.CLIENTBOUND, 0, EmptyPacket::read),
    /** 20. */
    NOTIFICATION(20, Direction.CLIENTBOUND, 1, NotificationPacket::read),
    /** 30. */
    COOLDOWN_SET(30, Direction.CLIENTBOUND, 2, CooldownSetPacket::read),
    /** 31. */
    COOLDOWN_REMOVE(31, Direction.CLIENTBOUND, 2, RemovePacket::read),
    /** 32. */
    COOLDOWNS_CLEAR(32, Direction.CLIENTBOUND, 2, EmptyPacket::read),
    /** 40. */
    TIMER_SET(40, Direction.CLIENTBOUND, 3, TimerSetPacket::read),
    /** 41. */
    TIMER_REMOVE(41, Direction.CLIENTBOUND, 3, RemovePacket::read),
    /** 42. */
    TIMERS_CLEAR(42, Direction.CLIENTBOUND, 3, EmptyPacket::read),
    /** 50. */
    HEIGHT_LIMIT(50, Direction.CLIENTBOUND, 4, HeightLimitPacket::read),
    /** 51. */
    HEIGHT_LIMIT_CLEAR(51, Direction.CLIENTBOUND, 4, EmptyPacket::read),
    /** 60. */
    TEAM(60, Direction.CLIENTBOUND, 5, TeamPacket::read),
    /** 61. */
    TEAM_CLEAR(61, Direction.CLIENTBOUND, 5, EmptyPacket::read),
    /** 70. */
    RICH_PRESENCE(70, Direction.CLIENTBOUND, 6, RichPresencePacket::read),
    /** 71. */
    RICH_PRESENCE_CLEAR(71, Direction.CLIENTBOUND, 6, EmptyPacket::read),
    /** 80. */
    BEAM_SET(80, Direction.CLIENTBOUND, 7, BeamSetPacket::read),
    /** 81. */
    BEAM_REMOVE(81, Direction.CLIENTBOUND, 7, RemovePacket::read),
    /** 82. */
    BEAMS_CLEAR(82, Direction.CLIENTBOUND, 7, EmptyPacket::read),
    /** 90. */
    BORDER_SET(90, Direction.CLIENTBOUND, 8, BorderSetPacket::read),
    /** 91. */
    BORDER_REMOVE(91, Direction.CLIENTBOUND, 8, RemovePacket::read),
    /** 92. */
    BORDERS_CLEAR(92, Direction.CLIENTBOUND, 8, EmptyPacket::read),
    /** 100. */
    GLOW_SET(100, Direction.CLIENTBOUND, 9, GlowSetPacket::read),
    /** 101. */
    GLOW_REMOVE(101, Direction.CLIENTBOUND, 9, EntityPacket::read),
    /** 102. */
    GLOW_CLEAR(102, Direction.CLIENTBOUND, 9, EmptyPacket::read),
    /** 110. */
    HOLOGRAM_SET(110, Direction.CLIENTBOUND, 10, HologramSetPacket::read),
    /** 111. */
    HOLOGRAM_REMOVE(111, Direction.CLIENTBOUND, 10, RemovePacket::read),
    /** 112. */
    HOLOGRAMS_CLEAR(112, Direction.CLIENTBOUND, 10, EmptyPacket::read),
    /** 120. */
    MODULE_SETTINGS(120, Direction.CLIENTBOUND, 11, ModuleSettingsPacket::read),
    /** 121. */
    MODULE_SETTINGS_RELEASE(121, Direction.CLIENTBOUND, 11, ModuleSettingsReleasePacket::read),
    /** 130. */
    NAMETAG_SET(130, Direction.CLIENTBOUND, 12, NametagSetPacket::read),
    /** 131. */
    NAMETAG_RESET(131, Direction.CLIENTBOUND, 12, EntityPacket::read),
    /** 132. */
    NAMETAGS_CLEAR(132, Direction.CLIENTBOUND, 12, EmptyPacket::read),
    /** 140. */
    NICK_HIDER(140, Direction.CLIENTBOUND, 13, NickHiderPacket::read),
    /** 141. */
    NICK_HIDER_CLEAR(141, Direction.CLIENTBOUND, 13, EmptyPacket::read),
    /** 150. */
    FOOD_VALUES(150, Direction.CLIENTBOUND, 14, FoodValuesPacket::read),
    /** 151. */
    FOOD_VALUES_CLEAR(151, Direction.CLIENTBOUND, 14, EmptyPacket::read),
    /** 160. */
    TNT_COUNTDOWN(160, Direction.CLIENTBOUND, 15, TntCountdownPacket::read),
    /** 161. */
    TNT_COUNTDOWN_CLEAR(161, Direction.CLIENTBOUND, 15, EmptyPacket::read),
    /** 170. */
    TRANSFER(170, Direction.CLIENTBOUND, 16, TransferPacket::read);

    /** Reads a kind's body. */
    @FunctionalInterface
    interface Reader {
        Packet read(PacketKind kind, WireReader in);
    }

    private static final PacketKind[] BY_ID = new PacketKind[256];

    static {
        for (PacketKind k : values()) {
            BY_ID[k.id] = k;
        }
    }

    private final int id;
    private final Direction direction;
    private final int featureBit;
    private final Reader reader;

    PacketKind(int id, Direction direction, int featureBit, Reader reader) {
        this.id = id;
        this.direction = direction;
        this.featureBit = featureBit;
        this.reader = reader;
    }

    /** @return the kind byte */
    public int id() {
        return id;
    }

    /** @return who sends it */
    public Direction direction() {
        return direction;
    }

    /**
     * @return the Hello feature bit a client must have set to accept this kind, or -1 if none
     *     (Hello, Reset, Transfer result)
     */
    public int featureBit() {
        return featureBit;
    }

    /**
     * @param id a kind byte
     * @return the kind, or null if this library does not know it
     */
    public static PacketKind byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
    }

    Packet read(WireReader in) {
        return reader.read(this, in);
    }
}
