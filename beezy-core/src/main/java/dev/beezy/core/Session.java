package dev.beezy.core;

import dev.beezy.api.BeezyUser;
import dev.beezy.api.ClientInfo;
import dev.beezy.api.DeliveryPolicy;
import dev.beezy.api.Feature;
import dev.beezy.api.SendResult;
import dev.beezy.api.exception.BeezyException;
import dev.beezy.api.exception.FeatureUnavailableException;
import dev.beezy.api.exception.LimitExceededException;
import dev.beezy.api.exception.RateLimitedException;
import dev.beezy.api.exception.TransferTimeoutException;
import dev.beezy.api.exception.UserDisconnectedException;
import dev.beezy.api.service.BeamService;
import dev.beezy.api.service.BorderService;
import dev.beezy.api.service.CooldownService;
import dev.beezy.api.service.FoodService;
import dev.beezy.api.service.GlowService;
import dev.beezy.api.service.HeightLimitService;
import dev.beezy.api.service.HologramService;
import dev.beezy.api.service.NametagService;
import dev.beezy.api.service.NickHiderService;
import dev.beezy.api.service.NotificationService;
import dev.beezy.api.service.RichPresenceService;
import dev.beezy.api.service.SettingsService;
import dev.beezy.api.service.TeamService;
import dev.beezy.api.service.TimerService;
import dev.beezy.api.service.TntService;
import dev.beezy.api.service.WaypointService;
import dev.beezy.api.transfer.TransferResult;
import dev.beezy.api.value.WorldKey;
import dev.beezy.core.spi.Scheduler;
import dev.beezy.protocol.Packet;
import dev.beezy.protocol.PacketCodec;
import dev.beezy.protocol.packet.EmptyPacket;
import dev.beezy.protocol.packet.TransferPacket;
import dev.beezy.protocol.packet.TransferResultPacket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;

/**
 * One Beezy player's session. Every piece of state is guarded by {@code synchronized (this)};
 * payloads go to the platform inside that lock, so the order of sends is the order of calls.
 */
final class Session implements BeezyUser {

    /** Unanswered transfers kept per player; older ones fail with a timeout. */
    private static final int MAX_PENDING_TRANSFERS = 16;

    final BeezyCore core;
    private final UUID id;
    private volatile ClientInfo client;
    private volatile boolean connected = true;

    private final ElementServices.Waypoints waypoints = new ElementServices.Waypoints(this);
    private final ElementServices.Beams beams = new ElementServices.Beams(this);
    private final ElementServices.Borders borders = new ElementServices.Borders(this);
    private final ElementServices.Holograms holograms = new ElementServices.Holograms(this);
    private final ElementServices.Glows glow = new ElementServices.Glows(this);
    private final ElementServices.Nametags nametags = new ElementServices.Nametags(this);
    private final ElementServices.Cooldowns cooldowns = new ElementServices.Cooldowns(this);
    private final ElementServices.Timers timers = new ElementServices.Timers(this);
    private final List<ElementServiceImpl<?, ?>> elements = Arrays.<ElementServiceImpl<?, ?>>asList(
            waypoints, beams, borders, holograms, glow, nametags, cooldowns, timers);

    private final ValueServices.Notifications notifications;
    private final ValueServices.Team team = new ValueServices.Team(this);
    private final ValueServices.Presence presence = new ValueServices.Presence(this);
    private final ValueServices.Settings settings = new ValueServices.Settings(this);
    private final ValueServices.Nick nick = new ValueServices.Nick(this);
    private final ValueServices.Food food = new ValueServices.Food(this);
    private final ValueServices.Tnt tnt = new ValueServices.Tnt(this);
    private final ValueServices.Height height = new ValueServices.Height(this);
    private final List<ValueServices.Stateful> values;

    private final RateLimiter transferLimiter;
    private final Map<String, Pending> pending = new LinkedHashMap<String, Pending>();

    Session(BeezyCore core, UUID id, ClientInfo client) {
        this.core = core;
        this.id = id;
        this.client = client;
        this.notifications = new ValueServices.Notifications(this);
        this.values = Arrays.<ValueServices.Stateful>asList(notifications, team, presence, settings, nick, food, tnt, height);
        this.transferLimiter = new RateLimiter(core.clock(), core.config.transferBurst, core.config.transferWindow);
    }

