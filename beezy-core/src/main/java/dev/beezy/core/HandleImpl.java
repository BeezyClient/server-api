package dev.beezy.core;

import dev.beezy.api.BeezyUser;
import dev.beezy.api.SendResult;
import dev.beezy.api.display.Element;
import dev.beezy.api.display.Handle;
import java.time.Instant;
import java.util.Optional;

/** Base of every handle. All mutable state is guarded by the session's lock. */
abstract class HandleImpl<V extends Element> implements Handle<V> {

    final ElementServiceImpl<V, ?> service;
    final String id;
    V value;
    SendResult result = SendResult.SENT;
    boolean active;
    Instant sentAt;
    Instant expiresAt;

    HandleImpl(ElementServiceImpl<V, ?> service, V value) {
        this.service = service;
        this.id = value.id();
        this.value = value;
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final BeezyUser user() {
        return service.session;
    }

    @Override
    public final V value() {
        synchronized (service.session) {
            return value;
        }
    }

    @Override
    public final boolean isActive() {
        synchronized (service.session) {
            return activeNow(service.session.core.now());
        }
    }

    boolean activeNow(Instant now) {
        return active && service.session.isConnected() && (expiresAt == null || now.isBefore(expiresAt));
    }

    @Override
    public final SendResult result() {
        synchronized (service.session) {
            return result;
        }
    }

    @Override
    public final Optional<Instant> expiresAt() {
        synchronized (service.session) {
            return Optional.ofNullable(expiresAt);
        }
    }

    @Override
    public final SendResult update(V newValue) {
        if (newValue == null || !newValue.id().equals(id)) {
            throw new IllegalArgumentException("a handle can only be updated with a value of the same id (" + id + ")");
        }
        return service.showInto(this, newValue);
    }

    @Override
    public final SendResult remove() {
        return service.removeHandle(this);
    }

    /** Stops tracking. Caller holds the lock. */
    void deactivate() {
        active = false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + id + ", active=" + isActive() + "}";
    }
}
