package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Encoder and decoder for {@code beezy:server} format 1 (PROTOCOL.md).
 *
 * <p>Every encode method returns the complete plugin-message payload (format byte, kind byte,
 * body) and throws {@link IllegalArgumentException} for input the client would drop: a string
 * over its cap (caps are UTF-8 bytes), a number out of range, a list longer than its cap, a
 * message over 32 768 bytes. Colours go out opaque ({@code 0xFFrrggbb}); null is 0, the
 * client's default. Nothing is ever truncated silently.
 */
public final class BeezyCodec {

    private BeezyCodec() {
    }

    // ---- client -> server ----------------------------------------------------------------

    /**
     * Parses anything a client sends: a {@link Hello} or a {@link TransferResult}. Returns null
     * for anything else or anything malformed.
     */
    public static ClientMessage decode(byte[] payload) {
        if (payload == null || payload.length < 2) {
            return null;
        }
        switch (payload[1] & 0xFF) {
            case KIND_HELLO:
                return decodeHello(payload);
            case KIND_TRANSFER_RESULT:
                return decodeTransferResult(payload);
            default:
                return null;
        }
    }

    /**
     * Parses a Hello. Returns null when the payload is not a well-formed format-1 Hello (wrong
     * format or kind, truncated, a string over its cap or not valid UTF-8, over 32 KiB). Bytes
     * after the features field are ignored, so a later client may append fields.
     */
    public static Hello decodeHello(byte[] payload) {
        if (payload == null || payload.length > MAX_MESSAGE_BYTES) {
            return null;
        }
        ByteBuffer in = ByteBuffer.wrap(payload); // big-endian by default
        try {
            if ((in.get() & 0xFF) != FORMAT || (in.get() & 0xFF) != KIND_HELLO) {
                return null;
            }
            String minecraft = readStr(in, MAX_VERSION);
            String beezy = readStr(in, MAX_VERSION);
            int features = in.getInt();
            if (minecraft == null || beezy == null) {
                return null;
            }
            return new Hello(minecraft, beezy, features);
        } catch (RuntimeException e) { // BufferUnderflowException
            return null;
        }
    }

    /**
     * Parses a Transfer result. Returns null when the payload is not a well-formed format-1
     * Transfer result (wrong format or kind, truncated, an empty id or one over 32 bytes, a status
     * the protocol does not list, over 32 KiB). Trailing bytes are ignored.
     */
    public static TransferResult decodeTransferResult(byte[] payload) {
        if (payload == null || payload.length > MAX_MESSAGE_BYTES) {
            return null;
        }
        ByteBuffer in = ByteBuffer.wrap(payload);
        try {
            if ((in.get() & 0xFF) != FORMAT || (in.get() & 0xFF) != KIND_TRANSFER_RESULT) {
                return null;
            }
            String id = readStr(in, MAX_ID);
            TransferResult.Status status = TransferResult.Status.byId(in.get() & 0xFF);
            if (id == null || id.isEmpty() || status == null) {
                return null;
            }
            return new TransferResult(id, status);
        } catch (RuntimeException e) { // BufferUnderflowException
            return null;
        }
    }

    /** Encodes a Transfer result, as the client does. Useful for tests. */
    public static byte[] transferResult(TransferResult result) {
        Check.notNull(result, "transfer result");
        return new Out(KIND_TRANSFER_RESULT).id(result.id(), MAX_ID, "transfer id").u8(result.status().id()).done();
    }

    /** Encodes a Hello, as the client does. Useful for tests. */
    public static byte[] hello(Hello hello) {
        Check.notNull(hello, "hello");
        return new Out(KIND_HELLO)
                .str(hello.minecraftVersion(), MAX_VERSION, "minecraft version")
                .str(hello.beezyVersion(), MAX_VERSION, "beezy version")
                .i32(hello.featureBits())
                .done();
    }

    // ---- server -> client ----------------------------------------------------------------

    public static byte[] reset() {
        return new Out(KIND_RESET).done();
    }

    public static byte[] waypoint(Waypoint w) {
        Check.notNull(w, "waypoint");
        return new Out(KIND_WAYPOINT_SET)
                .id(w.id(), MAX_ID, "waypoint id")
                .str(w.name(), MAX_NAME, "waypoint name")
                .i32(w.x()).i32(w.y()).i32(w.z())
                .str(w.world(), MAX_WORLD, "waypoint world")
                .color(w.color())
                .u8(w.beam() ? 1 : 0)
                .u32(w.durationMs())
                .done();
    }

    public static byte[] waypointRemove(String id) {
        return new Out(KIND_WAYPOINT_REMOVE).id(id, MAX_ID, "waypoint id").done();
    }

    public static byte[] waypointsClear() {
        return new Out(KIND_WAYPOINTS_CLEAR).done();
    }

