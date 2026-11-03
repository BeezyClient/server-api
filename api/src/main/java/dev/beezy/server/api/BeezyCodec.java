package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.*;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Encoder and decoder for {@code beezy:server} format 1 (docs/SERVER-API.md).
 *
 * <p>Every encode method returns the complete plugin-message payload (format byte, kind byte,
 * body) and throws {@link IllegalArgumentException} for input the client would drop: a string
 * over its cap (caps are UTF-8 bytes), a number out of range, more than 32 team members, a
 * message over 32 768 bytes. Nothing is ever truncated silently.
 */
public final class BeezyCodec {

    private BeezyCodec() {
    }

    // ---- client -> server ----------------------------------------------------------------

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
                .str(w.id(), MAX_ID, "waypoint id")
                .str(w.name(), MAX_NAME, "waypoint name")
                .i32(w.x()).i32(w.y()).i32(w.z())
                .str(w.dimension(), MAX_DIMENSION, "waypoint dimension")
                .i32(w.color())
                .u8(w.beam() ? 1 : 0)
                .done();
    }

    public static byte[] waypointRemove(String id) {
        return new Out(KIND_WAYPOINT_REMOVE).str(id, MAX_ID, "waypoint id").done();
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
                .str(c.id(), MAX_ID, "cooldown id")
                .str(c.label(), MAX_LABEL, "cooldown label")
                .str(c.itemIcon(), MAX_ITEM_ICON, "cooldown item icon")
                .u32(Check.range(c.durationMs(), 0, MAX_COOLDOWN_MS, "cooldown duration ms"))
                .done();
    }

    public static byte[] cooldownRemove(String id) {
        return new Out(KIND_COOLDOWN_REMOVE).str(id, MAX_ID, "cooldown id").done();
    }

    public static byte[] cooldownsClear() {
        return new Out(KIND_COOLDOWNS_CLEAR).done();
    }

    public static byte[] timer(Timer t) {
        Check.notNull(t, "timer");
        return new Out(KIND_TIMER_SET)
                .str(t.id(), MAX_ID, "timer id")
                .str(t.label(), MAX_LABEL, "timer label")
                .u8(t.mode().id())
                .u32(t.startMs())
                .u8(t.running() ? 1 : 0)
                .done();
    }

    public static byte[] timerRemove(String id) {
        return new Out(KIND_TIMER_REMOVE).str(id, MAX_ID, "timer id").done();
    }

    public static byte[] timersClear() {
        return new Out(KIND_TIMERS_CLEAR).done();
    }

    /**
     * @param dimension e.g. {@code minecraft:overworld}; empty or null means the player's current one
     * @throws IllegalArgumentException if {@code minY > maxY} or the dimension is over 64 bytes
     */
    public static byte[] heightLimit(String dimension, int minY, int maxY) {
        if (minY > maxY) {
            throw new IllegalArgumentException("height limit min y " + minY + " is above max y " + maxY);
        }
        return new Out(KIND_HEIGHT_LIMIT)
                .str(dimension == null ? "" : dimension, MAX_DIMENSION, "height limit dimension")
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
                    .i32(m.color())
                    .u8(m.hasPosition() ? 1 : 0);
            if (m.hasPosition()) {
                out.f64(m.x()).f64(m.y()).f64(m.z()).str(m.dimension(), MAX_DIMENSION, "team member dimension");
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

        byte[] done() {
            if (buf.size() > MAX_MESSAGE_BYTES) {
                throw new IllegalArgumentException("message is " + buf.size() + " bytes, at most " + MAX_MESSAGE_BYTES + " allowed");
            }
            return buf.toByteArray();
        }
    }
}
