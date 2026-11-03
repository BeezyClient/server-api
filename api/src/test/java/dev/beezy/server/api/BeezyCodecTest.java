package dev.beezy.server.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Byte-for-byte checks of every kind against PROTOCOL.md. */
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
    void emptyIdsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.waypointRemove(""));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.cooldownRemove(""));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.timerRemove(""));
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder("").build());
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.beamRemove(""));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.borderRemove(""));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.hologramRemove(""));
    }

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
                .world("minecraft:overworld").color(new Color(0x55, 0xFF, 0x55)).beam(true)
                .duration(Duration.ofSeconds(90)).build();
        assertArrayEquals(bytes(1, 10,
                        1, "a",
                        5, "Spawn",
                        0, 0, 0, 1,
                        0xFF, 0xFF, 0xFF, 0xFE,
                        1, 2, 3, 4,
                        19, "minecraft:overworld",
                        0xFF, 0x55, 0xFF, 0x55,
                        1,
                        0x00, 0x01, 0x5F, 0x90),          // 90 000 ms
                BeezyCodec.waypoint(w));
    }

    @Test
    void waypointDefaults() {
        // name defaults to the id, world to "any", color to the client's default, no beam, no duration
        Waypoint w = Waypoint.builder("home").build();
        assertArrayEquals(bytes(1, 10, 4, "home", 4, "home", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0),
                BeezyCodec.waypoint(w));
        assertEquals(null, w.color());
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
        members.add(new TeamMember(a, "Alice", new Color(0xFF5555)));
        members.add(new TeamMember(b, "Bob", null).withPosition(1.5, -2.0, 0.0, "minecraft:overworld"));
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

    // ---- beams, borders, glow, holograms -----------------------------------------------------

    private static final UUID U = new UUID(0x0102030405060708L, 0x090A0B0C0D0E0F10L);
    private static final Object[] U_BYTES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    /** {@code head}, the uuid's 16 bytes, then {@code tail}. */
    private static byte[] withUuid(Object[] head, Object... tail) {
        List<Object> all = new ArrayList<Object>(Arrays.asList(head));
        all.addAll(Arrays.asList(U_BYTES));
        all.addAll(Arrays.asList(tail));
        return bytes(all.toArray());
    }

    @Test
    void colorsAreOpaqueRgbAndNullIsDefault() {
        assertEquals(0xFF123456, Check.argb(new Color(0x123456)));
        assertEquals(0xFF112233, Check.argb(new Color(0x11, 0x22, 0x33, 0))); // alpha ignored
        assertEquals(0, Check.argb(null));
        assertEquals(0xFF000000, Check.argb(Color.BLACK)); // black is not "default"
    }

    @Test
    void beam() {
        Beam b = Beam.builder("b").at(1, 64, -1).world("minecraft:the_end").color(Color.YELLOW)
                .duration(Duration.ofSeconds(30)).build();
        assertArrayEquals(bytes(1, 80, 1, "b",
                        0, 0, 0, 1, 0, 0, 0, 64, 0xFF, 0xFF, 0xFF, 0xFF,
                        17, "minecraft:the_end",
                        0xFF, 0xFF, 0xFF, 0x00,
                        0, 0, 0x75, 0x30),
                BeezyCodec.beam(b));
        assertArrayEquals(bytes(1, 80, 1, "b", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                BeezyCodec.beam(Beam.builder("b").build()));
        assertArrayEquals(bytes(1, 81, 1, "b"), BeezyCodec.beamRemove("b"));
        assertArrayEquals(bytes(1, 82), BeezyCodec.beamsClear());
    }

    @Test
    void border() {
        // corners in any order: sent as min x, min z, max x, max z
        Border b = Border.builder("arena").area(10, -5, -10, 5).solid(true).onlyWhenNear(true).build();
        assertArrayEquals(bytes(1, 90, 5, "arena", 0,
                        0xC0, 0x24, 0, 0, 0, 0, 0, 0,     // -10
                        0xC0, 0x14, 0, 0, 0, 0, 0, 0,     // -5
                        0x40, 0x24, 0, 0, 0, 0, 0, 0,     // 10
                        0x40, 0x14, 0, 0, 0, 0, 0, 0,     // 5
                        0, 0, 0, 0,
                        3,
                        0, 0, 0, 0),
                BeezyCodec.border(b));
        byte[] solid = BeezyCodec.border(Border.builder("s").world("w").color(Color.RED).solid(true)
                .duration(Duration.ofMillis(1)).build());
        assertArrayEquals(bytes(0xFF, 0xFF, 0, 0, 1, 0, 0, 0, 1), Arrays.copyOfRange(solid, solid.length - 9, solid.length));
        byte[] near = BeezyCodec.border(Border.builder("n").onlyWhenNear(true).build());
        assertEquals(2, near[near.length - 5]);
        assertArrayEquals(bytes(1, 91, 5, "arena"), BeezyCodec.borderRemove("arena"));
        assertArrayEquals(bytes(1, 92), BeezyCodec.bordersClear());
    }

    @Test
    void glow() {
        assertArrayEquals(withUuid(new Object[] {1, 100}, 0xFF, 0x12, 0x34, 0x56), BeezyCodec.glow(U, new Color(0x123456)));
        assertArrayEquals(withUuid(new Object[] {1, 100}, 0, 0, 0, 0), BeezyCodec.glow(U, null));
        assertArrayEquals(withUuid(new Object[] {1, 101}), BeezyCodec.glowRemove(U));
        assertArrayEquals(bytes(1, 102), BeezyCodec.glowClear());
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.glow(null, Color.RED));
    }

    @Test
    void hologram() {
        Hologram h = Hologram.builder("h").at(0.5, 66, -0.5).world("w").lines("§6Shop", "x")
                .duration(Duration.ofMillis(1)).build();
        assertArrayEquals(bytes(1, 110, 1, "h",
                        0x3F, 0xE0, 0, 0, 0, 0, 0, 0,
                        0x40, 0x50, 0x80, 0, 0, 0, 0, 0,
                        0xBF, 0xE0, 0, 0, 0, 0, 0, 0,
                        1, "w",
                        2, 7, "§6Shop", 1, "x",
                        0, 0, 0, 1),
                BeezyCodec.hologram(h));
        assertArrayEquals(bytes(1, 111, 1, "h"), BeezyCodec.hologramRemove("h"));
        assertArrayEquals(bytes(1, 112), BeezyCodec.hologramsClear());
        // a positive duration under 1 ms is 1 ms, never "until removed"
        assertEquals(1, Hologram.builder("h").line("a").duration(Duration.ofNanos(1)).build().durationMs());
    }

    // ---- module settings, nametags, nick, food, TNT, transfer ----------------------------------

    @Test
    void moduleSettings() {
        ModuleSettings s = ModuleSettings.builder()
                .set("zoom", "max_zoom", 4)
                .set("freelook", false)
                .set("hud", "scale", 1.5)
                .set("chat", "prefix", "hi")
                .set("crosshair", "color", new Color(0xFF0000))
                .set("zoom", "max_zoom", 6)   // replaces, keeps its place
                .build();
        assertArrayEquals(bytes(1, 120, 5,
                        4, "zoom", 8, "max_zoom", 1, 0, 0, 0, 6,
                        8, "freelook", 0, 0, 0,
                        3, "hud", 5, "scale", 2, 0x3F, 0xF8, 0, 0, 0, 0, 0, 0,
                        4, "chat", 6, "prefix", 3, 2, "hi",
                        9, "crosshair", 5, "color", 4, 0xFF, 0xFF, 0, 0),
                BeezyCodec.moduleSettings(s));
        assertArrayEquals(bytes(1, 120, 1, 1, "m", 1, "o", 1, 0xFF, 0xFF, 0xFF, 0xFF),
                BeezyCodec.moduleSettings(ModuleSettings.builder().set("m", "o", -1).build()));
        assertArrayEquals(bytes(1, 120, 1, 1, "m", 0, 0, 1),
                BeezyCodec.moduleSettings(ModuleSettings.builder().set("m", true).build()));
    }

    @Test
    void moduleSettingsRelease() {
        assertArrayEquals(bytes(1, 121, 2, 4, "zoom", 8, "max_zoom", 8, "freelook", 0),
                BeezyCodec.moduleSettingsRelease(Arrays.asList(SettingKey.of("zoom", "max_zoom"), SettingKey.of("freelook"))));
        assertArrayEquals(bytes(1, 121, 0), BeezyCodec.moduleSettingsReleaseAll());
        // an empty release would mean "all": only releaseAll sends that
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.moduleSettingsRelease(Collections.<SettingKey>emptyList()));
    }

    @Test
    void moduleSettingsCaps() {
        assertThrows(IllegalArgumentException.class, () -> ModuleSettings.builder().build());
        ModuleSettings.Builder b = ModuleSettings.builder();
        for (int i = 0; i < 64; i++) {
            b.set(repeat('m', 32), repeat('o', 30) + String.format("%02d", i), repeat('t', 64));
        }
        assertTrue(BeezyCodec.moduleSettings(b.build()).length <= BeezyProtocol.MAX_MESSAGE_BYTES);
        b.set("one", "more", true);
        assertThrows(IllegalArgumentException.class, b::build);
        assertThrows(IllegalArgumentException.class, () -> SettingKey.of(""));
        assertThrows(IllegalArgumentException.class, () -> SettingKey.of(repeat('m', 33)));
        assertThrows(IllegalArgumentException.class, () -> SettingKey.of("m", repeat('o', 33)));
        assertThrows(IllegalArgumentException.class, () -> SettingValue.of(repeat('t', 65)));
        assertThrows(IllegalArgumentException.class, () -> SettingValue.of(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> SettingValue.of((Color) null));
        assertEquals(SettingKey.of("zoom"), SettingKey.of("zoom", null));
    }

    @Test
    void nametag() {
        assertArrayEquals(withUuid(new Object[] {1, 130}, 2, 6, "§cRed", 3, "Bob"), BeezyCodec.nametag(Nametag.of(U, "§cRed", "Bob")));
        assertArrayEquals(withUuid(new Object[] {1, 130}, 0), BeezyCodec.nametag(Nametag.hidden(U)));
        assertArrayEquals(withUuid(new Object[] {1, 131}), BeezyCodec.nametagReset(U));
        assertArrayEquals(bytes(1, 132), BeezyCodec.nametagsClear());
        Nametag.of(U, "1", "2", "3", "4", repeat('l', 64));
        assertThrows(IllegalArgumentException.class, () -> Nametag.of(U, "1", "2", "3", "4", "5", "6"));
        assertThrows(IllegalArgumentException.class, () -> Nametag.of(U, repeat('l', 65)));
        assertThrows(IllegalArgumentException.class, () -> Nametag.of(null, "a"));
    }

    @Test
    void nickHider() {
        assertArrayEquals(bytes(1, 140, 5, "Steve"), BeezyCodec.nickHider("Steve"));
        assertArrayEquals(bytes(1, 141), BeezyCodec.nickHiderClear());
        assertNotNull(BeezyCodec.nickHider(repeat('n', 16)));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.nickHider(repeat('n', 17)));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.nickHider(""));
    }

    @Test
    void foodValues() {
        List<FoodValue> values = Arrays.asList(new FoodValue("minecraft:golden_carrot", 6, 14.4), new FoodValue("x:y", 0, 0));
        assertArrayEquals(bytes(1, 150, 2,
                        23, "minecraft:golden_carrot", 6, 0x41, 0x66, 0x66, 0x66,
                        3, "x:y", 0, 0, 0, 0, 0),
                BeezyCodec.foodValues(values));
        assertArrayEquals(bytes(1, 151), BeezyCodec.foodValuesClear());
        new FoodValue(repeat('i', 64), 20, 20);
        assertThrows(IllegalArgumentException.class, () -> new FoodValue(repeat('i', 65), 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new FoodValue("i", 21, 1));
        assertThrows(IllegalArgumentException.class, () -> new FoodValue("i", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new FoodValue("i", 1, 20.5));
        assertThrows(IllegalArgumentException.class, () -> new FoodValue("i", 1, -0.1));
        assertThrows(IllegalArgumentException.class, () -> new FoodValue("i", 1, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.foodValues(Collections.<FoodValue>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.foodValues(Collections.nCopies(65, new FoodValue("i", 1, 1))));
    }

    @Test
    void tntCountdown() {
        assertArrayEquals(bytes(1, 160, 0, 80), BeezyCodec.tntCountdown(80));
        assertArrayEquals(bytes(1, 160, 0x04, 0xB0), BeezyCodec.tntCountdown(1200));
        assertArrayEquals(bytes(1, 161), BeezyCodec.tntCountdownClear());
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.tntCountdown(0));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.tntCountdown(1201));
        assertEquals(80, BeezyCodec.tntTicks(Duration.ofSeconds(4)));
        assertEquals(1, BeezyCodec.tntTicks(Duration.ofMillis(30)));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.tntTicks(Duration.ofMillis(10)));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.tntTicks(Duration.ofSeconds(61)));
    }

    @Test
    void transfer() {
        assertArrayEquals(bytes(1, 170, 2, "t1", 22, "play.example.net:25565"), BeezyCodec.transfer("t1", "play.example.net:25565"));
        assertNotNull(BeezyCodec.transfer("t", repeat('a', 255)));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.transfer("t", repeat('a', 256)));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.transfer("t", ""));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.transfer("", "a"));
    }

    @Test
    void decodeTransferResult() {
        String[] names = {"CONNECTING", "REFUSED", "INVALID_ADDRESS", "FAILED"};
        for (int status = 0; status < 4; status++) {
            byte[] wire = bytes(1, 3, 2, "t1", status);
            TransferResult r = BeezyCodec.decodeTransferResult(wire);
            assertNotNull(r);
            assertEquals("t1", r.id());
            assertEquals(names[status], r.status().name());
            assertEquals(status, r.status().id());
            assertArrayEquals(wire, BeezyCodec.transferResult(r));
            assertEquals(r, BeezyCodec.decode(wire));
        }
        assertNotNull(BeezyCodec.decodeTransferResult(bytes(1, 3, 1, "t", 0, 9, 9))); // trailing bytes
        assertNull(BeezyCodec.decodeTransferResult(bytes(1, 3, 1, "t", 4)));          // unknown status
        assertNull(BeezyCodec.decodeTransferResult(bytes(1, 3, 0, 0)));               // empty id
        assertNull(BeezyCodec.decodeTransferResult(bytes(1, 1, 1, "t", 0)));          // a Hello's kind
        assertNull(BeezyCodec.decodeTransferResult(bytes(2, 3, 1, "t", 0)));          // format 2
        byte[] valid = bytes(1, 3, 1, "t", 0);
        for (int len = 0; len < valid.length; len++) {
            assertNull(BeezyCodec.decodeTransferResult(Arrays.copyOf(valid, len)), "truncated to " + len);
        }
        assertNull(BeezyCodec.decode(bytes(1, 2)));
        assertNull(BeezyCodec.decode(null));
        assertTrue(BeezyCodec.decode(HELLO) instanceof Hello);
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
        Waypoint.builder(repeat('i', 32)).name(repeat('n', 32)).world(repeat('d', 64)).duration(Duration.ofHours(24)).build();
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder(repeat('i', 33)).build());
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder("a").name(repeat('n', 33)).build());
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder("a").world(repeat('d', 65)).build());
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder("a").duration(Duration.ofHours(24).plusMillis(1)).build());
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder("a").duration(Duration.ofMillis(-1)).build());
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
    void beamBorderHologramCaps() {
        assertThrows(IllegalArgumentException.class, () -> Beam.builder(repeat('i', 33)).build());
        assertThrows(IllegalArgumentException.class, () -> Beam.builder("b").world(repeat('w', 65)).build());
        assertThrows(IllegalArgumentException.class, () -> Beam.builder("b").duration(Duration.ofDays(2)).build());
        assertThrows(IllegalArgumentException.class, () -> Border.builder("b").area(Double.NaN, 0, 1, 1).build());
        assertThrows(IllegalArgumentException.class, () -> Border.builder("b").area(0, 0, Double.POSITIVE_INFINITY, 1).build());
        assertThrows(IllegalArgumentException.class, () -> Hologram.builder("h").build()); // no lines
        assertThrows(IllegalArgumentException.class, () -> Hologram.builder("h").lines("1", "2", "3", "4", "5", "6", "7", "8", "9").build());
        assertThrows(IllegalArgumentException.class, () -> Hologram.builder("h").line(repeat('l', 129)).build());
        assertThrows(IllegalArgumentException.class, () -> Hologram.builder("h").line("a").at(Double.NaN, 0, 0).build());
        Hologram.Builder full = Hologram.builder(repeat('i', 32)).world(repeat('w', 64));
        for (int i = 0; i < 8; i++) {
            full.line(repeat('l', 128));
        }
        assertTrue(BeezyCodec.hologram(full.build()).length <= BeezyProtocol.MAX_MESSAGE_BYTES);
    }

    @Test
    void heightLimitCaps() {
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.heightLimit(repeat('d', 65), 0, 1));
        assertThrows(IllegalArgumentException.class, () -> BeezyCodec.heightLimit("", 10, 0));
    }

    @Test
    void teamCaps() {
        UUID u = UUID.randomUUID();
        new TeamMember(u, repeat('n', 16), null);
        assertThrows(IllegalArgumentException.class, () -> new TeamMember(u, repeat('n', 17), null));
        assertThrows(IllegalArgumentException.class, () -> new TeamMember(null, "n", null));
        assertThrows(IllegalArgumentException.class, () -> new TeamMember(u, "n", null).withPosition(Double.NaN, 0, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new TeamMember(u, "n", null).withPosition(0, 0, 0, repeat('d', 65)));
        List<TeamMember> members = new ArrayList<TeamMember>();
        for (int i = 0; i < 32; i++) {
            members.add(new TeamMember(UUID.randomUUID(), repeat('n', 16), Color.RED).withPosition(0, 0, 0, repeat('d', 64)));
        }
        byte[] full = BeezyCodec.team(members);
        assertTrue(full.length <= BeezyProtocol.MAX_MESSAGE_BYTES);
        members.add(new TeamMember(u, "n", null));
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

    private static final byte[] HELLO = bytes(1, 1, 6, "1.21.1", 3, "0.2", 0, 0x01, 0xFF, 0xFF);

    @Test
    void decodeHello() {
        Hello h = BeezyCodec.decodeHello(HELLO);
        assertNotNull(h);
        assertEquals("1.21.1", h.minecraftVersion());
        assertEquals("0.2", h.beezyVersion());
        assertEquals(0x1FFFF, h.featureBits());
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
        Feature[] newer = {Feature.BEAMS, Feature.BORDERS, Feature.GLOW, Feature.HOLOGRAMS, Feature.MODULE_SETTINGS,
                Feature.NAMETAGS, Feature.NICK_HIDER, Feature.FOOD_VALUES, Feature.TNT_COUNTDOWN, Feature.TRANSFER};
        for (int i = 0; i < newer.length; i++) {
            assertEquals(7 + i, newer[i].bit(), newer[i].name());
        }
        assertEquals(17, Feature.values().length);
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
