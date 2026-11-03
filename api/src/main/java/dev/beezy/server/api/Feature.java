package dev.beezy.server.api;

/**
 * A feature the client lists in its Hello. A bit is set only when the feature is available in
 * that client <em>and</em> switched on by the player; a server should not send what is off.
 */
public enum Feature {
    WAYPOINTS(0),
    NOTIFICATIONS(1),
    COOLDOWNS(2),
    TIMERS(3),
    HEIGHT_LIMIT(4),
    TEAM(5),
    RICH_PRESENCE(6),
    BEAMS(7),
    BORDERS(8),
    GLOW(9),
    HOLOGRAMS(10),
    MODULE_SETTINGS(11),
    NAMETAGS(12),
    NICK_HIDER(13),
    /** Food values for the Saturation display. */
    FOOD_VALUES(14),
    TNT_COUNTDOWN(15),
    TRANSFER(16);

    private final int bit;

    Feature(int bit) {
        this.bit = bit;
    }

    /** Bit index in the Hello's {@code u32} features field. */
    public int bit() {
        return bit;
    }

    /** {@code 1 << bit()}. */
    public int mask() {
        return 1 << bit;
    }
}
