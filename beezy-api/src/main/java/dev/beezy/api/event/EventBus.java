package dev.beezy.api.event;

import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.function.Consumer;

/**
 * Delivers Beezy events to handlers, in subscription order. A handler that throws is logged
 * and does not stop the others.
 *
 * <pre>{@code
 * Subscription s = beezy.events().subscribe(UserHelloEvent.class, e -> e.user().notifications().send("Hi", "Welcome"));
 * }</pre>
 */
@Stable
@NonExtendable
public interface EventBus {

    /**
     * @param type the event type (subtypes are delivered too)
     * @param handler the handler
     * @param <E> the type
     * @return the subscription
     */
    <E extends BeezyEvent> Subscription subscribe(Class<E> type, Consumer<? super E> handler);
}