    public static byte[] notification(Notification n) {
        Check.notNull(n, "notification");
        return new Out(KIND_NOTIFICATION)
                .str(n.title(), MAX_NOTIFICATION_TITLE, "notification title")
                .str(n.text(), MAX_NOTIFICATION_TEXT, "notification text")
                .u8(n.level().id())
                .u16(Check.range(n.seconds(), MIN_NOTIFICATION_SECONDS, MAX_NOTIFICATION_SECONDS, "notification seconds"))
                .done();
    }

    public static byte[] cooldown(Cooldown c) {
        Check.notNull(c, "cooldown");
        return new Out(KIND_COOLDOWN_SET)
                .id(c.id(), MAX_ID, "cooldown id")
                .str(c.label(), MAX_LABEL, "cooldown label")
                .str(c.itemIcon(), MAX_ITEM_ICON, "cooldown item icon")
                .u32(Check.range(c.durationMs(), 0, MAX_COOLDOWN_MS, "cooldown duration ms"))
                .done();
    }

    public static byte[] cooldownRemove(String id) {
        return new Out(KIND_COOLDOWN_REMOVE).id(id, MAX_ID, "cooldown id").done();
    }

    public static byte[] cooldownsClear() {
        return new Out(KIND_COOLDOWNS_CLEAR).done();
    }

    public static byte[] timer(Timer t) {
        Check.notNull(t, "timer");
        return new Out(KIND_TIMER_SET)
                .id(t.id(), MAX_ID, "timer id")
                .str(t.label(), MAX_LABEL, "timer label")
                .u8(t.mode().id())
                .u32(t.startMs())
                .u8(t.running() ? 1 : 0)
                .done();
    }

    public static byte[] timerRemove(String id) {
        return new Out(KIND_TIMER_REMOVE).id(id, MAX_ID, "timer id").done();
    }

    public static byte[] timersClear() {
        return new Out(KIND_TIMERS_CLEAR).done();
    }

    /**
     * @param world world key, e.g. {@code minecraft:overworld}; empty or null means the one the player is in
     * @throws IllegalArgumentException if {@code minY > maxY} or the world is over 64 bytes
     */
    public static byte[] heightLimit(String world, int minY, int maxY) {
        if (minY > maxY) {
            throw new IllegalArgumentException("height limit min y " + minY + " is above max y " + maxY);
        }
        return new Out(KIND_HEIGHT_LIMIT)
                .str(world == null ? "" : world, MAX_WORLD, "height limit world")
                .i32(minY).i32(maxY)
                .done();
    }

    public static byte[] heightLimitClear() {
        return new Out(KIND_HEIGHT_LIMIT_CLEAR).done();
    }

    /** Replaces the whole team; at most 32 members. An empty list is valid (no mates). */
    public static byte[] team(List<TeamMember> members) {
        Check.notNull(members, "team members");
        Check.range(members.size(), 0, MAX_TEAM_MEMBERS, "team member count");
        Out out = new Out(KIND_TEAM).u8(members.size());
        for (TeamMember m : members) {
            Check.notNull(m, "team member");
            out.uuid(m.uuid())
                    .str(m.name(), MAX_TEAM_MEMBER_NAME, "team member name")
                    .color(m.color())
                    .u8(m.hasPosition() ? 1 : 0);
            if (m.hasPosition()) {
                out.f64(m.x()).f64(m.y()).f64(m.z()).str(m.world(), MAX_WORLD, "team member world");
            }
        }
        return out.done();
    }

    public static byte[] teamClear() {
        return new Out(KIND_TEAM_CLEAR).done();
    }

    public static byte[] richPresence(RichPresence p) {
        Check.notNull(p, "rich presence");
        return new Out(KIND_RICH_PRESENCE)
                .str(p.game(), MAX_PRESENCE_FIELD, "presence game")
                .str(p.mode(), MAX_PRESENCE_FIELD, "presence mode")
                .str(p.map(), MAX_PRESENCE_FIELD, "presence map")
                .str(p.state(), MAX_PRESENCE_STATE, "presence state")
                .u8(Check.range(p.teamSize(), 0, 255, "presence team size"))
                .u8(Check.range(p.teamMax(), 0, 255, "presence team max"))
                .done();
    }

    public static byte[] richPresenceClear() {
        return new Out(KIND_RICH_PRESENCE_CLEAR).done();
    }

    public static byte[] beam(Beam b) {
        Check.notNull(b, "beam");
        return new Out(KIND_BEAM_SET)
                .id(b.id(), MAX_ID, "beam id")
                .i32(b.x()).i32(b.y()).i32(b.z())
                .str(b.world(), MAX_WORLD, "beam world")
                .color(b.color())
                .u32(b.durationMs())
                .done();
    }

