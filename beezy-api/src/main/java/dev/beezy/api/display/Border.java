package dev.beezy.api.display;

import dev.beezy.api.value.Area;
import dev.beezy.api.value.Color;
import dev.beezy.api.value.WorldKey;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.time.Duration;
import java.util.Optional;

/**
 * A wall around a rectangle, from the bottom to the top of the world. 16 per player.
 * {@code solid} stops the player on the client only: the server must still enforce it.
 */
@Stable
public final class Border implements Element {

    private final String id;
    private final WorldKey world;
    private final Area area;
    private final Color color;
    private final boolean solid;
    private final boolean onlyWhenNear;
    private final Duration duration;

    private Border(Builder b) {
        this.id = Lifetimes.id(b.id, "border id");
        if (b.area == null) {
            throw new IllegalStateException("border " + b.id + " has no area: call area(...)");
        }
        this.world = b.world;
        this.area = b.area;
        this.color = b.color;
        this.solid = b.solid;
        this.onlyWhenNear = b.onlyWhenNear;
        this.duration = Lifetimes.check(b.duration, "border duration");
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
        return new Builder(id).world(world).area(area).color(color).solid(solid).onlyWhenNear(onlyWhenNear).duration(duration);
    }

    @Override
    public String id() {
        return id;
    }

    /** @return the world, empty for any */
    public Optional<WorldKey> world() {
        return Optional.ofNullable(world);
    }

    /** @return the area */
    public Area area() {
        return area;
    }

    /** @return the colour, empty for default */
    public Optional<Color> color() {
        return Optional.ofNullable(color);
    }

    /** @return whether the client blocks walking through */
    public boolean solid() {
        return solid;
    }

    /** @return whether it is only drawn within 16 blocks */
    public boolean onlyWhenNear() {
        return onlyWhenNear;
    }

    /** @return the lifetime, empty until removed */
    public Optional<Duration> duration() {
        return Optional.ofNullable(duration);
    }

    /** Builds a {@link Border}. */
    public static final class Builder {
        private final String id;
        private WorldKey world;
        private Area area;
        private Color color;
        private boolean solid;
        private boolean onlyWhenNear;
        private Duration duration;

        private Builder(String id) {
            this.id = id;
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
         * @param area the rectangle
         * @return this
         */
        public Builder area(Area area) {
            this.area = Validate.notNull(area, "area");
            return this;
        }

        /**
         * @param x1 x
         * @param z1 z
         * @param x2 other x
         * @param z2 other z
         * @return this
         */
        public Builder area(double x1, double z1, double x2, double z2) {
            return area(Area.of(x1, z1, x2, z2));
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
         * @param solid whether the client blocks walking through
         * @return this
         */
        public Builder solid(boolean solid) {
            this.solid = solid;
            return this;
        }

        /**
         * @param onlyWhenNear only drawn within 16 blocks
         * @return this
         */
        public Builder onlyWhenNear(boolean onlyWhenNear) {
            this.onlyWhenNear = onlyWhenNear;
            return this;
        }

        /**
         * @param duration at most 24 h
         * @return this
         */
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /** @return the border */
        public Border build() {
            return new Border(this);
        }
    }
}
