package dev.beezy.api.display;

import dev.beezy.api.value.BlockPosition;
import dev.beezy.api.value.Color;
import dev.beezy.api.value.WorldKey;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.time.Duration;
import java.util.Optional;

/** A beacon-like beam from a block up to the sky. 32 per player. */
@Stable
public final class Beam implements Element {

    private final String id;
    private final BlockPosition position;
    private final WorldKey world;
    private final Color color;
    private final Duration duration;

    private Beam(Builder b) {
        this.id = Lifetimes.id(b.id, "beam id");
        if (b.position == null) {
            throw new IllegalStateException("beam " + b.id + " has no position: call at(...)");
        }
        this.position = b.position;
        this.world = b.world;
        this.color = b.color;
        this.duration = Lifetimes.check(b.duration, "beam duration");
    }

    /**
     * @param id 1-32 bytes
     * @return a builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** @return a builder with these values */
    public Builder toBuilder() {
        return new Builder(id).at(position).world(world).color(color).duration(duration);
    }

    @Override
    public String id() {
        return id;
    }

    /** @return the block */
    public BlockPosition position() {
        return position;
    }

    /** @return the world, empty for any */
    public Optional<WorldKey> world() {
        return Optional.ofNullable(world);
    }

    /** @return the colour, empty for default */
    public Optional<Color> color() {
        return Optional.ofNullable(color);
    }

    /** @return the lifetime, empty until removed */
    public Optional<Duration> duration() {
        return Optional.ofNullable(duration);
    }

    /** Builds a {@link Beam}. */
    public static final class Builder {
        private final String id;
        private BlockPosition position;
        private WorldKey world;
        private Color color;
        private Duration duration;

        private Builder(String id) {
            this.id = id;
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
         * @param world null for any
         * @return this
         */
        public Builder world(WorldKey world) {
            this.world = world;
            return this;
        }

        /**
         * @param color null for default
         * @return this
         */
        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        /**
         * @param duration at most 24 h, null until removed
         * @return this
         */
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /** @return the beam */
        public Beam build() {
            return new Beam(this);
        }
    }
}
