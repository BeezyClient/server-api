package dev.beezy.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import dev.beezy.protocol.packet.SettingEntry;
import dev.beezy.protocol.packet.TeamPacket;
import dev.beezy.protocol.packet.TimerSetPacket;
import dev.beezy.protocol.packet.TntCountdownPacket;
import dev.beezy.protocol.packet.TransferPacket;
import dev.beezy.protocol.packet.TransferResultPacket;
import dev.beezy.protocol.packet.WaypointSetPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Every kind, byte for byte against hand-built payloads, and decoded back to the same bytes. */
class PacketCodecTest {

    private static final UUID U = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
    private static final String GOLD = "{\"color\":\"gold\",\"text\":\"Shop\"}";
    private static final String HEX = "{\"color\":\"#ff8800\",\"bold\":true,\"text\":\"§x\",\"extra\":[{\"text\":\"é\"}]}";

    private final Set<PacketKind> covered = EnumSet.noneOf(PacketKind.class);

    private void check(Packet packet, byte[] expected) throws MalformedPacketException {
        byte[] actual = PacketCodec.encode(packet);
        assertArrayEquals(expected, actual, packet.kind().toString());
        Packet back = PacketCodec.decode(actual);
        assertEquals(packet.kind(), back.kind());
        assertArrayEquals(expected, PacketCodec.encode(back), "round trip " + packet.kind());
        assertEquals(packet, back);
        covered.add(packet.kind());
    }

    @Test
    void specExampleNotification() throws Exception {
        check(new NotificationPacket("Welcome", "Have fun!", 0, 5), new Bytes().raw(
                0x01, 0x14, 0x07, 'W', 'e', 'l', 'c', 'o', 'm', 'e', 0x09, 'H', 'a', 'v', 'e', ' ', 'f', 'u', 'n', '!', 0x00, 0x00, 0x05).done());
    }

