package dev.beezy.protocol;

import dev.beezy.stability.Stable;

/**
 * Constants of the {@code beezy:server} protocol, format 1: the channel, the message cap and
 * every field cap (caps on strings are UTF-8 bytes). PROTOCOL.md is the contract.
 */
@Stable
public final class Protocol {

    /** Plugin channel name, the same on every Minecraft version (under 1.8's 20-character limit). */
    public static final String CHANNEL = "beezy:server";
    /** First byte of every message. */
    public static final int FORMAT = 1;
    /** A whole message is at most this many bytes. */
    public static final int MAX_MESSAGE_BYTES = 32768;

    /** Cap of ids (waypoints, beams, borders, holograms, cooldowns, timers, transfers). */
    public static final int MAX_ID = 32;
    /** Cap of the versions in a Hello. */
    public static final int MAX_VERSION = 32;
    /** Cap of a waypoint name. */
    public static final int MAX_NAME = 32;
    /** Cap of a {@code world} field. */
    public static final int MAX_WORLD = 64;
    /** Cap of a notification title. */
    public static final int MAX_NOTIFICATION_TITLE = 32;
    /** Cap of a notification text. */
    public static final int MAX_NOTIFICATION_TEXT = 160;
    /** Notification display time, seconds. */
    public static final int MIN_NOTIFICATION_SECONDS = 1;
    /** Notification display time, seconds. */
    public static final int MAX_NOTIFICATION_SECONDS = 30;
    /** Cap of cooldown and timer labels. */
    public static final int MAX_LABEL = 32;
    /** Cap of an item id (cooldown icon, food item). */
    public static final int MAX_ITEM = 64;
    /** Cooldown duration cap: one hour. */
    public static final long MAX_COOLDOWN_MS = 3_600_000L;
    /** Cap of every {@code duration} field: 24 hours (0 means "until removed"). */
    public static final long MAX_DURATION_MS = 86_400_000L;
    /** Members in one Team message. */
    public static final int MAX_TEAM_MEMBERS = 32;
    /** Cap of a team member's name. */
    public static final int MAX_TEAM_MEMBER_NAME = 16;
    /** Cap of the rich presence game, mode and map. */
    public static final int MAX_PRESENCE_FIELD = 32;
    /** Cap of the rich presence state. */
    public static final int MAX_PRESENCE_STATE = 64;
    /** Lines of a hologram (at least 1). */
    public static final int MAX_HOLOGRAM_LINES = 8;
    /** Lines of a nametag (0 hides it). */
    public static final int MAX_NAMETAG_LINES = 5;
    /** Smallest {@code component}: 1 byte of JSON. */
    public static final int MIN_COMPONENT_BYTES = 1;
    /** Largest {@code component}: 4096 bytes of JSON. */
    public static final int MAX_COMPONENT_BYTES = 4096;
    /** Entries in one module settings (or release) message. */
    public static final int MAX_SETTINGS = 64;
    /** Cap of a module id. */
    public static final int MAX_MODULE = 32;
    /** Cap of a setting (option) id. */
    public static final int MAX_OPTION = 32;
    /** Cap of a text setting value. */
    public static final int MAX_SETTING_TEXT = 64;
    /** Cap of the Nick Hider name. */
    public static final int MAX_NICK = 16;
    /** Entries in one food values message. */
    public static final int MAX_FOOD_VALUES = 64;
    /** Hunger range of a food value: 0 to this. */
    public static final int MAX_HUNGER = 20;
    /** Saturation range of a food value: 0 to this. */
    public static final float MAX_SATURATION = 20f;
    /** TNT fuse range, ticks. */
    public static final int MIN_TNT_TICKS = 1;
    /** TNT fuse range, ticks. */
    public static final int MAX_TNT_TICKS = 1200;
    /** Cap of a transfer address. */
    public static final int MAX_ADDRESS = 255;

    private Protocol() {
    }
}
