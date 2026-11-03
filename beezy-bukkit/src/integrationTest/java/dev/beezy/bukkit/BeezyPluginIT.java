package dev.beezy.bukkit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beezy.api.BeezyClientApi;
import dev.beezy.api.BeezyProvider;
import dev.beezy.api.BeezyUser;
import dev.beezy.api.SendResult;
import dev.beezy.api.display.Hologram;
import dev.beezy.api.display.HologramHandle;
import dev.beezy.api.display.Waypoint;
import dev.beezy.api.transfer.TransferResult;
import dev.beezy.api.value.Color;
import dev.beezy.core.spi.Scheduler;
import dev.beezy.protocol.JsonText;
import dev.beezy.protocol.Packet;
import dev.beezy.protocol.PacketCodec;
import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.TransferStatus;
import dev.beezy.protocol.packet.HelloPacket;
import dev.beezy.protocol.packet.HologramSetPacket;
import dev.beezy.protocol.packet.NotificationPacket;
import dev.beezy.protocol.packet.TransferPacket;
import dev.beezy.protocol.packet.TransferResultPacket;
import dev.beezy.protocol.packet.WaypointSetPacket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The real plugin on MockBukkit (Paper 26.3, its own Adventure 5). MockBukkit drops plugin
 * messages, so players here record them.
 */
class BeezyPluginIT {

    private static final int ALL = 0x1FFFF;

    private ServerMock server;
    private BeezyPlugin plugin;

    /** A player that keeps what the server sends on beezy:server. */
    static final class RecordingPlayer extends PlayerMock {
        final List<byte[]> received = new ArrayList<byte[]>();

        RecordingPlayer(ServerMock server, String name) {
            super(server, name, UUID.nameUUIDFromBytes(name.getBytes()));
        }

        @Override
        public void sendPluginMessage(Plugin source, String channel, byte[] message) {
            super.sendPluginMessage(source, channel, message);
            if (Protocol.CHANNEL.equals(channel)) {
                received.add(message);
            }
        }

        List<Packet> drain() throws Exception {
            List<Packet> out = new ArrayList<Packet>();
            for (byte[] b : received) {
                out.add(PacketCodec.decode(b));
            }
            received.clear();
            return out;
        }
    }

    private final List<Event> events = new ArrayList<Event>();

    /** Records Beezy's Bukkit events, as another plugin would receive them. */
    public final class Recorder implements Listener {
        /** @param e the event */
        @EventHandler
        public void onHello(BeezyHelloEvent e) {
            events.add(e);
        }

        /** @param e the event */
        @EventHandler
        public void onTransfer(BeezyTransferResultEvent e) {
            events.add(e);
        }
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(BeezyPlugin.class);
        server.getPluginManager().registerEvents(new Recorder(), MockBukkit.createMockPlugin("Listener"));
    }

