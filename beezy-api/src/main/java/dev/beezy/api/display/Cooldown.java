package dev.beezy.api.display;

import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.time.Duration;
import java.util.Optional;

/**
 * A cooldown bar that counts down from arrival and disappears at 0. Up to one hour; 16 per
 * player.
 */
@Stable
public final class Cooldown implements Element {

    private final String id;
    private final String label;
    private final String icon;
    private final Duration duration;

    private Cooldown(String id, String label, String icon, Duration duration) {
        this.id = Lifetimes.id(id, "cooldown id");
        this.label = Validate.str(label, Protocol.MAX_LABEL, "cooldown label");
        this.icon = icon == null ? "" : Validate.str(icon, Protocol.MAX_ITEM, "cooldown item icon");
        Validate.notNull(duration, "cooldown duration");
        if (duration.isNegative() || duration.isZero() || duration.toMillis() > Protocol.MAX_COOLDOWN_MS) {
            throw new IllegalArgumentException("cooldown duration is " + duration + ", must be 1 ms to 1 hour");
        }
        this.duration = duration;
    }

    /**
     * @param id 1-32 bytes
     * @param label at most 32 bytes
     * @param duration 1 ms to 1 h
     * @return the cooldown
     */
    public static Cooldown of(String id, String label, Duration duration) {
        return new Cooldown(id, label, "", duration);
    }

    /**
     * @param itemId an item id such as {@code minecraft:ender_pearl} (at most 64 bytes), null for none
     * @return the same cooldown with that icon
     */
    public Cooldown withIcon(String itemId) {
        return new Cooldown(id, label, itemId, duration);
    }

    @Override
    public String id() {
        return id;
    }

    /** @return the label */
    public String label() {
        return label;
    }

    /** @return the item icon */
    public Optional<String> icon() {
        return icon.isEmpty() ? Optional.<String>empty() : Optional.of(icon);
    }

    /** @return the length */
    public Duration duration() {
        return duration;
    }
}