    @Test
    void everyKind() throws Exception {
        check(new HelloPacket("1.21.4", "0.2.0", 0x1FFFF), Bytes.msg(1).str("1.21.4").str("0.2.0").i32(0x1FFFF).done());
        check(EmptyPacket.reset(), new Bytes().raw(1, 2).done());
        check(new TransferResultPacket("t1", TransferStatus.REFUSED), Bytes.msg(3).str("t1").u8(1).done());

        check(new WaypointSetPacket("flag", "Enemy flag", 120, -64, -40, "minecraft:overworld", 0xFFFF5555, 1, 120_000),
                Bytes.msg(10).str("flag").str("Enemy flag").i32(120).i32(-64).i32(-40).str("minecraft:overworld")
                        .i32(0xFFFF5555).u8(1).i32(120_000).done());
        check(new RemovePacket(PacketKind.WAYPOINT_REMOVE, "flag"), Bytes.msg(11).str("flag").done());
        check(EmptyPacket.of(PacketKind.WAYPOINTS_CLEAR), Bytes.msg(12).done());

        check(new NotificationPacket("Héllo", "x", 3, 30), Bytes.msg(20).str("Héllo").str("x").u8(3).u16(30).done());

        check(new CooldownSetPacket("pearl", "Ender Pearl", "minecraft:ender_pearl", 15_000),
                Bytes.msg(30).str("pearl").str("Ender Pearl").str("minecraft:ender_pearl").i32(15_000).done());
        check(new RemovePacket(PacketKind.COOLDOWN_REMOVE, "pearl"), Bytes.msg(31).str("pearl").done());
        check(EmptyPacket.of(PacketKind.COOLDOWNS_CLEAR), Bytes.msg(32).done());

        check(new TimerSetPacket("round", "Round ends", TimerSetPacket.MODE_COUNTDOWN, 300_000, false),
                Bytes.msg(40).str("round").str("Round ends").u8(0).i32(300_000).u8(0).done());
        check(new TimerSetPacket("up", "", TimerSetPacket.MODE_STOPWATCH, 0xFFFFFFFFL, true),
                Bytes.msg(40).str("up").str("").u8(1).i32(-1).u8(1).done());
        check(new RemovePacket(PacketKind.TIMER_REMOVE, "round"), Bytes.msg(41).str("round").done());
        check(EmptyPacket.of(PacketKind.TIMERS_CLEAR), Bytes.msg(42).done());

        check(new HeightLimitPacket("", -64, 320), Bytes.msg(50).str("").i32(-64).i32(320).done());
        check(EmptyPacket.of(PacketKind.HEIGHT_LIMIT_CLEAR), Bytes.msg(51).done());

        check(new TeamPacket(Arrays.asList(new TeamPacket.Member(U, "Mate", 0xFF5555FF),
                        new TeamPacket.Member(U, "Far", 0, 1.5, 64, -2.25, "minecraft:the_end"))),
                Bytes.msg(60).u8(2).uuid(U).str("Mate").i32(0xFF5555FF).u8(0)
                        .uuid(U).str("Far").i32(0).u8(1).f64(1.5).f64(64).f64(-2.25).str("minecraft:the_end").done());
        check(new TeamPacket(Collections.<TeamPacket.Member>emptyList()), Bytes.msg(60).u8(0).done());
        check(EmptyPacket.of(PacketKind.TEAM_CLEAR), Bytes.msg(61).done());

        check(new RichPresencePacket("Bed Wars", "Solo", "Lighthouse", "In game", 1, 4),
                Bytes.msg(70).str("Bed Wars").str("Solo").str("Lighthouse").str("In game").u8(1).u8(4).done());
        check(EmptyPacket.of(PacketKind.RICH_PRESENCE_CLEAR), Bytes.msg(71).done());

        check(new BeamSetPacket("drop", 1, 2, 3, "", 0xFFFFFF55, 0), Bytes.msg(80).str("drop").i32(1).i32(2).i32(3).str("").i32(0xFFFFFF55).i32(0).done());
        check(new RemovePacket(PacketKind.BEAM_REMOVE, "drop"), Bytes.msg(81).str("drop").done());
        check(EmptyPacket.of(PacketKind.BEAMS_CLEAR), Bytes.msg(82).done());

        check(new BorderSetPacket("arena", "minecraft:overworld", -50, -50.5, 50, 50.5, 0xFFFF0000, 3, 86_400_000),
                Bytes.msg(90).str("arena").str("minecraft:overworld").f64(-50).f64(-50.5).f64(50).f64(50.5)
                        .i32(0xFFFF0000).u8(3).i32(86_400_000).done());
        check(new RemovePacket(PacketKind.BORDER_REMOVE, "arena"), Bytes.msg(91).str("arena").done());
        check(EmptyPacket.of(PacketKind.BORDERS_CLEAR), Bytes.msg(92).done());

        check(new GlowSetPacket(U, 0), Bytes.msg(100).uuid(U).i32(0).done());
        check(new EntityPacket(PacketKind.GLOW_REMOVE, U), Bytes.msg(101).uuid(U).done());
        check(EmptyPacket.of(PacketKind.GLOW_CLEAR), Bytes.msg(102).done());

        check(new HologramSetPacket("shop", 10.5, 72, -3.5, "minecraft:overworld",
                        Arrays.asList(JsonText.of(GOLD), JsonText.of(HEX)), 600_000),
                Bytes.msg(110).str("shop").f64(10.5).f64(72).f64(-3.5).str("minecraft:overworld").u8(2)
                        .component(GOLD).component(HEX).i32(600_000).done());
        check(new RemovePacket(PacketKind.HOLOGRAM_REMOVE, "shop"), Bytes.msg(111).str("shop").done());
        check(EmptyPacket.of(PacketKind.HOLOGRAMS_CLEAR), Bytes.msg(112).done());

        check(new ModuleSettingsPacket(Arrays.asList(
                        SettingEntry.bool("freelook", "", false),
                        SettingEntry.integer("zoom", "max_zoom", 4),
                        SettingEntry.decimal("zoom", "speed", 0.5),
                        SettingEntry.text("chat", "prefix", "»"),
                        SettingEntry.color("hud", "accent", 0xFF00FF00))),
                Bytes.msg(120).u8(5)
                        .str("freelook").str("").u8(0).u8(0)
                        .str("zoom").str("max_zoom").u8(1).i32(4)
                        .str("zoom").str("speed").u8(2).f64(0.5)
                        .str("chat").str("prefix").u8(3).str("»")
                        .str("hud").str("accent").u8(4).i32(0xFF00FF00).done());
        check(new ModuleSettingsReleasePacket(Arrays.asList(new ModuleSettingsReleasePacket.Key("zoom", "max_zoom"))),
                Bytes.msg(121).u8(1).str("zoom").str("max_zoom").done());
        check(ModuleSettingsReleasePacket.all(), Bytes.msg(121).u8(0).done());

        check(new NametagSetPacket(U, Arrays.asList(JsonText.of(GOLD), JsonText.plain("Steve"))),
                Bytes.msg(130).uuid(U).u8(2).component(GOLD).component("{\"text\":\"Steve\"}").done());
        check(new NametagSetPacket(U, Collections.<JsonText>emptyList()), Bytes.msg(130).uuid(U).u8(0).done());
        check(new EntityPacket(PacketKind.NAMETAG_RESET, U), Bytes.msg(131).uuid(U).done());
        check(EmptyPacket.of(PacketKind.NAMETAGS_CLEAR), Bytes.msg(132).done());

        check(new NickHiderPacket("Steve"), Bytes.msg(140).str("Steve").done());
        check(EmptyPacket.of(PacketKind.NICK_HIDER_CLEAR), Bytes.msg(141).done());

        check(new FoodValuesPacket(Arrays.asList(new FoodValuesPacket.Entry("minecraft:golden_carrot", 6, 14.4f))),
                Bytes.msg(150).u8(1).str("minecraft:golden_carrot").u8(6).f32(14.4f).done());
        check(EmptyPacket.of(PacketKind.FOOD_VALUES_CLEAR), Bytes.msg(151).done());

        check(new TntCountdownPacket(40), Bytes.msg(160).u16(40).done());
        check(EmptyPacket.of(PacketKind.TNT_COUNTDOWN_CLEAR), Bytes.msg(161).done());

        check(new TransferPacket("t1", "lobby.example.net:25565"), Bytes.msg(170).str("t1").str("lobby.example.net:25565").done());

        assertEquals(EnumSet.allOf(PacketKind.class), covered, "every kind is covered");
    }