    public static byte[] beamRemove(String id) {
        return new Out(KIND_BEAM_REMOVE).id(id, MAX_ID, "beam id").done();
    }

    public static byte[] beamsClear() {
        return new Out(KIND_BEAMS_CLEAR).done();
    }

    public static byte[] border(Border b) {
        Check.notNull(b, "border");
        return new Out(KIND_BORDER_SET)
                .id(b.id(), MAX_ID, "border id")
                .str(b.world(), MAX_WORLD, "border world")
                .f64(b.minX()).f64(b.minZ()).f64(b.maxX()).f64(b.maxZ())
                .color(b.color())
                .u8(b.flags())
                .u32(b.durationMs())
                .done();
    }

    public static byte[] borderRemove(String id) {
        return new Out(KIND_BORDER_REMOVE).id(id, MAX_ID, "border id").done();
    }

    public static byte[] bordersClear() {
        return new Out(KIND_BORDERS_CLEAR).done();
    }

    /** @param color null for the client's default */
    public static byte[] glow(UUID entity, Color color) {
        return new Out(KIND_GLOW_SET).uuid(Check.notNull(entity, "glow entity")).color(color).done();
    }

    public static byte[] glowRemove(UUID entity) {
        return new Out(KIND_GLOW_REMOVE).uuid(Check.notNull(entity, "glow entity")).done();
    }

    public static byte[] glowClear() {
        return new Out(KIND_GLOW_CLEAR).done();
    }

    public static byte[] hologram(Hologram h) {
        Check.notNull(h, "hologram");
        Out out = new Out(KIND_HOLOGRAM_SET)
                .id(h.id(), MAX_ID, "hologram id")
                .f64(h.x()).f64(h.y()).f64(h.z())
                .str(h.world(), MAX_WORLD, "hologram world")
                .u8(Check.range(h.lines().size(), 1, MAX_HOLOGRAM_LINES, "hologram line count"));
        for (String line : h.lines()) {
            out.str(line, MAX_HOLOGRAM_LINE, "hologram line");
        }
        return out.u32(h.durationMs()).done();
    }

    public static byte[] hologramRemove(String id) {
        return new Out(KIND_HOLOGRAM_REMOVE).id(id, MAX_ID, "hologram id").done();
    }

    public static byte[] hologramsClear() {
        return new Out(KIND_HOLOGRAMS_CLEAR).done();
    }

    /** Module settings override: 1 to 64 entries. */
    public static byte[] moduleSettings(ModuleSettings settings) {
        Check.notNull(settings, "module settings");
        Map<SettingKey, SettingValue> entries = settings.entries();
        Out out = new Out(KIND_MODULE_SETTINGS).u8(Check.range(entries.size(), 1, MAX_SETTINGS, "module setting count"));
        for (Map.Entry<SettingKey, SettingValue> e : entries.entrySet()) {
            out.settingKey(e.getKey());
            SettingValue v = e.getValue();
            out.u8(v.type().id());
            switch (v.type()) {
                case BOOL:
                    out.u8(((Boolean) v.value()) ? 1 : 0);
                    break;
                case INT:
                    out.i32((Integer) v.value());
                    break;
                case DECIMAL:
                    out.f64((Double) v.value());
                    break;
                case TEXT:
                    out.str((String) v.value(), MAX_SETTING_TEXT, "setting text");
                    break;
                default:
                    out.color((Color) v.value());
            }
        }
        return out.done();
    }

    /** Gives those settings back to the player: 1 to 64 keys. */
    public static byte[] moduleSettingsRelease(Collection<SettingKey> keys) {
        Check.notNull(keys, "setting keys");
        Out out = new Out(KIND_MODULE_SETTINGS_RELEASE).u8(Check.range(keys.size(), 1, MAX_SETTINGS, "setting key count"));
        for (SettingKey k : keys) {
            out.settingKey(Check.notNull(k, "setting key"));
        }
        return out.done();
    }

    /** Gives every overridden setting back to the player (count 0). */
    public static byte[] moduleSettingsReleaseAll() {
        return new Out(KIND_MODULE_SETTINGS_RELEASE).u8(0).done();
    }

    public static byte[] nametag(Nametag n) {
        Check.notNull(n, "nametag");
        Out out = new Out(KIND_NAMETAG_SET)
                .uuid(n.player())
                .u8(Check.range(n.lines().size(), 0, MAX_NAMETAG_LINES, "nametag line count"));
        for (String line : n.lines()) {
            out.str(line, MAX_NAMETAG_LINE, "nametag line");
        }
        return out.done();
    }

    public static byte[] nametagReset(UUID player) {
        return new Out(KIND_NAMETAG_RESET).uuid(Check.notNull(player, "nametag player")).done();
    }

