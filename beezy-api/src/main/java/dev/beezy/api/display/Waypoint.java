package dev.beezy.api.display;

import dev.beezy.api.value.BlockPosition;
import dev.beezy.api.value.Color;
import dev.beezy.api.value.WorldKey;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.time.Duration;
import java.util.Optional;

/**
 * A waypoint at a block. 64 per player; the same id replaces.
 *
 * <pre>{@code
 * Waypoint.builder("flag").name("Enemy flag").at(120, 70, -40).world(WorldKey.OVERWORLD)
 *         .color(Color.RED).beam(true).duration(Duration.ofMinutes(2)).build();
 * }</pre>
 */
@Stable
public final class Waypoint implements Element {

    private final String id;
    private final String name;
    private final BlockPosition position;
    private final WorldKey world;
    private final Color color;
    private final boolean beam;
    private final Duration duration;

    private Waypoint(Builder b) {
        this.id = Lifetimes.id(b.id, "waypoint id");
        this.name = Validate.str(b.name, Protocol.MAX_NAME, "waypoint name");
        if (b.position == null) {
            throw new IllegalStateException("waypoint " + b.id + " has no position: call at(...)");
        }
        this.position = b.position;
        this.world = b.world;
        this.color = b.color;
        this.beam = b.beam;
        this.duration = Lifetimes.check(b.duration, "waypoint duration");
    }

    /**
     * @param id 1-32 bytes
     * @return a builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** @return a builder with this waypoint's values */
    public Builder toBuilder() {
        Builder b = new Builder(id);
        b.name = name;
        b.position = position;
        b.world = world;
        b.color = color;
        b.beam = beam;
        b.duration = duration;
        return b;
    }

    @Override
    public String id() {
        return id;
    }

    /** @return the name, may be empty */
    public String name() {
        return name;
    }

    /** @return the block */
    public BlockPosition position() {
        return position;
    }

    /** @return the world, empty for any */
    public Optional<WorldKey> world() {
        return Optional.ofNullable(world);
    }

    /** @return the colour, empty for the client's default */
    public Optional<Color> color() {
        return Optional.ofNullable(color);
    }

    /** @return whether a beam is drawn */
    public boolean beam() {
        return beam;
    }

    /** @return how long it stays, empty until removed */
    public Optional<Duration> duration() {
        return Optional.ofNullable(duration);
    }

    @Override
    public String toString() {
        return "Waypoint{" + id + " " + position + "}";
    }

    /** Builds a {@link Waypoint}. */
    public static final class Builder {
        private final String id;
        private String name = "";
        private BlockPosition position;
        private WorldKey world;
        private Color color;
        private boolean beam;
        private Duration duration;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * @param name at most 32 bytes
         * @return this
         */
        public Builder name(String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        /**
         * @param x x
         * @param y y
         * @param z z
         * @return this
         */
        public Builder at(int x, int y, int z) {
            return at(BlockPosition.of(x, y, z));
        }

        /**
         * @param position the block
         * @return this
         */
        public Builder at(BlockPosition position) {
            this.position = Validate.notNull(position, "position");
            return this;
        }

        /**
         * @param world the world, null for any
         * @return this
         */
        public Builder world(WorldKey world) {
            this.world = world;
            return this;
        }

        /**
         * @param color the colour, null for the client's default
         * @return this
         */
        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        /**
         * @param beam whether to draw a beam
         * @return this
         */
        public Builder beam(boolean beam) {
            this.beam = beam;
            return this;
        }

        /**
         * @param duration at most 24 h; null or zero until removed
         * @return this
         */
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /**
         * @return the waypoint
         * @throws IllegalArgumentException on invalid values
         */
        public Waypoint build() {
            return new Waypoint(this);
        }
    }
}
