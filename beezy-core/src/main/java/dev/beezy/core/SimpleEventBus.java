package dev.beezy.core;

import dev.beezy.api.event.BeezyEvent;
import dev.beezy.api.event.EventBus;
import dev.beezy.api.event.Subscription;
import dev.beezy.protocol.Validate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Synchronous, in subscription order; a throwing handler is logged and skipped. */
final class SimpleEventBus implements EventBus {

    private final Logger logger;
    private final List<Registration<?>> registrations = new CopyOnWriteArrayList<Registration<?>>();

    SimpleEventBus(Logger logger) {
        this.logger = logger;
    }

    @Override
    public <E extends BeezyEvent> Subscription subscribe(Class<E> type, Consumer<? super E> handler) {
        Registration<E> r = new Registration<E>(Validate.notNull(type, "type"), Validate.notNull(handler, "handler"));
        registrations.add(r);
        return r;
    }

    void post(BeezyEvent event) {
        for (Registration<?> r : registrations) {
            try {
                r.deliver(event);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "a Beezy event handler failed on " + event.getClass().getSimpleName(), e);
            }
        }
    }

    void clear() {
        registrations.clear();
    }

    private final class Registration<E extends BeezyEvent> implements Subscription {
        private final Class<E> type;
        private final Consumer<? super E> handler;

        Registration(Class<E> type, Consumer<? super E> handler) {
            this.type = type;
            this.handler = handler;
        }

        void deliver(BeezyEvent event) {
            if (type.isInstance(event)) {
                handler.accept(type.cast(event));
            }
        }

        @Override
        public void unsubscribe() {
            registrations.remove(this);
        }

        @Override
        public boolean isActive() {
            return registrations.contains(this);
        }
    }
}
