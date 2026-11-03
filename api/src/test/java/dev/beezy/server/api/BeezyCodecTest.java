package dev.beezy.server.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Byte-for-byte checks of every kind against docs/SERVER-API.md. */
class BeezyCodecTest {

    /** Builds expected bytes: Integers are single bytes, Strings are raw UTF-8 (no length prefix). */
    private static byte[] bytes(Object... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Object p : parts) {
            if (p instanceof Integer) {
                int v = (Integer) p;
                if (v < 0 || v > 0xFF) {
                    throw new AssertionError("not a byte: " + v);
                }
                out.write(v);
            } else {
                byte[] b = ((String) p).getBytes(StandardCharsets.UTF_8);
                out.write(b, 0, b.length);
            }
        }
        return out.toByteArray();
    }

    private static String repeat(char c, int n) {
        char[] chars = new char[n];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    // ---- the example from the spec ----------------------------------------------------------

    @Test
    void specExampleNotification() {
        byte[] expected = {0x01, 0x14, 0x07, 'W', 'e', 'l', 'c', 'o', 'm', 'e',
                0x09, 'H', 'a', 'v', 'e', ' ', 'f', 'u', 'n', '!', 0x00, 0x00, 0x05};
        assertArrayEquals(expected, BeezyCodec.notification(Notification.of("Welcome", "Have fun!")));
    }

    // ---- every server -> client kind ----------------------------------------------------------

    @Test
    void reset() {
        assertArrayEquals(bytes(1, 2), BeezyCodec.reset());
    }

    @Test
    void waypointSet() {
        Waypoint w = Waypoint.builder("a").name("Spawn").at(1, -2, 0x01020304)
                .dimension("minecraft:overworld").color(0xFF55FF55).beam(true).build();
        assertArrayEquals(bytes(1, 10,
                        1, "a",
                        5, "Spawn",
                        0, 0, 0, 1,
                        0xFF, 0xFF, 0xFF, 0xFE,
                        1, 2, 3, 4,
                        19, "minecraft:overworld",
                        0xFF, 0x55, 0xFF, 0x55,
                        1),
                BeezyCodec.waypoint(w));
    }

    @Test
    void waypointDefaults() {
        // name defaults to the id, dimension to "any", color to the client's default, no beam
        Waypoint w = Waypoint.builder("home").build();
        assertArrayEquals(bytes(1, 10, 4, "home", 4, "home", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                BeezyCodec.waypoint(w));
    }

    @Test
    void waypointRemoveAndClear() {
        assertArrayEquals(bytes(1, 11, 4, "home"), BeezyCodec.waypointRemove("home"));
        assertArrayEquals(bytes(1, 12), BeezyCodec.waypointsClear());
    }

    @Test
    void notificationLevelsAndSeconds() {
        Notification n = Notification.builder("T", "x").level(Notification.Level.ERROR).seconds(30).build();
        assertArrayEquals(bytes(1, 20, 1, "T", 1, "x", 3, 0, 30), BeezyCodec.notification(n));
        assertEquals(0, Notification.Level.INFO.id());
        assertEquals(1, Notification.Level.SUCCESS.id());
        assertEquals(2, Notification.Level.WARNING.id());
        assertEquals(3, Notification.Level.ERROR.id());
    }

    @Test
    void cooldownSet() {
        Cooldown c = new Cooldown("pearl", "Pearl", "minecraft:ender_pearl", 3_600_000);
        assertArrayEquals(bytes(1, 30,
                        5, "pearl",
                        5, "Pearl",
                        21, "minecraft:ender_pearl",
                        0x00, 0x36, 0xEE, 0x80),
                BeezyCodec.cooldown(c));
        assertArrayEquals(bytes(1, 30, 1, "c", 1, "C", 0, 0, 0, 0x03, 0xE8), BeezyCodec.cooldown(Cooldown.of("c", "C", 1000)));
    }

    @Test
    void cooldownRemoveAndClear() {
        assertArrayEquals(bytes(1, 31, 5, "pearl"), BeezyCodec.cooldownRemove("pearl"));
        assertArrayEquals(bytes(1, 32), BeezyCodec.cooldownsClear());
    }

    @Test
    void timerSet() {
        assertArrayEquals(bytes(1, 40, 1, "r", 5, "Round", 0, 0x00, 0x04, 0x93, 0xE0, 1),
                BeezyCodec.timer(Timer.countdown("r", "Round", 300_000)));
        // u32 uses the full unsigned range
        Timer sw = new Timer("g", "Game", Timer.Mode.STOPWATCH, 0xFFFFFFFFL, false);
        assertArrayEquals(bytes(1, 40, 1, "g", 4, "Game", 1, 0xFF, 0xFF, 0xFF, 0xFF, 0), BeezyCodec.timer(sw));
        assertArrayEquals(bytes(1, 40, 1, "g", 4, "Game", 1, 0, 0, 0, 7, 0),
                BeezyCodec.timer(Timer.stopwatch("g", "Game", 0).pausedAt(7)));
    }

    @Test
    void timerRemoveAndClear() {
        assertArrayEquals(bytes(1, 41, 1, "r"), BeezyCodec.timerRemove("r"));
        assertArrayEquals(bytes(1, 42), BeezyCodec.timersClear());
    }

    @Test
    void heightLimit() {
        assertArrayEquals(bytes(1, 50, 0, 0xFF, 0xFF, 0xFF, 0xC0, 0, 0, 0x01, 0x40),
                BeezyCodec.heightLimit(null, -64, 320));
        assertArrayEquals(bytes(1, 50, 20, "minecraft:the_nether", 0, 0, 0, 0, 0, 0, 0, 128),
                BeezyCodec.heightLimit("minecraft:the_nether", 0, 128));
        assertArrayEquals(bytes(1, 51), BeezyCodec.heightLimitClear());
    }

    @Test
    void team() {
        UUID a = new UUID(0x0102030405060708L, 0x090A0B0C0D0E0F10L);
        UUID b = new UUID(-1L, 0L);
        List<TeamMember> members = new ArrayList<TeamMember>();
        members.add(new TeamMember(a, "Alice", 0xFFFF5555));
        members.add(new TeamMember(b, "Bob", 0).withPosition(1.5, -2.0, 0.0, "minecraft:overworld"));
        assertArrayEquals(bytes(1, 60, 2,
                        // Alice: uuid, name, color, no position
                        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                        5, "Alice",
                        0xFF, 0xFF, 0x55, 0x55,
                        0,
                        // Bob: uuid, name, color, position
                        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0, 0, 0, 0,
                        3, "Bob",
                        0, 0, 0, 0,
                        1,
                        0x3F, 0xF8, 0, 0, 0, 0, 0, 0,          // 1.5
                        0xC0, 0x00, 0, 0, 0, 0, 0, 0,          // -2.0
                        0, 0, 0, 0, 0, 0, 0, 0,                // 0.0
                        19, "minecraft:overworld"),
                BeezyCodec.team(members));
        assertArrayEquals(bytes(1, 60, 0), BeezyCodec.team(Collections.<TeamMember>emptyList()));
        assertArrayEquals(bytes(1, 61), BeezyCodec.teamClear());
    }

    @Test
    void richPresence() {
        RichPresence p = RichPresence.builder("Bed Wars").mode("Solo").map("Map").state("In game").team(1, 4).build();
        assertArrayEquals(bytes(1, 70, 8, "Bed Wars", 4, "Solo", 3, "Map", 7, "In game", 1, 4), BeezyCodec.richPresence(p));
        assertArrayEquals(bytes(1, 70, 1, "G", 0, 0, 0, 0, 0), BeezyCodec.richPresence(RichPresence.builder("G").build()));
        assertArrayEquals(bytes(1, 71), BeezyCodec.richPresenceClear());
    }

    @Test
    void multiByteUtf8LengthIsInBytes() {
        // "é" is 2 bytes; the prefix counts bytes, not chars
        assertArrayEquals(bytes(1, 11, 2, "é"), BeezyCodec.waypointRemove("é"));
    }

    // ---- caps -------------------------------------------------------------------------------

    @Test
    void stringCapsAreUtf8Bytes() {
        assertNotNull(BeezyCodec.waypointRemove(repeat('a', 32)));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.waypointRemove(repeat('a', 33)));
        // 16 chars but 32 bytes: fits; 17 two-byte chars: 34 bytes, does not
        assertNotNull(BeezyCodec.waypointRemove(repeat('é', 16)));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.waypointRemove(repeat('é', 17)));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.waypointRemove(null));
    }

    @Test
    void waypointCaps() {
        Waypoint.builder(repeat('i', 32)).name(repeat('n', 32)).dimension(repeat('d', 64)).build();
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder(repeat('i', 33)).build());
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder("a").name(repeat('n', 33)).build());
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder("a").dimension(repeat('d', 65)).build());
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder(null).build());
    }

    @Test
    void notificationCaps() {
        Notification.builder(repeat('t', 32), repeat('x', 160)).seconds(1).build();
        assertThrows(IllegalArgumentException.class, () -> Notification.of(repeat('t', 33), "x"));
        assertThrows(IllegalArgumentException.class, () -> Notification.of("t", repeat('x', 161)));
        assertThrows(IllegalArgumentException.class, () -> Notification.builder("t", "x").seconds(0).build());
        assertThrows(IllegalArgumentException.class, () -> Notification.builder("t", "x").seconds(31).build());
        assertThrows(IllegalArgumentException.class, () -> Notification.builder("t", "x").level(null).build());
    }

    @Test
    void cooldownCaps() {
        new Cooldown(repeat('i', 32), repeat('l', 32), repeat('c', 64), 0);
        assertThrows(IllegalArgumentException.class, () -> new Cooldown(repeat('i', 33), "l", "", 1));
        assertThrows(IllegalArgumentException.class, () -> new Cooldown("i", repeat('l', 33), "", 1));
        assertThrows(IllegalArgumentException.class, () -> new Cooldown("i", "l", repeat('c', 65), 1));
        assertThrows(IllegalArgumentException.class, () -> Cooldown.of("i", "l", 3_600_001));
        assertThrows(IllegalArgumentException.class, () -> Cooldown.of("i", "l", -1));
    }

    @Test
    void timerCaps() {
        assertThrows(IllegalArgumentException.class, () -> Timer.countdown(repeat('i', 33), "l", 1));
        assertThrows(IllegalArgumentException.class, () -> Timer.countdown("i", repeat('l', 33), 1));
        assertThrows(IllegalArgumentException.class, () -> Timer.countdown("i", "l", -1));
        assertThrows(IllegalArgumentException.class, () -> Timer.countdown("i", "l", 0x1_0000_0000L));
        assertThrows(IllegalArgumentException.class, () -> new Timer("i", "l", null, 0, true));
    }

    @Test
    void heightLimitCaps() {
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.heightLimit(repeat('d', 65), 0, 1));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.heightLimit("", 10, 0));
    }

    @Test
    void teamCaps() {
        UUID u = UUID.randomUUID();
        new TeamMember(u, repeat('n', 16), 0);
        assertThrows(IllegalArgumentException.class, () -> new TeamMember(u, repeat('n', 17), 0));
        assertThrows(IllegalArgumentException.class, () -> new TeamMember(null, "n", 0));
        assertThrows(IllegalArgumentException.class, () -> new TeamMember(u, "n", 0).withPosition(Double.NaN, 0, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new TeamMember(u, "n", 0).withPosition(0, 0, 0, repeat('d', 65)));
        List<TeamMember> members = new ArrayList<TeamMember>();
        for (int i = 0; i < 32; i++) {
            members.add(new TeamMember(UUID.randomUUID(), repeat('n', 16), 0).withPosition(0, 0, 0, repeat('d', 64)));
        }
        byte[] full = BeezyCodec.team(members);
        assertTrue(full.length <= BeezyProtocol.MAX_MESSAGE_BYTES);
        members.add(new TeamMember(u, "n", 0));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.team(members));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.team(Collections.<TeamMember>singletonList(null)));
    }

    @Test
    void richPresenceCaps() {
        RichPresence.builder(repeat('g', 32)).mode(repeat('m', 32)).map(repeat('p', 32)).state(repeat('s', 64)).team(255, 255).build();
        assertThrows(IllegalArgumentException.class, () -> RichPresence.builder(repeat('g', 33)).build());
        assertThrows(IllegalArgumentException.class, () -> RichPresence.builder("g").mode(repeat('m', 33)).build());
        assertThrows(IllegalArgumentException.class, () -> RichPresence.builder("g").map(repeat('p', 33)).build());
        assertThrows(IllegalArgumentException.class, () -> RichPresence.builder("g").state(repeat('s', 65)).build());
        assertThrows(IllegalArgumentException.class, () -> RichPresence.builder("g").team(256, 0).build());
        assertThrows(IllegalArgumentException.class, () -> RichPresence.builder("g").team(0, -1).build());
    }

    // ---- Hello ------------------------------------------------------------------------------

    private static final byte[] HELLO = bytes(1, 1, 6, "1.21.1", 3, "0.2", 0, 0, 0, 0x7F);

    @Test
    void decodeHello() {
        Hello h = BeezyCodec.decodeHello(HELLO);
        assertNotNull(h);
        assertEquals("1.21.1", h.minecraftVersion());
        assertEquals("0.2", h.beezyVersion());
        assertEquals(0x7F, h.featureBits());
        for (Feature f : Feature.values()) {
            assertTrue(h.has(f), f.name());
        }
        assertArrayEquals(HELLO, BeezyCodec.hello(h));
    }

    @Test
    void helloFeatureBits() {
        Hello h = BeezyCodec.decodeHello(bytes(1, 1, 5, "1.8.9", 3, "0.2", 0x80, 0, 0, 0x22));
        assertNotNull(h);
        assertTrue(h.has(Feature.NOTIFICATIONS));
        assertTrue(h.has(Feature.TEAM));
        assertFalse(h.has(Feature.WAYPOINTS));
        assertFalse(h.has(Feature.RICH_PRESENCE));
        assertEquals(0x80000022, h.featureBits()); // unknown high bits are kept
        assertEquals(0, Feature.WAYPOINTS.bit());
        assertEquals(6, Feature.RICH_PRESENCE.bit());
    }

    @Test
    void helloTrailingBytesAreIgnored() {
        byte[] longer = Arrays.copyOf(HELLO, HELLO.length + 3);
        assertNotNull(BeezyCodec.decodeHello(longer));
    }

    @Test
    void malformedHelloIsNull() {
        assertNull(BeezyCodec.decodeHello(null));
        assertNull(BeezyCodec.decodeHello(new byte[0]));
        assertNull(BeezyCodec.decodeHello(bytes(1)));
        // wrong format, wrong kind
        assertNull(BeezyCodec.decodeHello(bytes(2, 1, 0, 0, 0, 0, 0, 0)));
        assertNull(BeezyCodec.decodeHello(bytes(1, 2, 0, 0, 0, 0, 0, 0)));
        // every truncation of a valid Hello
        for (int len = 0; len < HELLO.length; len++) {
            assertNull(BeezyCodec.decodeHello(Arrays.copyOf(HELLO, len)), "truncated to " + len);
        }
        // string over its cap (33 bytes)
        byte[] over = new byte[2 + 1 + 33 + 1 + 4];
        over[0] = 1;
        over[1] = 1;
        over[2] = 33;
        Arrays.fill(over, 3, 36, (byte) 'a');
        assertNull(BeezyCodec.decodeHello(over));
        // invalid UTF-8
        assertNull(BeezyCodec.decodeHello(bytes(1, 1, 1, 0xFF, 0, 0, 0, 0, 0)));
        // over 32 KiB
        assertNull(BeezyCodec.decodeHello(Arrays.copyOf(HELLO, BeezyProtocol.MAX_MESSAGE_BYTES + 1)));
    }
}
