package dev.beezy.server.api;

import java.awt.Color;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The platform-neutral {@link BeezyServerApi}: remembers each player's Hello and sends payloads
 * through a {@link Transport}. A platform plugin feeds it incoming messages
 * ({@link #handleIncoming}) and quits ({@link #forget}).
 */
public class BeezyApiCore implements BeezyServerApi {

    /** Unanswered transfers remembered per player; older ones are forgotten past this. */
    private static final int MAX_PENDING_TRANSFERS = 16;

    private final Transport transport;
    private final Map<UUID, Hello> hellos = new ConcurrentHashMap<UUID, Hello>();
    /** Per player: transfer id -> address, for transfers this server asked for. */
    private final Map<UUID, Map<String, String>> pendingTransfers = new ConcurrentHashMap<UUID, Map<String, String>>();
    private final AtomicLong transferIds = new AtomicLong();

    public BeezyApiCore(Transport transport) {
        this.transport = Check.notNull(transport, "transport");
    }

    /**
     * Handles a payload the player sent on {@code beezy:server}. Returns:
     * <ul>
     *   <li>a {@link Hello}, after remembering the player as a Beezy player;</li>
     *   <li>a {@link TransferResult} (with {@link TransferResult#address()} set) answering a
     *       transfer this server asked that player for;</li>
     *   <li>null for anything else: malformed, unknown, or a result nobody asked for.</li>
     * </ul>
     */
    public ClientMessage handleIncoming(UUID player, byte[] payload) {
        ClientMessage message = BeezyCodec.decode(payload);
        if (message instanceof Hello) {
            remember(player, (Hello) message);
            return message;
        }
        if (message instanceof TransferResult && isBeezy(player)) {
            TransferResult result = (TransferResult) message;
            String address = completeTransfer(player, result);
            return address == null ? null : new TransferResult(result.id(), result.status(), address);
        }
        return null;
    }

    /** Records a Hello for the player (replacing an earlier one). */
    public void remember(UUID player, Hello hello) {
        hellos.put(Check.notNull(player, "player"), Check.notNull(hello, "hello"));
    }

    /** Forgets the player, e.g. when they quit. */
    public void forget(UUID player) {
        hellos.remove(player);
        pendingTransfers.remove(player);
    }

    /** Forgets everyone, e.g. when the plugin is disabled. */
    public void forgetAll() {
        hellos.clear();
        pendingTransfers.clear();
    }

    @Override
    public boolean isBeezy(UUID player) {
        return player != null && hellos.containsKey(player);
    }

    @Override
    public Hello helloOf(UUID player) {
        return player == null ? null : hellos.get(player);
    }

    @Override
    public BeezyPlayer player(UUID player) {
        return isBeezy(player) ? new BeezyPlayer(this, player) : null;
    }

    @Override
    public Set<UUID> beezyPlayers() {
        return Collections.unmodifiableSet(new HashSet<UUID>(hellos.keySet()));
    }

    @Override
    public boolean sendWaypoint(UUID player, Waypoint waypoint) {
        return send(player, Feature.WAYPOINTS, BeezyCodec.waypoint(waypoint));
    }

    @Override
    public boolean removeWaypoint(UUID player, String id) {
        return send(player, Feature.WAYPOINTS, BeezyCodec.waypointRemove(id));
    }

    @Override
    public boolean clearWaypoints(UUID player) {
        return send(player, Feature.WAYPOINTS, BeezyCodec.waypointsClear());
    }

    @Override
    public boolean sendNotification(UUID player, Notification notification) {
        return send(player, Feature.NOTIFICATIONS, BeezyCodec.notification(notification));
    }

    @Override
    public boolean sendCooldown(UUID player, Cooldown cooldown) {
        return send(player, Feature.COOLDOWNS, BeezyCodec.cooldown(cooldown));
    }

    @Override
    public boolean removeCooldown(UUID player, String id) {
        return send(player, Feature.COOLDOWNS, BeezyCodec.cooldownRemove(id));
    }

    @Override
    public boolean clearCooldowns(UUID player) {
        return send(player, Feature.COOLDOWNS, BeezyCodec.cooldownsClear());
    }

    @Override
    public boolean sendTimer(UUID player, Timer timer) {
        return send(player, Feature.TIMERS, BeezyCodec.timer(timer));
    }

    @Override
    public boolean removeTimer(UUID player, String id) {
        return send(player, Feature.TIMERS, BeezyCodec.timerRemove(id));
    }

    @Override
    public boolean clearTimers(UUID player) {
        return send(player, Feature.TIMERS, BeezyCodec.timersClear());
    }

    @Override
    public boolean sendHeightLimit(UUID player, String world, int minY, int maxY) {
        return send(player, Feature.HEIGHT_LIMIT, BeezyCodec.heightLimit(world, minY, maxY));
    }

    @Override
    public boolean clearHeightLimit(UUID player) {
        return send(player, Feature.HEIGHT_LIMIT, BeezyCodec.heightLimitClear());
    }

    @Override
    public boolean sendTeam(UUID player, List<TeamMember> members) {
        return send(player, Feature.TEAM, BeezyCodec.team(members));
    }

    @Override
    public boolean clearTeam(UUID player) {
        return send(player, Feature.TEAM, BeezyCodec.teamClear());
    }

    @Override
    public boolean sendRichPresence(UUID player, RichPresence presence) {
        return send(player, Feature.RICH_PRESENCE, BeezyCodec.richPresence(presence));
    }

    @Override
    public boolean clearRichPresence(UUID player) {
        return send(player, Feature.RICH_PRESENCE, BeezyCodec.richPresenceClear());
    }

    @Override
    public boolean sendBeam(UUID player, Beam beam) {
        return send(player, Feature.BEAMS, BeezyCodec.beam(beam));
    }

    @Override
    public boolean removeBeam(UUID player, String id) {
        return send(player, Feature.BEAMS, BeezyCodec.beamRemove(id));
    }

    @Override
    public boolean clearBeams(UUID player) {
        return send(player, Feature.BEAMS, BeezyCodec.beamsClear());
    }

    @Override
    public boolean sendBorder(UUID player, Border border) {
        return send(player, Feature.BORDERS, BeezyCodec.border(border));
    }

    @Override
    public boolean removeBorder(UUID player, String id) {
        return send(player, Feature.BORDERS, BeezyCodec.borderRemove(id));
    }

    @Override
    public boolean clearBorders(UUID player) {
        return send(player, Feature.BORDERS, BeezyCodec.bordersClear());
    }

    @Override
    public boolean sendGlow(UUID player, UUID entity, Color color) {
        return send(player, Feature.GLOW, BeezyCodec.glow(entity, color));
    }

    @Override
    public boolean removeGlow(UUID player, UUID entity) {
        return send(player, Feature.GLOW, BeezyCodec.glowRemove(entity));
    }

    @Override
    public boolean clearGlow(UUID player) {
        return send(player, Feature.GLOW, BeezyCodec.glowClear());
    }

    @Override
    public boolean sendHologram(UUID player, Hologram hologram) {
        return send(player, Feature.HOLOGRAMS, BeezyCodec.hologram(hologram));
    }

    @Override
    public boolean removeHologram(UUID player, String id) {
        return send(player, Feature.HOLOGRAMS, BeezyCodec.hologramRemove(id));
    }

    @Override
    public boolean clearHolograms(UUID player) {
        return send(player, Feature.HOLOGRAMS, BeezyCodec.hologramsClear());
    }

    @Override
    public boolean overrideSettings(UUID player, ModuleSettings settings) {
        return send(player, Feature.MODULE_SETTINGS, BeezyCodec.moduleSettings(settings));
    }

    @Override
    public boolean releaseSettings(UUID player, Collection<SettingKey> keys) {
        return send(player, Feature.MODULE_SETTINGS, BeezyCodec.moduleSettingsRelease(keys));
    }

    @Override
    public boolean releaseAllSettings(UUID player) {
        return send(player, Feature.MODULE_SETTINGS, BeezyCodec.moduleSettingsReleaseAll());
    }

    @Override
    public boolean sendNametag(UUID player, Nametag nametag) {
        return send(player, Feature.NAMETAGS, BeezyCodec.nametag(nametag));
    }

    @Override
    public boolean resetNametag(UUID player, UUID target) {
        return send(player, Feature.NAMETAGS, BeezyCodec.nametagReset(target));
    }

    @Override
    public boolean clearNametags(UUID player) {
        return send(player, Feature.NAMETAGS, BeezyCodec.nametagsClear());
    }

    @Override
    public boolean sendNick(UUID player, String nick) {
        return send(player, Feature.NICK_HIDER, BeezyCodec.nickHider(nick));
    }

    @Override
    public boolean clearNick(UUID player) {
        return send(player, Feature.NICK_HIDER, BeezyCodec.nickHiderClear());
    }

    @Override
    public boolean sendFoodValues(UUID player, List<FoodValue> values) {
        return send(player, Feature.FOOD_VALUES, BeezyCodec.foodValues(values));
    }

    @Override
    public boolean clearFoodValues(UUID player) {
        return send(player, Feature.FOOD_VALUES, BeezyCodec.foodValuesClear());
    }

    @Override
    public boolean sendTntCountdown(UUID player, int ticks) {
        return send(player, Feature.TNT_COUNTDOWN, BeezyCodec.tntCountdown(ticks));
    }

    @Override
    public boolean sendTntCountdown(UUID player, Duration fuse) {
        return sendTntCountdown(player, BeezyCodec.tntTicks(fuse));
    }

    @Override
    public boolean clearTntCountdown(UUID player) {
        return send(player, Feature.TNT_COUNTDOWN, BeezyCodec.tntCountdownClear());
    }

    @Override
    public String transfer(UUID player, String address) {
        String id = "transfer-" + transferIds.incrementAndGet();
        return transfer(player, id, address) ? id : null;
    }

    @Override
    public boolean transfer(UUID player, String id, String address) {
        byte[] payload = BeezyCodec.transfer(id, address);
        if (!wants(player, Feature.TRANSFER)) {
            return false;
        }
        // Remembered before sending, so an answer that comes back at once is matched.
        Map<String, String> pending = pendingTransfers.computeIfAbsent(player,
                k -> Collections.synchronizedMap(new LinkedHashMap<String, String>()));
        synchronized (pending) {
            pending.remove(id); // re-insert at the end
            pending.put(id, address);
            Iterator<String> oldest = pending.keySet().iterator();
            while (pending.size() > MAX_PENDING_TRANSFERS) {
                oldest.next();
                oldest.remove();
            }
        }
        transport.send(player, payload);
        return true;
    }

    /**
     * The address of the transfer this result answers, or null if this server did not ask for it.
     * A request stays pending while the client is only connecting (a failure may follow).
     */
    private String completeTransfer(UUID player, TransferResult result) {
        Map<String, String> pending = pendingTransfers.get(player);
        if (pending == null) {
            return null;
        }
        synchronized (pending) {
            return result.status() == TransferResult.Status.CONNECTING ? pending.get(result.id()) : pending.remove(result.id());
        }
    }

    @Override
    public boolean reset(UUID player) {
        return send(player, null, BeezyCodec.reset());
    }

    /** Encodes first (so bad input always throws), then sends only if the player wants it. */
    private boolean send(UUID player, Feature feature, byte[] payload) {
        if (!wants(player, feature)) {
            return false;
        }
        transport.send(player, payload);
        return true;
    }

    /** A Beezy player with {@code feature} on (any Beezy player for a null feature). */
    private boolean wants(UUID player, Feature feature) {
        Hello hello = helloOf(player);
        return hello != null && (feature == null || hello.has(feature));
    }
}
