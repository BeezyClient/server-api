package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_ID;
import static dev.beezy.server.api.BeezyProtocol.MAX_WORLD;

import java.awt.Color;
import java.time.Duration;

/**
 * A beacon-like beam from a block up to the sky (kind 80), seen from any distance in render
 * range. The same id replaces; the client keeps at most 32.
 *
 * <pre>{@code
 * Beam drop = Beam.builder("drop").at(120, 64, -40).world("minecraft:overworld")
 *         .color(Color.YELLOW).duration(Duration.ofSeconds(30)).build();
 * }</pre>
 */
public final class Beam {

    private final String id;
    private final int x;
    private final int y;
    private final int z;
    private final String world;
    private final Color color;
    private final long durationMs;

    private Beam(Builder b) {
        this.id = Check.id(b.id, MAX_ID, "beam id");
        this.x = b.x;
        this.y = b.y;
        this.z = b.z;
        this.world = Check.optStr(b.world, MAX_WORLD, "beam world");
        this.color = b.color;
        this.durationMs = Check.durationMs(b.duration, "beam duration");
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    /** World key; empty means any world. */
    public String world() {
        return world;
    }

    /** Null means the client's default colour. */
    public Color color() {
        return color;
    }

    /** Milliseconds until the client removes it by itself; 0 means until removed. */
    public long durationMs() {
        return durationMs;
    }

    public static final class Builder {
        private final String id;
        private int x;
        private int y;
        private int z;
        private String world = "";
        private Color color;
        private Duration duration;

        private Builder(String id) {
            this.id = id;
        }

        /** Block coordinates of the beam's base. */
        public Builder at(int x, int y, int z) {
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

        /** Null (the default) lets the client pick. */
        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        /** Removed by the client after this long (at most 24 h); null or zero keeps it until removed. */
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /** @throws IllegalArgumentException if a field is over its cap or out of range */
        public Beam build() {
            return new Beam(this);
        }
    }
}
