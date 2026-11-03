package dev.beezy.api;

import dev.beezy.stability.Stable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A feature a client lists in its Hello. A feature is listed only when that part of Beezy is
 * available <em>and</em> the player switched it on; messages for a feature that is off are
 * never sent.
 */
@Stable
public enum Feature {
    /** Waypoints (needs the player's Waypoints module). */
    WAYPOINTS(0),
    /** Notifications. */
    NOTIFICATIONS(1),
    /** Cooldown bars. */
    COOLDOWNS(2),
    /** Timers. */
    TIMERS(3),
    /** The Height Limit HUD. */
    HEIGHT_LIMIT(4),
    /** Team mates on the minimap. */
    TEAM(5),
    /** Discord rich presence. */
    RICH_PRESENCE(6),
    /** Beams. */
    BEAMS(7),
    /** Borders. */
    BORDERS(8),
    /** Glowing entities. */
    GLOW(9),
    /** Holograms. */
    HOLOGRAMS(10),
    /** Module settings overrides. */
    MODULE_SETTINGS(11),
    /** Nametags. */
    NAMETAGS(12),
    /** Nick Hider name. */
    NICK_HIDER(13),
    /** Food values for the Saturation display. */
    FOOD_VALUES(14),
    /** TNT countdown fuse length. */
    TNT_COUNTDOWN(15),
    /** Transfers to another server. */
    TRANSFER(16);

    private final int bit;

    Feature(int bit) {
        this.bit = bit;
    }

    /** @return the bit index in the Hello's feature field */
    public int bit() {
        return bit;
    }

    /** @return {@code 1 << bit()} */
    public int mask() {
        return 1 << bit;
    }

    /**
     * @param bits a Hello's feature field
     * @return the known features set in it
     */
    public static Set<Feature> fromBits(int bits) {
        EnumSet<Feature> set = EnumSet.noneOf(Feature.class);
        for (Feature f : values()) {
            if ((bits & f.mask()) != 0) {
                set.add(f);
            }
        }
        return Collections.unmodifiableSet(set);
    }
}
