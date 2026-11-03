package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_ID;
import static dev.beezy.server.api.BeezyProtocol.MAX_WORLD;

import java.awt.Color;
import java.time.Duration;

/**
 * A wall from the bottom to the top of the world around a rectangle (kind 90). The same id
 * replaces; the client keeps at most 16. {@link Builder#solid(boolean) Solid} only stops the
 * player on the client: the server must still enforce it.
 *
 * <pre>{@code
 * Border arena = Border.builder("arena").world("minecraft:overworld")
 *         .area(-50, -50, 50, 50).color(Color.RED).solid(true).onlyWhenNear(true).build();
 * }</pre>
 */
public final class Border {

    private final String id;
    private final String world;
    private final double minX;
    private final double minZ;
    private final double maxX;
    private final double maxZ;
    private final Color color;
    private final boolean solid;
    private final boolean onlyWhenNear;
    private final long durationMs;

    private Border(Builder b) {
        this.id = Check.id(b.id, MAX_ID, "border id");
        this.world = Check.optStr(b.world, MAX_WORLD, "border world");
        this.minX = Check.finite(b.minX, "border min x");
        this.minZ = Check.finite(b.minZ, "border min z");
        this.maxX = Check.finite(b.maxX, "border max x");
        this.maxZ = Check.finite(b.maxZ, "border max z");
        if (minX > maxX || minZ > maxZ) {
            throw new IllegalArgumentException("border min (" + minX + ", " + minZ + ") is above max (" + maxX + ", " + maxZ + ")");
        }
        this.color = b.color;
        this.solid = b.solid;
        this.onlyWhenNear = b.onlyWhenNear;
        this.durationMs = Check.durationMs(b.duration, "border duration");
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    /** World key; empty means any world. */
    public String world() {
        return world;
    }

    public double minX() {
        return minX;
    }

    public double minZ() {
        return minZ;
    }

    public double maxX() {
        return maxX;
    }

    public double maxZ() {
        return maxZ;
    }

    /** Null means the client's default colour. */
    public Color color() {
        return color;
    }

    public boolean solid() {
        return solid;
    }

    public boolean onlyWhenNear() {
        return onlyWhenNear;
    }

    /** Milliseconds until the client removes it by itself; 0 means until removed. */
    public long durationMs() {
        return durationMs;
    }

    /** Wire flags: bit 0 solid, bit 1 only when near. */
    int flags() {
        return (solid ? 1 : 0) | (onlyWhenNear ? 2 : 0);
    }

    public static final class Builder {
        private final String id;
        private String world = "";
        private double minX;
        private double minZ;
        private double maxX;
        private double maxZ;
        private Color color;
        private boolean solid;
        private boolean onlyWhenNear;
        private Duration duration;

        private Builder(String id) {
            this.id = id;
        }

        /** World key such as {@code minecraft:overworld}; empty or null (the default) for any world. */
        public Builder world(String world) {
            this.world = world;
            return this;
        }

        /** The rectangle, any two opposite corners (they are sorted). */
        public Builder area(double x1, double z1, double x2, double z2) {
            this.minX = Math.min(x1, x2);
            this.minZ = Math.min(z1, z2);
            this.maxX = Math.max(x1, x2);
            this.maxZ = Math.max(z1, z2);
            return this;
        }

        /** Null (the default) lets the client pick. */
        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        /** The client stops the player walking through it. Off by default. */
        public Builder solid(boolean solid) {
            this.solid = solid;
            return this;
        }

        /** Only drawn within 16 blocks of the player. Off by default. */
        public Builder onlyWhenNear(boolean onlyWhenNear) {
            this.onlyWhenNear = onlyWhenNear;
            return this;
        }

        /** Removed by the client after this long (at most 24 h); null or zero keeps it until removed. */
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /** @throws IllegalArgumentException if a field is over its cap or not finite */
        public Border build() {
            return new Border(this);
        }
    }
}
