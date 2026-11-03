package dev.beezy.core;

import dev.beezy.api.Feature;
import dev.beezy.api.SendResult;
import dev.beezy.api.display.Element;
import dev.beezy.api.display.Handle;
import dev.beezy.api.service.ElementService;
import dev.beezy.protocol.Packet;
import dev.beezy.protocol.PacketCodec;
import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.packet.EmptyPacket;
import dev.beezy.protocol.packet.EntityPacket;
import dev.beezy.protocol.packet.RemovePacket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Elements of one kind for one session: the current handle per id, the client's count limit,
 * re-sending. A {@code duration} is the client's business: it is written into the message and
 * nothing is scheduled; here expiry is only arithmetic (sent at + duration) checked lazily, and
 * expired entries are dropped on the next access.
 */
abstract class ElementServiceImpl<V extends Element, H extends Handle<V>> implements ElementService<V, H> {

    final Session session;
    final Feature feature;
    final String what;
    private final int limit;
    private final PacketKind removeKind;
    private final PacketKind clearKind;
    private final Map<String, HandleImpl<V>> current = new LinkedHashMap<String, HandleImpl<V>>();

    ElementServiceImpl(Session session, Feature feature, String what, int limit, PacketKind removeKind, PacketKind clearKind) {
        this.session = session;
        this.feature = feature;
        this.what = what;
        this.limit = limit;
        this.removeKind = removeKind;
        this.clearKind = clearKind;
    }

    /** The set message; {@code lifetimeMs} is -1 for the value's own duration, else the remaining time. */
    abstract Packet setPacket(V value, long lifetimeMs);

    abstract Optional<Duration> lifetime(V value);

    abstract HandleImpl<V> newHandle(V value);

    /** The message that re-sends an active handle with {@code remainingMs} left (-1: no duration). */
    Packet resendPacket(HandleImpl<V> h, long remainingMs) {
        return setPacket(h.value, remainingMs);
    }

    @SuppressWarnings("unchecked")
    private H cast(HandleImpl<V> h) {
        return (H) h;
    }

    @Override
    public final H show(V value) {
        if (value == null) {
            throw new IllegalArgumentException(what + " must not be null");
        }
        HandleImpl<V> h = newHandle(value);
        showInto(h, value);
        return cast(h);
    }

    SendResult showInto(HandleImpl<V> h, V value) {
        byte[] payload = PacketCodec.encode(setPacket(value, -1)); // invalid values throw here
        synchronized (session) {
            Instant now = session.core.now();
            purgeExpired(now);
            HandleImpl<V> existing = current.get(h.id);
            SendResult r = session.check(feature);
            if (r.isSent() && existing == null && current.size() >= limit) {
                r = SendResult.LIMIT_REACHED;
            }
            h.result = r;
            if (!r.isSent()) {
                if (!h.activeNow(now)) {
                    h.value = value;
                }
                return session.refuse(r, feature, what, null, limit);
            }
            session.send(payload);
            if (existing != null && existing != h) {
                existing.deactivate();
            }
            h.value = value;
            h.active = true;
            h.sentAt = now;
            current.put(h.id, h);
            Optional<Duration> life = lifetime(value);
            h.expiresAt = life.isPresent() ? now.plus(life.get()) : null;
            return SendResult.SENT;
        }
    }

    private void purgeExpired(Instant now) {
        for (Iterator<HandleImpl<V>> it = current.values().iterator(); it.hasNext(); ) {
            HandleImpl<V> h = it.next();
            if (!h.activeNow(now)) {
                h.deactivate();
                it.remove();
            }
        }
    }

    private Packet removePacket(String id) {
        if (removeKind == PacketKind.GLOW_REMOVE || removeKind == PacketKind.NAMETAG_RESET) {
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(what + " id must be a UUID: " + id);
            }
            return new EntityPacket(removeKind, uuid);
        }
        return new RemovePacket(removeKind, id);
    }

    @Override
    public final Optional<H> get(String id) {
        synchronized (session) {
            purgeExpired(session.core.now());
            HandleImpl<V> h = current.get(id);
            return h == null ? Optional.<H>empty() : Optional.of(cast(h));
        }
    }

    @Override
    public final Collection<H> active() {
        synchronized (session) {
            purgeExpired(session.core.now());
            List<H> out = new ArrayList<H>(current.size());
            for (HandleImpl<V> h : current.values()) {
                out.add(cast(h));
            }
            return out;
        }
    }

    @Override
    public final SendResult remove(String id) {
        byte[] payload = PacketCodec.encode(removePacket(id));
        synchronized (session) {
            SendResult r = session.check(feature);
            if (!r.isSent()) {
                return session.refuse(r, feature, what, null, 0);
            }
            session.send(payload);
            HandleImpl<V> h = current.remove(id);
            if (h != null) {
                h.deactivate();
            }
            return SendResult.SENT;
        }
    }

    SendResult removeHandle(HandleImpl<V> h) {
        synchronized (session) {
            if (current.get(h.id) != h || !h.activeNow(session.core.now())) {
                return SendResult.SENT; // nothing to remove
            }
            return remove(h.id);
        }
    }

    @Override
    public final SendResult clear() {
        byte[] payload = PacketCodec.encode(EmptyPacket.of(clearKind));
        synchronized (session) {
            SendResult r = session.check(feature);
            if (!r.isSent()) {
                return session.refuse(r, feature, what, null, 0);
            }
            session.send(payload);
            forget();
            return SendResult.SENT;
        }
    }

    @Override
    public final int limit() {
        return limit;
    }

    /** Forgets everything (after a Reset, clear or quit). Caller holds the lock. */
    void forget() {
        for (HandleImpl<V> h : current.values()) {
            h.deactivate();
        }
        current.clear();
    }

    /** Re-sends what is still active, with its remaining time. Caller holds the lock. */
    void resend() {
        Instant now = session.core.now();
        purgeExpired(now);
        if (current.isEmpty() || !session.check(feature).isSent()) {
            return;
        }
        for (HandleImpl<V> h : current.values()) {
            long remaining = h.expiresAt == null ? -1 : Math.max(1, h.expiresAt.toEpochMilli() - now.toEpochMilli());
            session.send(PacketCodec.encode(resendPacket(h, remaining)));
        }
    }
}