    // ---- gating and policy -----------------------------------------------------------------------

    /** SENT when this session may receive a message of that feature (null: any). Caller holds the lock or does not care. */
    SendResult check(Feature feature) {
        if (!connected) {
            return SendResult.DISCONNECTED;
        }
        if (feature != null && !client.has(feature)) {
            return SendResult.FEATURE_UNAVAILABLE;
        }
        return SendResult.SENT;
    }

    /** Applies the delivery policy to a send that did not happen: logs it, then returns or throws. */
    SendResult refuse(SendResult result, Feature feature, String what, Duration retryAfter, int limit) {
        Level level = core.config.debug ? Level.INFO : Level.FINE;
        if (core.logger().isLoggable(level)) {
            core.logger().log(level, "[beezy] " + what + " for " + id + " not sent: " + result);
        }
        if (core.config.policy == DeliveryPolicy.THROW) {
            throw exception(result, feature, what, retryAfter, limit);
        }
        return result;
    }

    private BeezyException exception(SendResult result, Feature feature, String what, Duration retryAfter, int limit) {
        switch (result) {
            case FEATURE_UNAVAILABLE:
                return new FeatureUnavailableException(id, feature);
            case RATE_LIMITED:
                return new RateLimitedException(what, retryAfter == null ? Duration.ZERO : retryAfter);
            case LIMIT_REACHED:
                return new LimitExceededException(what, limit);
            default:
                return new UserDisconnectedException(id);
        }
    }

    /** Encodes (bad input throws), gates, sends, then runs {@code onSent} under the lock. */
    SendResult deliver(Feature feature, String what, Packet packet, Runnable onSent) {
        byte[] payload = PacketCodec.encode(packet);
        synchronized (this) {
            SendResult r = check(feature);
            if (!r.isSent()) {
                return refuse(r, feature, what, null, 0);
            }
            send(payload);
            onSent.run();
            return SendResult.SENT;
        }
    }

    void send(byte[] payload) {
        core.adapter().send(id, payload);
    }

    // ---- lifecycle ------------------------------------------------------------------------------

    void hello(ClientInfo info) {
        this.client = info;
    }

    /** The player left or the core shut down: everything is forgotten, pending transfers fail. */
    void disconnect() {
        List<Pending> failed;
        synchronized (this) {
            connected = false;
            forgetAll();
            failed = new ArrayList<Pending>(pending.values());
            pending.clear();
        }
        for (Pending p : failed) {
            p.fail(new UserDisconnectedException(id));
        }
    }

    private void forgetAll() {
        for (ElementServiceImpl<?, ?> e : elements) {
            e.forget();
        }
        for (ValueServices.Stateful v : values) {
            v.forget();
        }
    }

    @Override
    public SendResult reset() {
        byte[] payload = PacketCodec.encode(EmptyPacket.reset());
        synchronized (this) {
            SendResult r = check(null);
            if (!r.isSent()) {
                return refuse(r, null, "reset", null, 0);
            }
            send(payload);
            forgetAll();
            return SendResult.SENT;
        }
    }

    @Override
    public SendResult resync() {
        byte[] payload = PacketCodec.encode(EmptyPacket.reset());
        synchronized (this) {
            SendResult r = check(null);
            if (!r.isSent()) {
                return refuse(r, null, "resync", null, 0);
            }
            send(payload);
            for (ElementServiceImpl<?, ?> e : elements) {
                e.resend();
            }
            for (ValueServices.Stateful v : values) {
                v.resend();
            }
            return SendResult.SENT;
        }
    }

    // ---- transfers ------------------------------------------------------------------------------