    private <E extends Event> E only(Class<E> type) {
        List<E> found = new ArrayList<E>();
        for (Event e : events) {
            if (type.isInstance(e)) {
                found.add(type.cast(e));
            }
        }
        assertEquals(1, found.size(), type.getSimpleName() + " fired once");
        return found.get(0);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private RecordingPlayer join(String name) {
        RecordingPlayer p = new RecordingPlayer(server, name);
        server.addPlayer(p);
        return p;
    }

    private BeezyUser hello(RecordingPlayer p, int features) throws Exception {
        server.getMessenger().dispatchIncomingMessage(p, Protocol.CHANNEL,
                PacketCodec.encode(new HelloPacket("26.3", "0.2.0", features)));
        p.drain();
        return plugin.api().user(p.getUniqueId()).get();
    }

    private static String plain(String message) {
        return ChatColor.stripColor(message);
    }

    @Test
    void enablesAndRegistersEverything() {
        assertTrue(plugin.isEnabled());
        assertTrue(server.getMessenger().isIncomingChannelRegistered(plugin, Protocol.CHANNEL));
        assertTrue(server.getMessenger().isOutgoingChannelRegistered(plugin, Protocol.CHANNEL));
        assertSame(plugin.api(), server.getServicesManager().load(BeezyClientApi.class));
        assertSame(plugin.api(), BeezyClientApi.get());
        assertNotNull(server.getCommandMap().getCommand("beezy"));
        server.getPluginManager().disablePlugin(plugin);
        assertFalse(BeezyProvider.isLoaded());
    }

    @Test
    void resetOnJoinThenHelloMakesABeezyUser() throws Exception {
        RecordingPlayer p = join("Steve");
        assertEquals(1, p.received.size());
        assertArrayEquals(new byte[] {1, 2}, p.received.get(0), "a Reset first");
        assertFalse(plugin.api().isBeezy(p.getUniqueId()));

        BeezyUser user = hello(p, ALL);
        assertEquals("0.2.0", user.client().beezyVersion());
        assertEquals("Steve", user.name().get());
        BeezyHelloEvent helloEvent = only(BeezyHelloEvent.class);
        assertTrue(helloEvent.isFirst());
        assertSame(p, helloEvent.getPlayer());
        assertSame(user, helloEvent.getUser());

        p.disconnect();
        assertFalse(plugin.api().isBeezy(p.getUniqueId()));
        assertFalse(user.isConnected());
    }

    @Test
    void sendsTheRightBytesAndSchedulesNothingForDurations() throws Exception {
        RecordingPlayer p = join("Alex");
        BeezyUser user = hello(p, ALL);
        Location at = new Location(server.getWorlds().isEmpty() ? server.addSimpleWorld("world") : server.getWorlds().get(0), 10.5, 72, -3.5);
        int tasksBefore = server.getScheduler().getPendingTasks().size();

        Component title = Component.text("Shop", NamedTextColor.GOLD);
        HologramHandle h = user.holograms().show(BeezyBukkit.hologram("shop", at).line(title).line("§7Right-click")
                .duration(Duration.ofSeconds(30)).build());
        user.waypoints().show(Waypoint.builder("spawn").at(1, 64, -2).name("Spawn").color(Color.GREEN).duration(Duration.ofSeconds(30)).build());

        List<Packet> sent = p.drain();
        assertEquals(2, sent.size());
        HologramSetPacket holo = (HologramSetPacket) sent.get(0);
        assertEquals(JsonText.of(GsonComponentSerializer.gson().serialize(title)), holo.lines().get(0), "the server's Adventure serializes");
        assertTrue(holo.lines().get(0).json().contains("gold"));
        assertTrue(holo.lines().get(1).json().contains("Right-click"));
        assertEquals(30_000, holo.durationMs());
        assertEquals(10.5, holo.x());
        assertArrayEquals(PacketCodec.encode(new WaypointSetPacket("spawn", "Spawn", 1, 64, -2, "", 0xFF55FF55, 0, 30_000)),
                PacketCodec.encode(sent.get(1)));

        assertEquals(tasksBefore, server.getScheduler().getPendingTasks().size(), "a duration schedules nothing");
        server.getScheduler().performTicks(20 * 31);
        assertTrue(p.received.isEmpty(), "and nothing is sent when it runs out: the client removes it");
        assertTrue(h.expiresAt().isPresent());
        assertEquals(SendResult.SENT, h.remove());
    }

    @Test
    void commandsCheckPermissionsAndUseTheApi() throws Exception {
        RecordingPlayer p = join("Notch");
        hello(p, ALL);
        p.setOp(false);
        p.performCommand("beezy check Notch");
        assertTrue(plain(p.nextMessage()).contains("permission"));
        assertTrue(p.received.isEmpty());

        server.executeConsole("beezy", "check", "Notch");
        assertTrue(plain(server.getConsoleSender().nextMessage()).contains("Notch uses Beezy 0.2.0"));
        assertTrue(plain(server.getConsoleSender().nextMessage()).startsWith("Features on: waypoints, notifications"));

        server.executeConsole("beezy", "notify", "*", "\"Bed", "Wars\"", "Your", "bed", "is", "gone");
        assertEquals("Sent to 1 Beezy player.", plain(server.getConsoleSender().nextMessage()));
        NotificationPacket n = (NotificationPacket) p.drain().get(0);
        assertEquals("Bed Wars", n.title());
        assertEquals("Your bed is gone", n.text());

        p.setOp(true);
        p.performCommand("beezy beam Notch add drop ~ ~ ~ gold");
        assertEquals("Sent to 1 Beezy player.", plain(p.nextMessage()));
        assertEquals(PacketKind.BEAM_SET, p.drain().get(0).kind());

        p.performCommand("beezy waypoint Notch add w 1 2 3");
        p.nextMessage();
        p.drain();
        server.getPluginManager().disablePlugin(plugin);
    }

    @Test
    void transferResultCompletesAndFiresTheEvent() throws Exception {
        RecordingPlayer p = join("Jeb");
        BeezyUser user = hello(p, ALL);
        server.executeConsole("beezy", "transfer", "Jeb", "lobby.example.net");
        TransferPacket t = (TransferPacket) p.drain().get(0);
        assertEquals("lobby.example.net", t.address());
        assertEquals(1, server.getScheduler().getPendingTasks().size(), "the answer timeout");

        server.getMessenger().dispatchIncomingMessage(p, Protocol.CHANNEL,
                PacketCodec.encode(new TransferResultPacket(t.id(), TransferStatus.CONNECTING)));
        BeezyTransferResultEvent resultEvent = only(BeezyTransferResultEvent.class);
        assertEquals(TransferResult.Status.CONNECTING, resultEvent.getResult().status());
        assertEquals("lobby.example.net", resultEvent.getResult().address());
        assertTrue(plain(server.getConsoleSender().nextMessage()).contains("Jeb is connecting to lobby.example.net"));
        server.getScheduler().performTicks(1);
        assertTrue(server.getScheduler().getPendingTasks().isEmpty(), "timeout cancelled");

        // one a minute (wall clock, as the client counts)
        Object[] outcome = new Object[1];
        user.transfer("other.example.net").whenComplete((r, e) -> outcome[0] = e != null ? e : r);
        assertInstanceOf(dev.beezy.api.exception.RateLimitedException.class, outcome[0]);
        assertTrue(p.received.isEmpty());

        // without an answer the stage times out after 30 s of ticks
        RecordingPlayer q = join("Dinnerbone");
        BeezyUser other = hello(q, ALL);
        other.transfer("other.example.net").whenComplete((r, e) -> outcome[0] = e != null ? e : r);
        q.drain();
        server.getScheduler().performTicks(20 * 31);
        assertInstanceOf(dev.beezy.api.exception.TransferTimeoutException.class, outcome[0]);
    }

    @Test
    void foliaSchedulerRunsOnTheGlobalRegionScheduler() throws Exception {
        Scheduler folia = new Schedulers.Folia(plugin);
        AtomicInteger runs = new AtomicInteger();
        folia.schedule(Duration.ofMillis(100), runs::incrementAndGet);
        server.getScheduler().performTicks(1);
        assertEquals(0, runs.get(), "100 ms is 2 ticks");
        server.getScheduler().performTicks(2);
        assertEquals(1, runs.get());
        // MockBukkit does not implement ScheduledTask#cancel (UnimplementedOperationException), so
        // only check that our cancel reaches it without throwing.
        folia.schedule(Duration.ofMillis(100), runs::incrementAndGet).cancel();
    }
}