    @Test
    void componentLimits() {
        StringBuilder big = new StringBuilder("{\"text\":\"");
        while (big.length() < 4094) {
            big.append('a');
        }
        big.append("\"}");
        assertEquals(4096, big.length());
        JsonText.of(big.toString());
        assertThrows(IllegalArgumentException.class, () -> JsonText.of(big.toString() + " "));
        assertThrows(IllegalArgumentException.class, () -> JsonText.of(""));
        assertEquals("{\"text\":\"a\\\"b\\\\c\\n\"}", JsonText.plain("a\"b\\c\n").json());
    }

    @Test
    void messageCapIsEnforced() {
        StringBuilder sb = new StringBuilder("{\"text\":\"");
        while (sb.length() < 4094) {
            sb.append('x');
        }
        JsonText line = JsonText.of(sb.append("\"}").toString());
        List<JsonText> lines = new ArrayList<JsonText>();
        for (int i = 0; i < 8; i++) {
            lines.add(line);
        }
        HologramSetPacket p = new HologramSetPacket("h", 0, 0, 0, "", lines, 0);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PacketCodec.encode(p));
        assertTrue(e.getMessage().contains("32768"), e.getMessage());
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RemovePacket(PacketKind.WAYPOINT_REMOVE, ""));
        assertThrows(IllegalArgumentException.class, () -> new RemovePacket(PacketKind.WAYPOINT_REMOVE, "123456789012345678901234567890123"));
        assertThrows(IllegalArgumentException.class, () -> new RemovePacket(PacketKind.TRANSFER, "x"));
        assertThrows(IllegalArgumentException.class, () -> new NotificationPacket("a", "b", 4, 5));
        assertThrows(IllegalArgumentException.class, () -> new NotificationPacket("a", "b", 0, 31));
        assertThrows(IllegalArgumentException.class, () -> new HeightLimitPacket("", 10, 9));
        assertThrows(IllegalArgumentException.class, () -> new BorderSetPacket("b", "", 1, 0, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new BorderSetPacket("b", "", 0, 0, 1, 1, 0, 4, 0));
        assertThrows(IllegalArgumentException.class, () -> new HologramSetPacket("h", Double.NaN, 0, 0, "", Arrays.asList(JsonText.plain("x")), 0));
        assertThrows(IllegalArgumentException.class, () -> new HologramSetPacket("h", 0, 0, 0, "", Collections.<JsonText>emptyList(), 0));
        assertThrows(IllegalArgumentException.class, () -> new WaypointSetPacket("w", "", 0, 0, 0, "", 0, 0, 86_400_001L));
        assertThrows(IllegalArgumentException.class, () -> new TntCountdownPacket(0));
        assertThrows(IllegalArgumentException.class, () -> new FoodValuesPacket.Entry("x", 21, 1));
        assertThrows(IllegalArgumentException.class, () -> new FoodValuesPacket.Entry("x", 1, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new TransferPacket("t", ""));
        assertThrows(IllegalArgumentException.class, () -> EmptyPacket.of(PacketKind.TEAM));
    }

    @Test
    void malformedPayloads() {
        assertMalformed(null, false);
        assertMalformed(new byte[] {1}, false);
        assertMalformed(new byte[] {2, 2}, false);                       // unknown format
        assertMalformed(new byte[] {1, (byte) 200}, true);              // unknown kind
        assertMalformed(Bytes.msg(1).str("1.21").done(), false);         // cut short
        assertMalformed(Bytes.msg(3).str("t1").u8(3).done(), false);     // status 3 is not listed
        assertMalformed(Bytes.msg(3).str("").u8(0).done(), false);       // empty id
        assertMalformed(Bytes.msg(1).raw(2, 0xC3, 0x28).str("x").i32(0).done(), false); // invalid UTF-8
        assertMalformed(Bytes.msg(1).str("123456789012345678901234567890123").str("x").i32(0).done(), false); // over cap
        assertMalformed(Bytes.msg(110).str("h").f64(0).f64(0).f64(0).str("").u8(1).u16(0).i32(0).done(), false); // empty component
        assertMalformed(new byte[Protocol.MAX_MESSAGE_BYTES + 1], false);
    }

    @Test
    void trailingBytesAreIgnoredAndDirectionIsChecked() throws Exception {
        byte[] hello = Bytes.msg(1).str("1.8.9").str("0.2").i32(3).raw(9, 9, 9).done();
        HelloPacket h = (HelloPacket) PacketCodec.decode(hello, Direction.SERVERBOUND);
        assertEquals(3, h.features());
        MalformedPacketException e = assertThrows(MalformedPacketException.class,
                () -> PacketCodec.decode(Bytes.msg(12).done(), Direction.SERVERBOUND));
        assertTrue(e.getMessage().contains("WAYPOINTS_CLEAR"));
    }

    @Test
    void kindRegistry() {
        for (PacketKind k : PacketKind.values()) {
            assertEquals(k, PacketKind.byId(k.id()));
        }
        assertEquals(-1, PacketKind.RESET.featureBit());
        assertEquals(10, PacketKind.HOLOGRAM_SET.featureBit());
        assertEquals(16, PacketKind.TRANSFER.featureBit());
        assertEquals(Direction.SERVERBOUND, PacketKind.TRANSFER_RESULT.direction());
    }

    private static void assertMalformed(byte[] payload, boolean unknownKind) {
        MalformedPacketException e = assertThrows(MalformedPacketException.class, () -> PacketCodec.decode(payload));
        assertEquals(unknownKind, e.isUnknownKind(), e.getMessage());
    }
}