    @Override
    public CompletionStage<TransferResult> transfer(String address) {
        String requestId = "t" + core.nextTransferId();
        byte[] payload = PacketCodec.encode(new TransferPacket(requestId, address)); // bad address throws
        CompletableFuture<TransferResult> future = new CompletableFuture<TransferResult>();
        Pending evicted = null;
        synchronized (this) {
            SendResult r = check(Feature.TRANSFER);
            Duration wait = Duration.ZERO;
            if (r.isSent()) {
                wait = transferLimiter.tryAcquire();
                if (!wait.isZero()) {
                    r = SendResult.RATE_LIMITED;
                }
            }
            if (!r.isSent()) {
                try {
                    refuse(r, Feature.TRANSFER, "transfer", wait, 0);
                    future.completeExceptionally(exception(r, Feature.TRANSFER, "transfer", wait, 0));
                } catch (BeezyException e) {
                    future.completeExceptionally(e);
                }
                return future;
            }
            final Pending p = new Pending(requestId, address, future);
            pending.put(requestId, p);
            if (pending.size() > MAX_PENDING_TRANSFERS) {
                Iterator<Pending> oldest = pending.values().iterator();
                evicted = oldest.next();
                oldest.remove();
            }
            send(payload);
            p.timeout = core.scheduler().schedule(core.config.transferTimeout, () -> timeout(p));
        }
        if (evicted != null) {
            evicted.fail(new TransferTimeoutException(core.config.transferTimeout));
        }
        return future;
    }

    private void timeout(Pending p) {
        synchronized (this) {
            if (pending.get(p.id) != p) {
                return;
            }
            pending.remove(p.id);
        }
        p.fail(new TransferTimeoutException(core.config.transferTimeout));
    }

    /** @return the result, or null if this server did not ask for that transfer */
    TransferResult completeTransfer(TransferResultPacket packet) {
        Pending p;
        synchronized (this) {
            p = pending.remove(packet.id());
        }
        if (p == null) {
            return null;
        }
        TransferResult.Status status;
        switch (packet.status()) {
            case CONNECTING:
                status = TransferResult.Status.CONNECTING;
                break;
            case REFUSED:
                status = TransferResult.Status.REFUSED;
                break;
            default:
                status = TransferResult.Status.INVALID_ADDRESS;
        }
        TransferResult result = new TransferResult(p.id, p.address, status);
        if (p.timeout != null) {
            p.timeout.cancel();
        }
        p.future.complete(result);
        return result;
    }

    private static final class Pending {
        final String id;
        final String address;
        final CompletableFuture<TransferResult> future;
        Scheduler.Task timeout;

        Pending(String id, String address, CompletableFuture<TransferResult> future) {
            this.id = id;
            this.address = address;
            this.future = future;
        }

        void fail(Throwable t) {
            if (timeout != null) {
                timeout.cancel();
            }
            future.completeExceptionally(t);
        }
    }

    // ---- BeezyUser ------------------------------------------------------------------------------

    @Override
    public UUID uniqueId() {
        return id;
    }

    @Override
    public Optional<String> name() {
        return core.adapter().name(id);
    }

    @Override
    public ClientInfo client() {
        return client;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public Optional<WorldKey> world() {
        return core.adapter().world(id);
    }

    @Override
    public WaypointService waypoints() {
        return waypoints;
    }

    @Override
    public BeamService beams() {
        return beams;
    }

    @Override
    public BorderService borders() {
        return borders;
    }

    @Override
    public HologramService holograms() {
        return holograms;
    }

    @Override
    public GlowService glow() {
        return glow;
    }

    @Override
    public NametagService nametags() {
        return nametags;
    }

    @Override
    public NotificationService notifications() {
        return notifications;
    }

    @Override
    public CooldownService cooldowns() {
        return cooldowns;
    }

    @Override
    public TimerService timers() {
        return timers;
    }

    @Override
    public TeamService team() {
        return team;
    }

    @Override
    public RichPresenceService richPresence() {
        return presence;
    }

    @Override
    public SettingsService settings() {
        return settings;
    }

    @Override
    public NickHiderService nickHider() {
        return nick;
    }

    @Override
    public FoodService food() {
        return food;
    }

    @Override
    public TntService tnt() {
        return tnt;
    }

    @Override
    public HeightLimitService heightLimit() {
        return height;
    }

    @Override
    public String toString() {
        return "BeezyUser{" + id + ", " + client + (connected ? "" : ", disconnected") + "}";
    }
}
