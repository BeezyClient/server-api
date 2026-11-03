package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_ID;
import static dev.beezy.server.api.BeezyProtocol.MAX_NAME;
import static dev.beezy.server.api.BeezyProtocol.MAX_WORLD;

import java.awt.Color;
import java.time.Duration;

/**
 * A server waypoint (kind 10). Sending one with an id the player already has replaces it; the
 * client keeps at most 64 server waypoints.
 *
 * <pre>{@code
 * Waypoint spawn = Waypoint.builder("spawn").name("Spawn").at(0, 64, 0)
 *         .world("minecraft:overworld").color(Color.GREEN).beam(true)
 *         .duration(Duration.ofMinutes(5)).build();
 * }</pre>
 */
public final class Waypoint {

    private final String id;
    private final String name;
    private final int x;
    private final int y;
    private final int z;
    private final String world;
    private final Color color;
    private final boolean beam;
    private final long durationMs;

    private Waypoint(Builder b) {
        this.id = Check.id(b.id, MAX_ID, "waypoint id");
        this.name = Check.str(b.name == null ? b.id : b.name, MAX_NAME, "waypoint name");
        this.x = b.x;
        this.y = b.y;
        this.z = b.z;
        this.world = Check.optStr(b.world, MAX_WORLD, "waypoint world");
        this.color = b.color;
        this.beam = b.beam;
        this.durationMs = Check.durationMs(b.duration, "waypoint duration");
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
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

    /** World key such as {@code minecraft:overworld}; empty means any world. */
    public String world() {
        return world;
    }

    /** Null means the client's default colour. */
    public Color color() {
        return color;
    }

    public boolean beam() {
        return beam;
    }

    /** Milliseconds until the client removes it by itself; 0 means until removed. */
    public long durationMs() {
        return durationMs;
    }

    public static final class Builder {
        private final String id;
        private String name;
        private int x;
        private int y;
        private int z;
        private String world = "";
        private Color color;
        private boolean beam;
        private Duration duration;

        private Builder(String id) {
            this.id = id;
        }

        /** Shown name (≤ 32 bytes). Defaults to the id. */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** Block coordinates. */
        public Builder at(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        /**
         * The world key the client knows, e.g. {@code minecraft:the_nether}; empty or null for any
         * world (the default). On Bukkit, {@code BeezyBukkit.world(World)} gives the key.
         */
        public Builder world(String world) {
            this.world = world;
            return this;
        }

        /** Null (the default) lets the client pick. */
        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        /** Draw a beacon-like beam. Off by default. */
        public Builder beam(boolean beam) {
            this.beam = beam;
            return this;
        }

        /** The client removes it by itself after this long (at most 24 h); null or zero (the default) keeps it until removed. */
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /** @throws IllegalArgumentException if a field is over its cap or out of range */
        public Waypoint build() {
            return new Waypoint(this);
        }
    }
}
