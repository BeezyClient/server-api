package dev.beezy.api.display;

import dev.beezy.api.value.Color;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.util.Optional;
import java.util.UUID;

/** An outline around an entity, for one player only. Its id is the entity's UUID. 128 per player. */
@Stable
public final class Glow implements Element {

    private final UUID entity;
    private final Color color;

    private Glow(UUID entity, Color color) {
        this.entity = Validate.notNull(entity, "glow entity");
        this.color = color;
    }

    /**
     * @param entity the entity
     * @param color null for the client's default
     * @return the glow
     */
    public static Glow of(UUID entity, Color color) {
        return new Glow(entity, color);
    }

    @Override
    public String id() {
        return entity.toString();
    }

    /** @return the entity */
    public UUID entity() {
        return entity;
    }

    /** @return the colour, empty for default */
    public Optional<Color> color() {
        return Optional.ofNullable(color);
    }
}
