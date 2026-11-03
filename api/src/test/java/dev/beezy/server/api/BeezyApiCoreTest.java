package dev.beezy.server.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BeezyApiCoreTest {

    private final List<byte[]> sent = new ArrayList<byte[]>();
    private final List<UUID> to = new ArrayList<UUID>();
    private final BeezyApiCore api = new BeezyApiCore((player, payload) -> {
        to.add(player);
        sent.add(payload);
    });
    private final UUID id = UUID.randomUUID();

    private void hello(int features) {
        assertNotNull(api.handleIncoming(id, BeezyCodec.hello(new Hello("1.21.11", "0.2", features))));
    }

    @Test
    void unknownPlayerGetsNothing() {
        assertFalse(api.isBeezy(id));
        assertNull(api.helloOf(id));
        assertNull(api.player(id));
        assertFalse(api.sendNotification(id, Notification.of("a", "b")));
        assertFalse(api.reset(id));
        assertTrue(sent.isEmpty());
    }

    @Test
    void helloMakesPlayerBeezy() {
        hello(0x1FFFF);
        assertTrue(api.isBeezy(id));
        assertEquals("0.2", api.helloOf(id).beezyVersion());
        assertEquals(Collections.singleton(id), api.beezyPlayers());
        BeezyPlayer p = api.player(id);
        assertTrue(p.sendNotification(Notification.of("Welcome", "Have fun!")));
        assertEquals(id, to.get(0));
        assertArrayEquals(BeezyCodec.notification(Notification.of("Welcome", "Have fun!")), sent.get(0));
        api.forget(id);
        assertFalse(api.isBeezy(id));
        assertFalse(p.reset());
    }

    @Test
    void malformedHelloIsIgnored() {
        assertNull(api.handleIncoming(id, new byte[] {1, 1, 9}));
        assertFalse(api.isBeezy(id));
    }

    @Test
    void featureOffIsNotSent() {
        hello(Feature.NOTIFICATIONS.mask());
        assertFalse(api.sendWaypoint(id, Waypoint.builder("w").build()));
        assertFalse(api.sendTeam(id, Collections.<TeamMember>emptyList()));
        assertTrue(api.sendNotification(id, Notification.of("a", "b")));
        assertTrue(api.reset(id)); // reset is not tied to a feature
        assertEquals(2, sent.size());
        assertArrayEquals(new byte[] {1, 2}, sent.get(1));
    }

    @Test
    void badInputThrowsEvenForNonBeezyPlayers() {
        assertThrows(IllegalArgumentException.class, () -> api.removeWaypoint(UUID.randomUUID(), null));
        assertThrows(IllegalArgumentException.class, () -> api.sendHeightLimit(UUID.randomUUID(), "", 5, 1));
    }

    @Test
    void everyMethodSendsItsKind() {
        hello(0x1FFFF);
        api.sendWaypoint(id, Waypoint.builder("w").build());
        api.removeWaypoint(id, "w");
        api.clearWaypoints(id);
        api.sendNotification(id, Notification.of("a", "b"));
        api.sendCooldown(id, Cooldown.of("c", "C", 1));
        api.removeCooldown(id, "c");
        api.clearCooldowns(id);
        api.sendTimer(id, Timer.countdown("t", "T", 1));
        api.removeTimer(id, "t");
        api.clearTimers(id);
        api.sendHeightLimit(id, "", 0, 256);
        api.clearHeightLimit(id);
        api.sendTeam(id, Collections.<TeamMember>emptyList());
        api.clearTeam(id);
        api.sendRichPresence(id, RichPresence.builder("g").build());
        api.clearRichPresence(id);
        UUID other = UUID.randomUUID();
        api.sendBeam(id, Beam.builder("b").build());
        api.removeBeam(id, "b");
        api.clearBeams(id);
        api.sendBorder(id, Border.builder("b").build());
        api.removeBorder(id, "b");
        api.clearBorders(id);
        api.sendGlow(id, other, Color.RED);
        api.removeGlow(id, other);
        api.clearGlow(id);
        api.sendHologram(id, Hologram.builder("h").line("x").build());
        api.removeHologram(id, "h");
        api.clearHolograms(id);
        api.overrideSettings(id, ModuleSettings.builder().set("zoom", true).build());
        api.releaseSettings(id, Arrays.asList(SettingKey.of("zoom")));
        api.releaseAllSettings(id);
        api.sendNametag(id, Nametag.of(other, "x"));
        api.resetNametag(id, other);
        api.clearNametags(id);
        api.sendNick(id, "Nick");
        api.clearNick(id);
        api.sendFoodValues(id, Arrays.asList(new FoodValue("minecraft:apple", 4, 2.4)));
        api.clearFoodValues(id);
        api.sendTntCountdown(id, 80);
        api.sendTntCountdown(id, Duration.ofSeconds(2));
        api.clearTntCountdown(id);
        api.transfer(id, "example.net");
        api.reset(id);
        int[] kinds = {10, 11, 12, 20, 30, 31, 32, 40, 41, 42, 50, 51, 60, 61, 70, 71,
                80, 81, 82, 90, 91, 92, 100, 101, 102, 110, 111, 112, 120, 121, 121, 130, 131, 132,
                140, 141, 150, 151, 160, 160, 161, 170, 2};
        assertEquals(kinds.length, sent.size());
        for (int i = 0; i < kinds.length; i++) {
            assertEquals(1, sent.get(i)[0]);
            assertEquals(kinds[i], sent.get(i)[1] & 0xFF);
        }
    }

    @Test
    void everyNewFeatureNeedsItsBit() {
        Feature[] features = {Feature.BEAMS, Feature.BORDERS, Feature.GLOW, Feature.HOLOGRAMS, Feature.MODULE_SETTINGS,
                Feature.NAMETAGS, Feature.NICK_HIDER, Feature.FOOD_VALUES, Feature.TNT_COUNTDOWN, Feature.TRANSFER};
        for (Feature only : features) {
            hello(only.mask());
            boolean[] got = {
                api.sendBeam(id, Beam.builder("b").build()),
                api.sendBorder(id, Border.builder("b").build()),
                api.sendGlow(id, id, null),
                api.sendHologram(id, Hologram.builder("h").line("x").build()),
                api.releaseAllSettings(id),
                api.clearNametags(id),
                api.clearNick(id),
                api.clearFoodValues(id),
                api.clearTntCountdown(id),
                api.transfer(id, "t", "example.net"),
            };
            for (int i = 0; i < features.length; i++) {
                assertEquals(features[i] == only, got[i], only + " -> " + features[i]);
            }
        }
        assertEquals(features.length, sent.size());
    }

    @Test
    void settingsEditorSendsOneMessage() {
        hello(Feature.MODULE_SETTINGS.mask());
        BeezyPlayer p = api.player(id);
        assertTrue(p.settings().set("zoom", "max_zoom", 4).set("freelook", false).send());
        assertArrayEquals(BeezyCodec.moduleSettings(ModuleSettings.builder().set("zoom", "max_zoom", 4).set("freelook", false).build()),
                sent.get(0));
        assertTrue(p.settings().release("zoom", "max_zoom"));
        assertTrue(p.settings().release("freelook"));
        assertTrue(p.settings().releaseAll());
        assertArrayEquals(new byte[] {1, 121, 0}, sent.get(3));
        assertThrows(IllegalArgumentException.class, () -> p.settings().send()); // nothing set
    }

    @Test
    void transferResultsAreMatchedToRequests() {
        hello(Feature.TRANSFER.mask());
        String tid = api.transfer(id, "lobby.example.net:25566");
        assertNotNull(tid);
        assertArrayEquals(BeezyCodec.transfer(tid, "lobby.example.net:25566"), sent.get(0));
        // a result nobody asked for is dropped
        assertNull(api.handleIncoming(id, BeezyCodec.transferResult(new TransferResult("other", TransferResult.Status.FAILED))));
        // connecting keeps the request (a failure may follow), then the final status completes it
        TransferResult connecting = (TransferResult) api.handleIncoming(id,
                BeezyCodec.transferResult(new TransferResult(tid, TransferResult.Status.CONNECTING)));
        assertEquals("lobby.example.net:25566", connecting.address());
        TransferResult failed = (TransferResult) api.handleIncoming(id,
                BeezyCodec.transferResult(new TransferResult(tid, TransferResult.Status.FAILED)));
        assertEquals(TransferResult.Status.FAILED, failed.status());
        assertEquals("lobby.example.net:25566", failed.address());
        assertNull(api.handleIncoming(id, BeezyCodec.transferResult(new TransferResult(tid, TransferResult.Status.FAILED))));
        // ids of one's own; results from a player without a Hello are ignored
        assertTrue(api.transfer(id, "mine", "a.example"));
        UUID stranger = UUID.randomUUID();
        assertNull(api.handleIncoming(stranger, BeezyCodec.transferResult(new TransferResult("mine", TransferResult.Status.REFUSED))));
        assertEquals(TransferResult.Status.REFUSED, ((TransferResult) api.handleIncoming(id,
                BeezyCodec.transferResult(new TransferResult("mine", TransferResult.Status.REFUSED)))).status());
        // forgotten on quit
        assertTrue(api.transfer(id, "late", "a.example"));
        api.forget(id);
        hello(Feature.TRANSFER.mask());
        assertNull(api.handleIncoming(id, BeezyCodec.transferResult(new TransferResult("late", TransferResult.Status.FAILED))));
    }

    @Test
    void transferNotSentWithoutTheFeature() {
        hello(Feature.NOTIFICATIONS.mask());
        assertNull(api.transfer(id, "example.net"));
        assertTrue(sent.isEmpty());
    }
}
