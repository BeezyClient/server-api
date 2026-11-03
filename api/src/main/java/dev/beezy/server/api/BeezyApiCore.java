package dev.beezy.server.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The platform-neutral {@link BeezyServerApi}: remembers each player's Hello and sends payloads
 * through a {@link Transport}. A platform plugin feeds it incoming messages
 * ({@link #handleIncoming}) and quits ({@link #forget}).
 */
public class BeezyApiCore implements BeezyServerApi {

    private final Transport transport;
    private final Map<UUID, Hello> hellos = new ConcurrentHashMap<UUID, Hello>();

    public BeezyApiCore(Transport transport) {
        this.transport = Check.notNull(transport, "transport");
    }

    /**
     * Handles a payload the player sent on {@code beezy:server}. Returns the Hello (and remembers
     * the player as a Beezy player) or null if the payload is not a valid Hello.
     */
    public Hello handleIncoming(UUID player, byte[] payload) {
        Hello hello = BeezyCodec.decodeHello(payload);
        if (hello != null) {
            remember(player, hello);
        }
        return hello;
    }

    /** Records a Hello for the player (replacing an earlier one). */
    public void remember(UUID player, Hello hello) {
        hellos.put(Check.notNull(player, "player"), Check.notNull(hello, "hello"));
    }

    /** Forgets the player, e.g. when they quit. */
    public void forget(UUID player) {
        hellos.remove(player);
    }

    /** Forgets everyone, e.g. when the plugin is disabled. */
    public void forgetAll() {
        hellos.clear();
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
    public boolean sendHeightLimit(UUID player, String dimension, int minY, int maxY) {
        return send(player, Feature.HEIGHT_LIMIT, BeezyCodec.heightLimit(dimension, minY, maxY));
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
    public boolean reset(UUID player) {
        return send(player, null, BeezyCodec.reset());
    }

    /** Encodes first (so bad input always throws), then sends only if the player wants it. */
    private boolean send(UUID player, Feature feature, byte[] payload) {
        Hello hello = helloOf(player);
        if (hello == null || (feature != null && !hello.has(feature))) {
            return false;
        }
        transport.send(player, payload);
        return true;
    }
}