    public static byte[] nametagsClear() {
        return new Out(KIND_NAMETAGS_CLEAR).done();
    }

    /** @param nick the name Nick Hider shows for the player themselves (1 to 16 bytes) */
    public static byte[] nickHider(String nick) {
        return new Out(KIND_NICK_HIDER).id(nick, MAX_NICK, "nick").done();
    }

    public static byte[] nickHiderClear() {
        return new Out(KIND_NICK_HIDER_CLEAR).done();
    }

    /** 1 to 64 food values. */
    public static byte[] foodValues(List<FoodValue> values) {
        Check.notNull(values, "food values");
        Out out = new Out(KIND_FOOD_VALUES).u8(Check.range(values.size(), 1, MAX_FOOD_VALUES, "food value count"));
        for (FoodValue f : values) {
            Check.notNull(f, "food value");
            out.str(f.item(), MAX_FOOD_ITEM, "food item").u8(f.hunger()).f32(f.saturation());
        }
        return out.done();
    }

    public static byte[] foodValuesClear() {
        return new Out(KIND_FOOD_VALUES_CLEAR).done();
    }

    /** @param ticks the TNT fuse length, 1 to 1200 (vanilla: 80) */
    public static byte[] tntCountdown(int ticks) {
        return new Out(KIND_TNT_COUNTDOWN).u16(Check.range(ticks, MIN_TNT_TICKS, MAX_TNT_TICKS, "tnt ticks")).done();
    }

    /** The fuse length as a duration, rounded to the nearest tick (50 ms); 1 tick to 60 s. */
    public static int tntTicks(Duration fuse) {
        Check.notNull(fuse, "tnt fuse");
        long ticks = (fuse.toMillis() + 25) / 50;
        return (int) Check.range(ticks, MIN_TNT_TICKS, MAX_TNT_TICKS, "tnt ticks");
    }

    public static byte[] tntCountdownClear() {
        return new Out(KIND_TNT_COUNTDOWN_CLEAR).done();
    }

    /**
     * @param id      echoed in the client's Transfer result (1 to 32 bytes)
     * @param address {@code host} or {@code host:port} (1 to 255 bytes)
     */
    public static byte[] transfer(String id, String address) {
        return new Out(KIND_TRANSFER)
                .id(id, MAX_ID, "transfer id")
                .id(address, MAX_ADDRESS, "transfer address")
                .done();
    }

    // ---- internals -------------------------------------------------------------------------

    /** Reads a u8-length-prefixed string; null if it is over {@code cap} bytes or not UTF-8. */
    private static String readStr(ByteBuffer in, int cap) {
        int len = in.get() & 0xFF;
        if (len > cap) {
            return null;
        }
        byte[] bytes = new byte[len];
        in.get(bytes);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    /** Big-endian message writer that starts with the format and kind bytes. */
    private static final class Out {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream(64);

        Out(int kind) {
            buf.write(FORMAT);
            buf.write(kind);
        }

        Out u8(int v) {
            buf.write(v & 0xFF);
            return this;
        }

        Out u16(int v) {
            buf.write((v >>> 8) & 0xFF);
            buf.write(v & 0xFF);
            return this;
        }

        Out i32(int v) {
            buf.write((v >>> 24) & 0xFF);
            buf.write((v >>> 16) & 0xFF);
            buf.write((v >>> 8) & 0xFF);
            buf.write(v & 0xFF);
            return this;
        }

        Out u32(long v) {
            return i32((int) Check.range(v, 0, 0xFFFFFFFFL, "u32"));
        }

        Out i64(long v) {
            i32((int) (v >>> 32));
            return i32((int) v);
        }

        Out f32(float v) {
            return i32(Float.floatToIntBits(v));
        }

        Out color(Color c) {
            return i32(Check.argb(c));
        }

        Out settingKey(SettingKey k) {
            return str(k.module(), MAX_MODULE, "setting module").str(k.option(), MAX_OPTION, "setting option");
        }

        Out f64(double v) {
            return i64(Double.doubleToLongBits(v));
        }

        Out uuid(UUID u) {
            return i64(u.getMostSignificantBits()).i64(u.getLeastSignificantBits());
        }

        Out str(String s, int cap, String field) {
            byte[] bytes = Check.str(s, cap, field).getBytes(StandardCharsets.UTF_8);
            u8(bytes.length);
            buf.write(bytes, 0, bytes.length);
            return this;
        }

        Out id(String s, int cap, String field) {
            return str(Check.id(s, cap, field), cap, field);
        }

        byte[] done() {
            if (buf.size() > MAX_MESSAGE_BYTES) {
                throw new IllegalArgumentException("message is " + buf.size() + " bytes, at most " + MAX_MESSAGE_BYTES + " allowed");
            }
            return buf.toByteArray();
        }
    }
}
