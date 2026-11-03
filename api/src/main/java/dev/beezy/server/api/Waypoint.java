package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_DIMENSION;
import static dev.beezy.server.api.BeezyProtocol.MAX_ID;
import static dev.beezy.server.api.BeezyProtocol.MAX_NAME;

/**
 * A server waypoint (kind 10). Sending one with an id the player already has replaces it; the
 * client keeps at most 64 server waypoints.
 *
 * <pre>{@code
 * Waypoint spawn = Waypoint.builder("spawn").name("Spawn").at(0, 64, 0)
 *         .dimension("minecraft:overworld").color(0xFF55FF55).beam(true).build();
 * }</pre>
 */
public final class Waypoint {

    private final String id;
    private final String name;
    private final int x;
    private final int y;
    private final int z;
    private final String dimension;
    private final int color;
    private final boolean beam;

    private Waypoint(Builder b) {
        this.id = Check.id(b.id, MAX_ID, "waypoint id");
        this.name = Check.str(b.name == null ? b.id : b.name, MAX_NAME, "waypoint name");
        this.x = b.x;
        this.y = b.y;
        this.z = b.z;
        this.dimension = Check.optStr(b.dimension, MAX_DIMENSION, "waypoint dimension");
        this.color = b.color;
        this.beam = b.beam;
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

    /** Dimension id such as {@code minecraft:overworld}; empty means any dimension. */
    public String dimension() {
        return dimension;
    }

    /** ARGB; alpha 0 means the client's default color. */
    public int color() {
        return color;
    }

    public boolean beam() {
        return beam;
    }

    public static final class Builder {
        private final String id;
        private String name;
        private int x;
        private int y;
        private int z;
        private String dimension = "";
        private int color;
        private boolean beam;

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

        /** E.g. {@code minecraft:the_nether}; empty or null for any dimension (the default). */
        public Builder dimension(String dimension) {
            this.dimension = dimension;
            return this;
        }

        /** ARGB, e.g. {@code 0xFFFF5555}; 0 (the default) lets the client pick. */
        public Builder color(int argb) {
            this.color = argb;
            return this;
        }

        /** Draw a beacon-like beam. Off by default. */
        public Builder beam(boolean beam) {
            this.beam = beam;
            return this;
        }

        /** @throws IllegalArgumentException if a field is over its cap */
        public Waypoint build() {
            return new Waypoint(this);
        }
    }
}
