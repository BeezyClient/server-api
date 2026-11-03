package dev.beezy.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beezy.api.BeezyUser;
import dev.beezy.api.DeliveryPolicy;
import dev.beezy.api.DeliveryReport;
import dev.beezy.api.Feature;
import dev.beezy.api.SendResult;
import dev.beezy.api.display.Cooldown;
import dev.beezy.api.display.Hologram;
import dev.beezy.api.display.HologramHandle;
import dev.beezy.api.display.Notification;
import dev.beezy.api.display.Timer;
import dev.beezy.api.display.TimerHandle;
import dev.beezy.api.display.Waypoint;
import dev.beezy.api.display.WaypointHandle;
import dev.beezy.api.event.TransferResultEvent;
import dev.beezy.api.event.UserDisconnectEvent;
import dev.beezy.api.event.UserHelloEvent;
import dev.beezy.api.exception.FeatureUnavailableException;
import dev.beezy.api.exception.LimitExceededException;
import dev.beezy.api.exception.RateLimitedException;
import dev.beezy.api.exception.TransferTimeoutException;
import dev.beezy.api.exception.UserDisconnectedException;
import dev.beezy.api.settings.ModuleSettings;
import dev.beezy.api.settings.SettingKey;
import dev.beezy.api.transfer.TransferResult;
import dev.beezy.api.value.Color;
import dev.beezy.api.value.Position;
import dev.beezy.api.value.WorldKey;
import dev.beezy.protocol.JsonText;
import dev.beezy.protocol.Packet;
import dev.beezy.protocol.PacketCodec;
import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.TransferStatus;
import dev.beezy.protocol.packet.CooldownSetPacket;
import dev.beezy.protocol.packet.HelloPacket;
import dev.beezy.protocol.packet.HologramSetPacket;
import dev.beezy.protocol.packet.ModuleSettingsReleasePacket;
import dev.beezy.protocol.packet.TimerSetPacket;
import dev.beezy.protocol.packet.TransferPacket;
import dev.beezy.protocol.packet.TransferResultPacket;
import dev.beezy.protocol.packet.WaypointSetPacket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BeezyCoreTest {

    private static final int ALL = 0x1FFFF;
    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private FakePlatform platform;
    private BeezyCore core;

    @BeforeEach
    void setUp() {
        platform = new FakePlatform();
        core = core(DeliveryPolicy.SKIP);
    }

    private BeezyCore core(DeliveryPolicy policy) {
        return new BeezyCore(platform, CoreConfig.builder().policy(policy).clock(platform.clock).build());
    }

    private BeezyUser hello(BeezyCore c, UUID player, int features) {
        c.handleIncoming(player, PacketCodec.encode(new HelloPacket("1.21.4", "0.2.0", features)));
        return c.user(player).get();
    }

    private List<Packet> packets(UUID player) throws Exception {
        List<Packet> out = new ArrayList<Packet>();
        for (byte[] b : platform.drain(player)) {
            out.add(PacketCodec.decode(b));
        }
        return out;
    }

    private Packet only(UUID player) throws Exception {
        List<Packet> p = packets(player);
        assertEquals(1, p.size(), "one message");
        return p.get(0);
    }

    @Test
    void helloMakesAUserAndFiresEvents() {
        List<UserHelloEvent> events = new ArrayList<UserHelloEvent>();
        core.events().subscribe(UserHelloEvent.class, events::add);
        assertFalse(core.isBeezy(id));
        BeezyUser user = hello(core, id, Feature.WAYPOINTS.mask() | Feature.TRANSFER.mask());
        assertTrue(core.isBeezy(id));
        assertEquals("0.2.0", user.client().beezyVersion());
        assertTrue(user.supports(Feature.WAYPOINTS));
        assertFalse(user.supports(Feature.HOLOGRAMS));
        hello(core, id, ALL);
        assertEquals(2, events.size());
        assertTrue(events.get(0).isFirst());
        assertFalse(events.get(1).isFirst());
        assertTrue(user.supports(Feature.HOLOGRAMS), "a repeated Hello updates the features");
        assertTrue(platform.sent.isEmpty(), "a Hello is never answered on its own");
    }

    @Test
    void malformedAndUnknownMessagesAreDropped() {
        core.handleIncoming(id, new byte[] {1, (byte) 250});
        core.handleIncoming(id, new byte[] {9});
        core.handleIncoming(id, PacketCodec.encode(new TransferResultPacket("t1", TransferStatus.CONNECTING)));
        assertFalse(core.isBeezy(id));
    }

    @Test
    void hologramLinesAreAdventureComponentsAsJson() throws Exception {
        BeezyUser user = hello(core, id, ALL);
        Component gold = Component.text("Shop", NamedTextColor.GOLD);
        Component fancy = Component.text().content("Sale ").color(net.kyori.adventure.text.format.TextColor.color(0xFF8800))
                .decorate(TextDecoration.BOLD).append(Component.text("50%")).build();
        HologramHandle h = user.holograms().show(Hologram.builder("shop").at(Position.of(10.5, 72, -3.5))
                .world(WorldKey.OVERWORLD).line(gold).line(fancy).line("§6Shop").line("plain").duration(Duration.ofMinutes(10)).build());
        assertTrue(h.isActive());
        HologramSetPacket p = (HologramSetPacket) only(id);
        String goldJson = GsonComponentSerializer.gson().serialize(gold);
        assertEquals("{\"color\":\"gold\",\"text\":\"Shop\"}", goldJson);
        assertEquals(Arrays.asList(JsonText.of(goldJson), JsonText.of(GsonComponentSerializer.gson().serialize(fancy)),
                JsonText.of(goldJson), JsonText.of("\"plain\"")), p.lines(), "legacy § strings become the same component; plain text is a bare JSON string");
        assertEquals(600_000, p.durationMs());
        assertEquals("minecraft:overworld", p.world());
        // the bytes carry the component as u16 length + JSON
        byte[] bytes = PacketCodec.encode(p);
        String wire = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(wire.contains(goldJson));
    }

    @Test
    void oversizedComponentIsRejected() {
        BeezyUser user = hello(core, id, ALL);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append('x');
        }
        Hologram big = Hologram.builder("big").at(0, 0, 0).line(Component.text(sb.toString())).build();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> user.holograms().show(big));
        assertTrue(e.getMessage().contains("hologram line 1"), e.getMessage());
        assertTrue(platform.sent.isEmpty());
    }

    @Test
    void handlesUpdateRemoveAndReplace() throws Exception {
        BeezyUser user = hello(core, id, ALL);
        Waypoint w = Waypoint.builder("flag").at(1, 2, 3).name("Flag").color(Color.RED).beam(true).build();
        WaypointHandle h = user.waypoints().show(w);
        WaypointSetPacket set = (WaypointSetPacket) only(id);
        assertEquals(0xFFFF5555, set.argb());
        assertEquals(WaypointSetPacket.FLAG_BEAM, set.flags());
        assertEquals(0, set.durationMs());
        assertTrue(h.isActive());
        assertEquals(h, user.waypoints().get("flag").get());

        assertEquals(SendResult.SENT, h.update(w.toBuilder().name("Moved").build()));
        assertEquals("Moved", ((WaypointSetPacket) only(id)).name());
        assertThrows(IllegalArgumentException.class, () -> h.update(Waypoint.builder("other").at(0, 0, 0).build()));

        WaypointHandle replacement = user.waypoints().show(w);
        packets(id);
        assertFalse(h.isActive(), "the same id replaces");
        assertTrue(replacement.isActive());

        assertEquals(SendResult.SENT, replacement.remove());
        assertEquals(PacketKind.WAYPOINT_REMOVE, only(id).kind());
        assertFalse(replacement.isActive());
        assertEquals(SendResult.SENT, replacement.remove());
        assertTrue(packets(id).isEmpty(), "removing an inactive handle sends nothing");
        assertTrue(user.waypoints().active().isEmpty());
    }

    @Test
    void durationIsOnlyWrittenIntoTheMessage() throws Exception {
        BeezyUser user = hello(core, id, ALL);
        WaypointHandle h = user.waypoints().show(Waypoint.builder("w").at(0, 0, 0).duration(Duration.ofSeconds(30)).build());
        assertEquals(30_000, ((WaypointSetPacket) only(id)).durationMs());
        assertTrue(platform.tasks.isEmpty(), "nothing is scheduled for a duration");
        assertEquals(platform.clock.now.plusSeconds(30), h.expiresAt().get());
        platform.advance(Duration.ofSeconds(29));
        assertTrue(h.isActive());
        platform.advance(Duration.ofSeconds(1));
        assertFalse(h.isActive(), "expiry is arithmetic on the send time");
        assertTrue(user.waypoints().active().isEmpty(), "dropped lazily");
        assertTrue(platform.sent.isEmpty(), "and no remove message is sent");

        // sub-millisecond durations never become "until removed"
        user.waypoints().show(Waypoint.builder("w").at(0, 0, 0).duration(Duration.ofNanos(10)).build());
        assertEquals(1, ((WaypointSetPacket) only(id)).durationMs());
    }

    @Test
    void cooldownsEndByThemselves() throws Exception {
        BeezyUser user = hello(core, id, ALL);
        user.cooldowns().show(Cooldown.of("pearl", "Ender Pearl", Duration.ofSeconds(15)).withIcon("minecraft:ender_pearl"));
        CooldownSetPacket p = (CooldownSetPacket) only(id);
        assertEquals(15_000, p.durationMs());
        assertEquals("minecraft:ender_pearl", p.itemIcon());
        platform.advance(Duration.ofSeconds(15));
        assertFalse(user.cooldowns().get("pearl").isPresent());
    }

    @Test
    void featureGatingUnderBothPolicies() throws Exception {
        BeezyUser user = hello(core, id, Feature.WAYPOINTS.mask());
        assertEquals(SendResult.FEATURE_UNAVAILABLE, user.notifications().send("a", "b"));
        HologramHandle h = user.holograms().show(Hologram.builder("h").at(0, 0, 0).line("x").build());
        assertFalse(h.isActive());
        assertEquals(SendResult.FEATURE_UNAVAILABLE, h.result());
        assertTrue(platform.sent.isEmpty());
        assertThrows(FeatureUnavailableException.class, () -> user.require(Feature.BEAMS));

        BeezyCore strict = core(DeliveryPolicy.THROW);
        UUID other = UUID.randomUUID();
        BeezyUser u2 = hello(strict, other, Feature.WAYPOINTS.mask());
        FeatureUnavailableException e = assertThrows(FeatureUnavailableException.class, () -> u2.notifications().send("a", "b"));
        assertEquals(Feature.NOTIFICATIONS, e.feature());
        assertTrue(packets(other).isEmpty());
    }

    @Test
    void countLimits() throws Exception {
        BeezyUser user = hello(core, id, ALL);
        for (int i = 0; i < 8; i++) {
            assertTrue(user.timers().show(Timer.stopwatch("t" + i, "")).isActive());
        }
        assertEquals(8, user.timers().limit());
        TimerHandle ninth = user.timers().show(Timer.stopwatch("t8", ""));
        assertEquals(SendResult.LIMIT_REACHED, ninth.result());
        assertTrue(user.timers().show(Timer.stopwatch("t0", "again")).isActive(), "replacing an id is not a new one");
        assertEquals(9, packets(id).size());

        BeezyCore strict = core(DeliveryPolicy.THROW);
        BeezyUser u2 = hello(strict, UUID.randomUUID(), ALL);
        for (int i = 0; i < 16; i++) {
            u2.borders().show(dev.beezy.api.display.Border.builder("b" + i).area(0, 0, 1, 1).build());
        }
        LimitExceededException e = assertThrows(LimitExceededException.class,
                () -> u2.borders().show(dev.beezy.api.display.Border.builder("b16").area(0, 0, 1, 1).build()));
        assertEquals(16, e.limit());
    }

    @Test
    void notificationRateLimit() throws Exception {
        BeezyUser user = hello(core, id, ALL);
        for (int i = 0; i < 5; i++) {
            assertEquals(SendResult.SENT, user.notifications().send("n" + i, ""));
        }
        assertEquals(SendResult.RATE_LIMITED, user.notifications().send("n5", ""));
        assertEquals(5, packets(id).size());
        platform.advance(Duration.ofSeconds(10));
        assertEquals(SendResult.SENT, user.notifications().send("later", ""));

        BeezyCore strict = core(DeliveryPolicy.THROW);
        BeezyUser u2 = hello(strict, UUID.randomUUID(), ALL);
        for (int i = 0; i < 5; i++) {
            u2.notifications().send(Notification.builder("x", "y").level(Notification.Level.ERROR).seconds(30).build());
        }
        RateLimitedException e = assertThrows(RateLimitedException.class, () -> u2.notifications().send("x", "y"));
        assertEquals(Duration.ofSeconds(10), e.retryAfter());
    }

    @Test
    void transferCompletesWithTheClientsAnswer() throws Exception {
        BeezyUser user = hello(core, id, ALL);
        List<TransferResultEvent> events = new ArrayList<TransferResultEvent>();
        core.events().subscribe(TransferResultEvent.class, events::add);
        CompletableFuture<TransferResult> f = user.transfer("lobby.example.net:25565").toCompletableFuture();
        TransferPacket sent = (TransferPacket) only(id);
        assertEquals("lobby.example.net:25565", sent.address());
        assertFalse(f.isDone());
        assertEquals(1, platform.tasks.size(), "the answer timeout");

        core.handleIncoming(id, PacketCodec.encode(new TransferResultPacket(sent.id(), TransferStatus.REFUSED)));
        TransferResult r = f.get();
        assertEquals(TransferResult.Status.REFUSED, r.status());
        assertEquals("lobby.example.net:25565", r.address());
        assertEquals(1, events.size());
        assertTrue(platform.tasks.isEmpty(), "timeout cancelled");

        // a second answer for the same id is ignored
        core.handleIncoming(id, PacketCodec.encode(new TransferResultPacket(sent.id(), TransferStatus.CONNECTING)));
        assertEquals(1, events.size());

        // one a minute
        CompletableFuture<TransferResult> again = user.transfer("other.example.net").toCompletableFuture();
        assertInstanceOf(RateLimitedException.class, cause(again));
        assertTrue(platform.sent.isEmpty());
        platform.advance(Duration.ofMinutes(1));
        CompletableFuture<TransferResult> late = user.transfer("other.example.net").toCompletableFuture();
        packets(id);
        platform.advance(Duration.ofSeconds(30));
        assertInstanceOf(TransferTimeoutException.class, cause(late));
    }

    @Test
    void transferFailsWhenOffOrWhenThePlayerLeaves() throws Exception {
        BeezyUser off = hello(core, id, Feature.WAYPOINTS.mask());
        assertInstanceOf(FeatureUnavailableException.class, cause(off.transfer("a.example").toCompletableFuture()));
        assertThrows(IllegalArgumentException.class, () -> off.transfer(""));

        UUID other = UUID.randomUUID();
        BeezyUser user = hello(core, other, ALL);
        CompletableFuture<TransferResult> f = user.transfer("a.example").toCompletableFuture();
        core.handleQuit(other);
        assertInstanceOf(UserDisconnectedException.class, cause(f));
        assertTrue(platform.tasks.isEmpty());
    }

    @Test
    void quitEndsTheSession() {
        List<UserDisconnectEvent> events = new ArrayList<UserDisconnectEvent>();
        core.events().subscribe(UserDisconnectEvent.class, events::add);
        BeezyUser user = hello(core, id, ALL);
        WaypointHandle h = user.waypoints().show(Waypoint.builder("w").at(0, 0, 0).build());
        platform.drain(id);
        core.handleQuit(id);
        assertFalse(user.isConnected());
        assertFalse(h.isActive());
        assertFalse(core.isBeezy(id));
        assertEquals(1, events.size());
        assertEquals(SendResult.DISCONNECTED, user.notifications().send("a", "b"));
        assertTrue(platform.sent.isEmpty());
        BeezyCore strict = core(DeliveryPolicy.THROW);
        BeezyUser u2 = hello(strict, id, ALL);
        strict.handleQuit(id);
        assertThrows(UserDisconnectedException.class, () -> u2.waypoints().clear());
    }

    @Test
    void resetForgetsAndResyncResendsWithRemainingTime() throws Exception {
        BeezyUser user = hello(core, id, ALL);
        user.waypoints().show(Waypoint.builder("w").at(0, 0, 0).duration(Duration.ofSeconds(60)).build());
        user.timers().show(Timer.countdown("round", "Round", Duration.ofMinutes(5)));
        user.settings().set(SettingKey.integer("zoom", "max_zoom"), 4);
        user.richPresence().set(dev.beezy.api.display.RichPresence.builder("Bed Wars").build());
        user.tnt().setFuseTicks(40);
        packets(id);

        platform.advance(Duration.ofSeconds(20));
        assertEquals(SendResult.SENT, user.resync());
        List<Packet> resent = packets(id);
        assertEquals(PacketKind.RESET, resent.get(0).kind());
        assertEquals(40_000, ((WaypointSetPacket) resent.get(1)).durationMs(), "remaining time");
        assertEquals(280_000, ((TimerSetPacket) resent.get(2)).startMs(), "the timer's current value");
        assertEquals(6, resent.size());

        assertEquals(SendResult.SENT, user.reset());
        assertEquals(PacketKind.RESET, only(id).kind());
        assertTrue(user.waypoints().active().isEmpty());
        assertTrue(user.settings().overrides().isEmpty());
        assertFalse(user.richPresence().current().isPresent());
    }

    @Test
    void timerPauseAndResume() throws Exception {
        BeezyUser user = hello(core, id, ALL);
        TimerHandle t = user.timers().show(Timer.countdown("round", "Round", Duration.ofSeconds(100)));
        packets(id);
        platform.advance(Duration.ofSeconds(30));
        assertEquals(Duration.ofSeconds(70), t.current());
        t.pause();
        TimerSetPacket p = (TimerSetPacket) only(id);
        assertEquals(70_000, p.startMs());
        assertFalse(p.running());
        platform.advance(Duration.ofSeconds(30));
        assertEquals(Duration.ofSeconds(70), t.current());
        t.resume();
        assertTrue(((TimerSetPacket) only(id)).running());
    }

    @Test
    void settingsAreTracked() throws Exception {
        BeezyUser user = hello(core, id, ALL);
        SettingKey<Integer> zoom = SettingKey.integer("zoom", "max_zoom");
        user.settings().apply(ModuleSettings.builder().set(zoom, 4).moduleSwitch("freelook", false).set("hud", "accent", Color.GOLD).build());
        assertEquals(Integer.valueOf(4), user.settings().override(zoom).get());
        assertEquals(3, user.settings().overrides().size());
        packets(id);
        user.settings().release(zoom);
        ModuleSettingsReleasePacket rel = (ModuleSettingsReleasePacket) only(id);
        assertEquals("max_zoom", rel.keys().get(0).option());
        assertFalse(user.settings().override(zoom).isPresent());
        user.settings().releaseAll();
        assertEquals(0, ((ModuleSettingsReleasePacket) only(id)).keys().size());
        assertThrows(IllegalArgumentException.class, () -> SettingKey.of("zoom", "", dev.beezy.api.settings.SettingType.INT));
        assertThrows(IllegalArgumentException.class, () -> user.settings().set(SettingKey.text("a", "b"), "12345678901234567890123456789012345678901234567890123456789012345"));
    }

    @Test
    void audiences() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        hello(core, a, ALL);
        hello(core, b, Feature.WAYPOINTS.mask());
        platform.worlds.put(a, WorldKey.THE_NETHER);
        DeliveryReport report = core.all().notify(Notification.of("Event", "Soon"));
        assertEquals(2, report.total());
        assertEquals(1, report.sent());
        assertEquals(1, report.count(SendResult.FEATURE_UNAVAILABLE));
        assertEquals(1, core.all().supporting(Feature.NOTIFICATIONS).size());
        assertEquals(1, core.world(WorldKey.THE_NETHER).size());
        assertEquals(1, core.audience(Arrays.asList(b, UUID.randomUUID())).size());
        assertEquals(2, core.all().showWaypoint(Waypoint.builder("w").at(0, 0, 0).build()).report().sent());
        assertEquals(2, core.all().reset().sent());
    }

    @Test
    void shutdownForgetsEveryone() {
        BeezyUser user = hello(core, id, ALL);
        core.shutdown();
        assertFalse(user.isConnected());
        assertTrue(core.users().isEmpty());
    }

    private static Throwable cause(CompletableFuture<?> f) {
        assertTrue(f.isCompletedExceptionally());
        ExecutionException e = assertThrows(ExecutionException.class, f::get);
        return e.getCause();
    }
}
