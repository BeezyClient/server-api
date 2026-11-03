package dev.beezy.server.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
        hello(0x7F);
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
        hello(0x7F);
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
        api.reset(id);
        int[] kinds = {10, 11, 12, 20, 30, 31, 32, 40, 41, 42, 50, 51, 60, 61, 70, 71, 2};
        assertEquals(kinds.length, sent.size());
        for (int i = 0; i < kinds.length; i++) {
            assertEquals(1, sent.get(i)[0]);
            assertEquals(kinds[i], sent.get(i)[1] & 0xFF);
        }
    }
}
