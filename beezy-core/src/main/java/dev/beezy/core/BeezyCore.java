package dev.beezy.core;

import dev.beezy.api.BeezyAudience;
import dev.beezy.api.BeezyClientApi;
import dev.beezy.api.BeezyUser;
import dev.beezy.api.ClientInfo;
import dev.beezy.api.DeliveryPolicy;
import dev.beezy.api.event.EventBus;
import dev.beezy.api.event.TransferResultEvent;
import dev.beezy.api.event.UserDisconnectEvent;
import dev.beezy.api.event.UserHelloEvent;
import dev.beezy.api.transfer.TransferResult;
import dev.beezy.api.value.WorldKey;
import dev.beezy.core.spi.PlatformAdapter;
import dev.beezy.core.spi.Scheduler;
import dev.beezy.protocol.Direction;
import dev.beezy.protocol.MalformedPacketException;
import dev.beezy.protocol.Packet;
import dev.beezy.protocol.PacketCodec;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.packet.EmptyPacket;
import dev.beezy.protocol.packet.HelloPacket;
import dev.beezy.protocol.packet.TransferResultPacket;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;

/**
 * The {@link BeezyClientApi} implementation. A platform plugin creates one with its
 * {@link PlatformAdapter}, feeds it what clients send ({@link #handleIncoming}) and quits
 * ({@link #handleQuit}), and sends {@link #resetPayload()} to every joining player.
 */
public final class BeezyCore implements BeezyClientApi {

    final CoreConfig config;
    private final PlatformAdapter adapter;
    private final SimpleEventBus events;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<UUID, Session>();
    private final AtomicLong transferIds = new AtomicLong();

    /**
     * @param adapter the platform
     * @param config the settings
     */
    public BeezyCore(PlatformAdapter adapter, CoreConfig config) {
        this.adapter = Validate.notNull(adapter, "adapter");
        this.config = Validate.notNull(config, "config");
        this.events = new SimpleEventBus(adapter.logger());
    }

    /** @return the Reset message, sent to every joining player */
    public static byte[] resetPayload() {
        return PacketCodec.encode(EmptyPacket.reset());
    }

    /**
     * Handles a payload a player sent on {@code beezy:server}: a Hello makes them a Beezy user
     * (and fires {@link UserHelloEvent}); a Transfer result for a transfer this server asked for
     * completes it (and fires {@link TransferResultEvent}). Anything else is logged and dropped.
     *
     * @param player the sender
     * @param payload the bytes
     */
    public void handleIncoming(UUID player, byte[] payload) {
        Packet packet;
        try {
            packet = PacketCodec.decode(payload, Direction.SERVERBOUND);
        } catch (MalformedPacketException e) {
            debug("dropped a message from " + player + ": " + e.getMessage());
            return;
        }
        if (packet instanceof HelloPacket) {
            HelloPacket h = (HelloPacket) packet;
            ClientInfo info = new ClientInfo(h.minecraftVersion(), h.beezyVersion(), h.features());
            boolean first = false;
            Session s = sessions.get(player);
            if (s == null) {
                Session created = new Session(this, player, info);
                s = sessions.putIfAbsent(player, created);
                if (s == null) {
                    s = created;
                    first = true;
                }
            }
            if (!first) {
                s.hello(info);
            }
            debug(player + " says Hello: " + info + (first ? "" : " (again)"));
            events.post(new UserHelloEvent(s, first));
        } else if (packet instanceof TransferResultPacket) {
            Session s = sessions.get(player);
            TransferResult result = s == null ? null : s.completeTransfer((TransferResultPacket) packet);
            if (result == null) {
                debug("ignored an unrequested transfer result from " + player);
                return;
            }
            debug(player + " answers " + result);
            events.post(new TransferResultEvent(s, result));
        }
    }

    /** @param player a player who left; their session ends and handles turn inactive */
    public void handleQuit(UUID player) {
        Session s = sessions.remove(player);
        if (s != null) {
            s.disconnect();
            events.post(new UserDisconnectEvent(s));
        }
    }

    /** Ends every session (no events) and drops every subscriber, on platform shutdown. */
    public void shutdown() {
        for (UUID id : new ArrayList<UUID>(sessions.keySet())) {
            Session s = sessions.remove(id);
            if (s != null) {
                s.disconnect();
            }
        }
        events.clear();
    }

    // ---- BeezyClientApi -------------------------------------------------------------------------

    @Override
    public Optional<BeezyUser> user(UUID player) {
        return player == null ? Optional.<BeezyUser>empty() : Optional.<BeezyUser>ofNullable(sessions.get(player));
    }

    @Override
    public boolean isBeezy(UUID player) {
        return player != null && sessions.containsKey(player);
    }

    @Override
    public Collection<BeezyUser> users() {
        return Collections.unmodifiableList(new ArrayList<BeezyUser>(sessions.values()));
    }

    @Override
    public BeezyAudience all() {
        return this::users;
    }

    @Override
    public BeezyAudience audience(Collection<UUID> players) {
        final List<UUID> ids = new ArrayList<UUID>(Validate.notNull(players, "players"));
        return () -> {
            List<BeezyUser> out = new ArrayList<BeezyUser>(ids.size());
            for (UUID id : ids) {
                Session s = sessions.get(id);
                if (s != null) {
                    out.add(s);
                }
            }
            return out;
        };
    }

    @Override
    public BeezyAudience world(final WorldKey world) {
        Validate.notNull(world, "world");
        return all().filter(u -> world.equals(adapter.world(u.uniqueId()).orElse(null)));
    }

    @Override
    public EventBus events() {
        return events;
    }

    @Override
    public DeliveryPolicy deliveryPolicy() {
        return config.policy;
    }

    // ---- for the sessions -----------------------------------------------------------------------

    PlatformAdapter adapter() {
        return adapter;
    }

    Scheduler scheduler() {
        return adapter.scheduler();
    }

    Logger logger() {
        return adapter.logger();
    }

    Clock clock() {
        return config.clock;
    }

    Instant now() {
        return config.clock.instant();
    }

    Function<Component, String> serializer() {
        return config.componentSerializer;
    }

    long nextTransferId() {
        return transferIds.incrementAndGet();
    }

    private void debug(String message) {
        if (config.debug) {
            adapter.logger().info("[debug] " + message);
        }
    }
}
