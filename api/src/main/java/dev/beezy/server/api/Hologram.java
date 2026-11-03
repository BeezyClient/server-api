package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_HOLOGRAM_LINE;
import static dev.beezy.server.api.BeezyProtocol.MAX_HOLOGRAM_LINES;
import static dev.beezy.server.api.BeezyProtocol.MAX_ID;
import static dev.beezy.server.api.BeezyProtocol.MAX_WORLD;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Floating text lines facing the player (kind 110), top line first, {@code §} codes kept. The
 * same id replaces; the client keeps at most 64.
 *
 * <pre>{@code
 * Hologram h = Hologram.builder("shop").at(0.5, 66, 0.5).world("minecraft:overworld")
 *         .lines("§6Shop", "Right-click the villager").duration(Duration.ofMinutes(10)).build();
 * }</pre>
 */
public final class Hologram {

    private final String id;
    private final double x;
    private final double y;
    private final double z;
    private final String world;
    private final List<String> lines;
    private final long durationMs;

    private Hologram(Builder b) {
        this.id = Check.id(b.id, MAX_ID, "hologram id");
        this.x = Check.finite(b.x, "hologram x");
        this.y = Check.finite(b.y, "hologram y");
        this.z = Check.finite(b.z, "hologram z");
        this.world = Check.optStr(b.world, MAX_WORLD, "hologram world");
        Check.range(b.lines.size(), 1, MAX_HOLOGRAM_LINES, "hologram line count");
        List<String> copy = new ArrayList<String>(b.lines.size());
        for (String line : b.lines) {
            copy.add(Check.str(line, MAX_HOLOGRAM_LINE, "hologram line"));
        }
        this.lines = Collections.unmodifiableList(copy);
        this.durationMs = Check.durationMs(b.duration, "hologram duration");
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    /** World key; empty means any world. */
    public String world() {
        return world;
    }

    /** 1 to 8 lines, top first. */
    public List<String> lines() {
        return lines;
    }

    /** Milliseconds until the client removes it by itself; 0 means until removed. */
    public long durationMs() {
        return durationMs;
    }

    public static final class Builder {
        private final String id;
        private double x;
        private double y;
        private double z;
        private String world = "";
        private final List<String> lines = new ArrayList<String>();
        private Duration duration;

        private Builder(String id) {
            this.id = id;
        }

        /** Exact position. */
        public Builder at(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        /** World key such as {@code minecraft:overworld}; empty or null (the default) for any world. */
        public Builder world(String world) {
            this.world = world;
            return this;
        }

        /** Replaces the lines (1 to 8, each ≤ 128 bytes). */
        public Builder lines(String... lines) {
            return lines(Arrays.asList(Check.notNull(lines, "hologram lines")));
        }

        /** Replaces the lines (1 to 8, each ≤ 128 bytes). */
        public Builder lines(List<String> lines) {
            this.lines.clear();
            this.lines.addAll(Check.notNull(lines, "hologram lines"));
            return this;
        }

        /** Adds a line below the others. */
        public Builder line(String line) {
            this.lines.add(line);
            return this;
        }

        /** Removed by the client after this long (at most 24 h); null or zero keeps it until removed. */
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /** @throws IllegalArgumentException if a field is over its cap or out of range */
        public Hologram build() {
            return new Hologram(this);
        }
    }
}
