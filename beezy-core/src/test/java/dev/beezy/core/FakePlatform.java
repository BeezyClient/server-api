package dev.beezy.core;

import dev.beezy.api.value.WorldKey;
import dev.beezy.core.spi.PlatformAdapter;
import dev.beezy.core.spi.Scheduler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/** A platform with no server: records payloads, runs tasks when time is advanced by hand. */
final class FakePlatform implements PlatformAdapter, Scheduler {

    final Map<UUID, List<byte[]>> sent = new HashMap<UUID, List<byte[]>>();
    final Map<UUID, WorldKey> worlds = new HashMap<UUID, WorldKey>();
    final List<Scheduled> tasks = new ArrayList<Scheduled>();
    final MutableClock clock = new MutableClock();

    @Override
    public synchronized void send(UUID player, byte[] payload) {
        List<byte[]> l = sent.get(player);
        if (l == null) {
            sent.put(player, l = new ArrayList<byte[]>());
        }
        l.add(payload);
    }

    synchronized List<byte[]> drain(UUID player) {
        List<byte[]> l = sent.remove(player);
        return l == null ? new ArrayList<byte[]>() : l;
    }

    @Override
    public Scheduler scheduler() {
        return this;
    }

    @Override
    public Logger logger() {
        return Logger.getLogger("beezy-test");
    }

    @Override
    public Optional<String> name(UUID player) {
        return Optional.of("player-" + player.toString().substring(0, 4));
    }

    @Override
    public Optional<WorldKey> world(UUID player) {
        return Optional.ofNullable(worlds.get(player));
    }

    @Override
    public synchronized Task schedule(Duration delay, Runnable task) {
        final Scheduled s = new Scheduled(clock.instant().plus(delay), task);
        tasks.add(s);
        return () -> {
            synchronized (FakePlatform.this) {
                tasks.remove(s);
            }
        };
    }

    /** Moves the clock and runs what is due. */
    void advance(Duration d) {
        clock.now = clock.now.plus(d);
        List<Runnable> due = new ArrayList<Runnable>();
        synchronized (this) {
            for (Iterator<Scheduled> it = tasks.iterator(); it.hasNext(); ) {
                Scheduled s = it.next();
                if (!s.at.isAfter(clock.now)) {
                    due.add(s.task);
                    it.remove();
                }
            }
        }
        for (Runnable r : due) {
            r.run();
        }
    }

    static final class Scheduled {
        final Instant at;
        final Runnable task;

        Scheduled(Instant at, Runnable task) {
            this.at = at;
            this.task = task;
        }
    }

    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-03T12:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
