package dev.beezy.api.event;

import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;

/** A registered handler. */
@Stable
@NonExtendable
public interface Subscription {

    /** Stops delivering events to the handler. */
    void unsubscribe();

    /** @return whether it still receives events */
    boolean isActive();
}
