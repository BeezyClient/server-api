package dev.beezy.api.display;

import dev.beezy.api.BeezyUser;
import dev.beezy.api.SendResult;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.time.Instant;
import java.util.Optional;

/**
 * Something shown to one user, returned by the service that showed it. A handle stays valid
 * after it stops being active: {@link #update} shows it again.
 *
 * <p>A handle is active from a successful show until it is removed, replaced by another
 * element with the same id, cleared, reset, expires (its {@code duration}), or the user leaves.
 *
 * @param <V> the value type
 */
@Stable
@NonExtendable
public interface Handle<V extends Element> {

    /** @return the element's id */
    String id();

    /** @return the user it is shown to */
    BeezyUser user();

    /** @return the value last shown (or attempted) */
    V value();

    /** @return whether the client is showing it, as far as the server knows */
    boolean isActive();

    /** @return the outcome of the last show or update */
    SendResult result();

    /** @return when the client removes it by itself, if it has a duration */
    Optional<Instant> expiresAt();

    /**
     * Shows a new value in its place; its duration starts again.
     *
     * @param value a value with the same id
     * @return the outcome
     * @throws IllegalArgumentException if the id differs
     */
    SendResult update(V value);

    /**
     * Removes it from the client. Does nothing (returns {@link SendResult#SENT}) when it is no
     * longer active.
     *
     * @return the outcome
     */
    SendResult remove();
}
