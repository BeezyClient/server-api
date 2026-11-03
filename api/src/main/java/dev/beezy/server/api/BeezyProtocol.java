package dev.beezy.server.api;

/**
 * Constants of the {@code beezy:server} protocol, format 1. The contract is docs/SERVER-API.md.
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

    // Per-field caps, in UTF-8 bytes.
    public static final int MAX_VERSION = 32;
    public static final int MAX_ID = 32;
    public static final int MAX_NAME = 32;
    public static final int MAX_DIMENSION = 64;
    public static final int MAX_NOTIFICATION_TITLE = 32;
    public static final int MAX_NOTIFICATION_TEXT = 160;
    public static final int MAX_ITEM_ICON = 64;
    public static final int MAX_LABEL = 32;
    public static final int MAX_TEAM_MEMBERS = 32;
    public static final int MAX_TEAM_MEMBER_NAME = 16;
    public static final int MAX_PRESENCE_FIELD = 32;
    public static final int MAX_PRESENCE_STATE = 64;

    /** Cooldown duration cap: one hour. */
    public static final long MAX_COOLDOWN_MS = 3_600_000L;
    /** Notification duration range, seconds. */
    public static final int MIN_NOTIFICATION_SECONDS = 1;
    public static final int MAX_NOTIFICATION_SECONDS = 30;

    private BeezyProtocol() {
    }
}
