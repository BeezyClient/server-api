package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.api.display.Glow;
import dev.beezy.api.display.GlowHandle;
import dev.beezy.api.value.Color;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.Optional;
import java.util.UUID;

/** Outlines entities for this user only: 128 at once, keyed by entity UUID. */
@Stable
@NonExtendable
public interface GlowService extends ElementService<Glow, GlowHandle> {

    /**
     * @param entity the entity
     * @param color null for the client's default
     * @return the handle
     */
    default GlowHandle glow(UUID entity, Color color) {
        return show(Glow.of(entity, color));
    }

    /**
     * @param entity the entity
     * @return the handle, in the client's default colour
     */
    default GlowHandle glow(UUID entity) {
        return show(Glow.of(entity, null));
    }

    /**
     * @param entity the entity
     * @return the active handle
     */
    default Optional<GlowHandle> get(UUID entity) {
        return get(entity.toString());
    }

    /**
     * @param entity the entity
     * @return the outcome
     */
    default SendResult remove(UUID entity) {
        return remove(entity.toString());
    }
}
