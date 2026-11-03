package dev.beezy.api.display;

import dev.beezy.api.value.Position;
import dev.beezy.api.value.Texts;
import dev.beezy.api.value.WorldKey;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;

/**
 * Floating text lines facing the player, top first. 1-8 lines, each an Adventure component
 * (at most 4096 bytes as JSON, checked when sent); 64 holograms per player.
 *
 * <pre>{@code
 * Hologram.builder("shop").at(Position.of(10.5, 72, -3.5)).world(WorldKey.OVERWORLD)
 *         .line(Component.text("Shop", NamedTextColor.GOLD))
 *         .line("§7Right-click to trade")          // § codes become a component
 *         .duration(Duration.ofMinutes(10)).build();
 * }</pre>
 */
@Stable
public final class Hologram implements Element {

    private final String id;
    private final Position position;
    private final WorldKey world;
    private final List<Component> lines;
    private final Duration duration;

    private Hologram(Builder b) {
        this.id = Lifetimes.id(b.id, "hologram id");
        if (b.position == null) {
            throw new IllegalStateException("hologram " + b.id + " has no position: call at(...)");
        }
        this.position = b.position;
        this.world = b.world;
        Validate.range(b.lines.size(), 1, Protocol.MAX_HOLOGRAM_LINES, "hologram line count");
        this.lines = Collections.unmodifiableList(new ArrayList<Component>(b.lines));
        this.duration = Lifetimes.check(b.duration, "hologram duration");
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
        return new Builder(id).at(position).world(world).lines(lines).duration(duration);
    }

    /**
     * The same hologram with other lines.
     *
     * @param lines 1-8 components
     * @return a new hologram
     */
    public Hologram withLines(List<? extends ComponentLike> lines) {
        return toBuilder().clearLines().lines(lines).build();
    }

    @Override
    public String id() {
        return id;
    }

    /** @return the position */
    public Position position() {
        return position;
    }

    /** @return the world, empty for any */
    public Optional<WorldKey> world() {
        return Optional.ofNullable(world);
    }

    /** @return the lines, top first */
    public List<Component> lines() {
        return lines;
    }

    /** @return the lifetime, empty until removed */
    public Optional<Duration> duration() {
        return Optional.ofNullable(duration);
    }

    /** Builds a {@link Hologram}. */
    public static final class Builder {
        private final String id;
        private Position position;
        private WorldKey world;
        private final List<Component> lines = new ArrayList<Component>();
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
        public Builder at(double x, double y, double z) {
            return at(Position.of(x, y, z));
        }

        /**
         * @param position where
         * @return this
         */
        public Builder at(Position position) {
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
         * @param line a component
         * @return this
         */
        public Builder line(ComponentLike line) {
            lines.add(Validate.notNull(line, "hologram line").asComponent());
            return this;
        }

        /**
         * @param line a {@code §} string or plain text
         * @return this
         */
        public Builder line(String line) {
            lines.add(Texts.legacy(line));
            return this;
        }

        /**
         * @param lines components, appended
         * @return this
         */
        public Builder lines(ComponentLike... lines) {
            return lines(Arrays.asList(lines));
        }

        /**
         * @param lines components, appended
         * @return this
         */
        public Builder lines(List<? extends ComponentLike> lines) {
            this.lines.addAll(Texts.components(lines));
            return this;
        }

        /**
         * @param lines {@code §} strings or plain text, appended
         * @return this
         */
        public Builder lines(String... lines) {
            for (String l : lines) {
                line(l);
            }
            return this;
        }

        /** @return this, without lines */
        public Builder clearLines() {
            lines.clear();
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

        /** @return the hologram */
        public Hologram build() {
            return new Hologram(this);
        }
    }
}
