package dev.beezy.server.api;

import java.util.List;
import java.util.UUID;

/**
 * One Beezy player, as a shortcut over {@link BeezyServerApi}. Holds no state of its own: if the
 * player leaves, sends simply return false.
 */
public final class BeezyPlayer {

    private final BeezyServerApi api;
    private final UUID uuid;

    public BeezyPlayer(BeezyServerApi api, UUID uuid) {
        this.api = Check.notNull(api, "api");
        this.uuid = Check.notNull(uuid, "uuid");
    }

    public UUID uuid() {
        return uuid;
    }

    /** The Hello, or null if the player left. */
    public Hello hello() {
        return api.helloOf(uuid);
    }

    /** True if the player is still a Beezy player here and has {@code feature} switched on. */
    public boolean has(Feature feature) {
        Hello h = hello();
        return h != null && h.has(feature);
    }

    public boolean sendWaypoint(Waypoint waypoint) {
        return api.sendWaypoint(uuid, waypoint);
    }

    public boolean removeWaypoint(String id) {
        return api.removeWaypoint(uuid, id);
    }

    public boolean clearWaypoints() {
        return api.clearWaypoints(uuid);
    }

    public boolean sendNotification(Notification notification) {
        return api.sendNotification(uuid, notification);
    }

    public boolean sendCooldown(Cooldown cooldown) {
        return api.sendCooldown(uuid, cooldown);
    }

    public boolean removeCooldown(String id) {
        return api.removeCooldown(uuid, id);
    }

    public boolean clearCooldowns() {
        return api.clearCooldowns(uuid);
    }

    public boolean sendTimer(Timer timer) {
        return api.sendTimer(uuid, timer);
    }

    public boolean removeTimer(String id) {
        return api.removeTimer(uuid, id);
    }

    public boolean clearTimers() {
        return api.clearTimers(uuid);
    }

    public boolean sendHeightLimit(String dimension, int minY, int maxY) {
        return api.sendHeightLimit(uuid, dimension, minY, maxY);
    }

    public boolean clearHeightLimit() {
        return api.clearHeightLimit(uuid);
    }

    public boolean sendTeam(List<TeamMember> members) {
        return api.sendTeam(uuid, members);
    }

    public boolean clearTeam() {
        return api.clearTeam(uuid);
    }

    public boolean sendRichPresence(RichPresence presence) {
        return api.sendRichPresence(uuid, presence);
    }

    public boolean clearRichPresence() {
        return api.clearRichPresence(uuid);
    }

    public boolean reset() {
        return api.reset(uuid);
    }

    @Override
    public String toString() {
        return "BeezyPlayer{" + uuid + "}";
    }
}
