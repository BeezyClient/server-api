package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.api.display.Element;
import dev.beezy.api.display.Handle;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.Collection;
import java.util.Optional;

/**
 * Elements of one kind shown to one user, keyed by id. Showing an id that is already shown
 * replaces it (and makes the old handle inactive).
 *
 * @param <V> the value type
 * @param <H> the handle type
 */
@Stable
@NonExtendable
public interface ElementService<V extends Element, H extends Handle<V>> {

    /**
     * Shows it, or replaces the element with the same id.
     *
     * @param value what to show
     * @return its handle; inactive if it was not sent (see {@link Handle#result()})
     * @throws IllegalArgumentException if the value does not fit the protocol
     * @throws dev.beezy.api.exception.BeezyException under {@link dev.beezy.api.DeliveryPolicy#THROW}
     */
    H show(V value);

    /**
     * @param id an id
     * @return the active handle with that id
     */
    Optional<H> get(String id);

    /** @return the active handles */
    Collection<H> active();

    /**
     * @param id an id
     * @return the outcome of removing it (sent even if the server did not show it)
     */
    SendResult remove(String id);

    /** @return the outcome of removing every element of this kind */
    SendResult clear();

    /** @return how many of this kind the client shows at once */
    int limit();
}
