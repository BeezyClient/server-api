package dev.beezy.server.api;

/**
 * Constants of the {@code beezy:server} protocol, format 1. The contract is PROTOCOL.md.
 */
public final class BeezyProtocol {

    /** Plugin channel name, the same on every Minecraft version (under 1.8's 20-character limit). */
    public static final String CHANNEL = "beezy:server";

    /** First byte of every message. */
    public static final int FORMAT = 1;

    /** A whole message is at most this many bytes. */
    public static final int MAX_MESSAGE_BYTES = 32768;

    // Client -> server
    public static final int KIND_HELLO = 1;
    public static final int KIND_TRANSFER_RESULT = 3;

    // Server -> client
    public static final int KIND_RESET = 2;
    public static final int KIND_WAYPOINT_SET = 10;
    public static final int KIND_WAYPOINT_REMOVE = 11;
    public static final int KIND_WAYPOINTS_CLEAR = 12;
    public static final int KIND_NOTIFICATION = 20;
    public static final int KIND_COOLDOWN_SET = 30;
    public static final int KIND_COOLDOWN_REMOVE = 31;
    public static final int KIND_COOLDOWNS_CLEAR = 32;
    public static final int KIND_TIMER_SET = 40;
    public static final int KIND_TIMER_REMOVE = 41;
    public static final int KIND_TIMERS_CLEAR = 42;
    public static final int KIND_HEIGHT_LIMIT = 50;
    public static final int KIND_HEIGHT_LIMIT_CLEAR = 51;
    public static final int KIND_TEAM = 60;
    public static final int KIND_TEAM_CLEAR = 61;
    public static final int KIND_RICH_PRESENCE = 70;
    public static final int KIND_RICH_PRESENCE_CLEAR = 71;
    public static final int KIND_BEAM_SET = 80;
    public static final int KIND_BEAM_REMOVE = 81;
    public static final int KIND_BEAMS_CLEAR = 82;
    public static final int KIND_BORDER_SET = 90;
    public static final int KIND_BORDER_REMOVE = 91;
    public static final int KIND_BORDERS_CLEAR = 92;
    public static final int KIND_GLOW_SET = 100;
    public static final int KIND_GLOW_REMOVE = 101;
    public static final int KIND_GLOW_CLEAR = 102;
    public static final int KIND_HOLOGRAM_SET = 110;
    public static final int KIND_HOLOGRAM_REMOVE = 111;
    public static final int KIND_HOLOGRAMS_CLEAR = 112;
    public static final int KIND_MODULE_SETTINGS = 120;
    public static final int KIND_MODULE_SETTINGS_RELEASE = 121;
    public static final int KIND_NAMETAG_SET = 130;
    public static final int KIND_NAMETAG_RESET = 131;
    public static final int KIND_NAMETAGS_CLEAR = 132;
    public static final int KIND_NICK_HIDER = 140;
    public static final int KIND_NICK_HIDER_CLEAR = 141;
    public static final int KIND_FOOD_VALUES = 150;
    public static final int KIND_FOOD_VALUES_CLEAR = 151;
    public static final int KIND_TNT_COUNTDOWN = 160;
    public static final int KIND_TNT_COUNTDOWN_CLEAR = 161;
    public static final int KIND_TRANSFER = 170;

    // Per-field caps, in UTF-8 bytes.
    public static final int MAX_VERSION = 32;
    public static final int MAX_ID = 32;
    public static final int MAX_NAME = 32;
    public static final int MAX_WORLD = 64;
    public static final int MAX_NOTIFICATION_TITLE = 32;
    public static final int MAX_NOTIFICATION_TEXT = 160;
    public static final int MAX_ITEM_ICON = 64;
    public static final int MAX_LABEL = 32;
    public static final int MAX_TEAM_MEMBERS = 32;
    public static final int MAX_TEAM_MEMBER_NAME = 16;
    public static final int MAX_PRESENCE_FIELD = 32;
    public static final int MAX_PRESENCE_STATE = 64;
    public static final int MAX_HOLOGRAM_LINES = 8;
    public static final int MAX_HOLOGRAM_LINE = 128;
    public static final int MAX_SETTINGS = 64;
    public static final int MAX_MODULE = 32;
    public static final int MAX_OPTION = 32;
    public static final int MAX_SETTING_TEXT = 64;
    public static final int MAX_NAMETAG_LINES = 5;
    public static final int MAX_NAMETAG_LINE = 64;
    public static final int MAX_NICK = 16;
    public static final int MAX_FOOD_VALUES = 64;
    public static final int MAX_FOOD_ITEM = 64;
    public static final int MAX_HUNGER = 20;
    public static final double MAX_SATURATION = 20.0;
    public static final int MIN_TNT_TICKS = 1;
    public static final int MAX_TNT_TICKS = 1200;
    public static final int MAX_ADDRESS = 255;

    /** Cooldown duration cap: one hour. */
    public static final long MAX_COOLDOWN_MS = 3_600_000L;
    /** Notification duration range, seconds. */
    public static final int MIN_NOTIFICATION_SECONDS = 1;
    public static final int MAX_NOTIFICATION_SECONDS = 30;
    /** Cap of every {@code duration} field (waypoints, beams, borders, holograms): 24 hours. 0 = until removed. */
    public static final long MAX_DURATION_MS = 86_400_000L;

    private BeezyProtocol() {
    }
}
