package dev.beezy.server.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What a server plugin uses to talk to Beezy players. On Bukkit/Paper get it from the services
 * manager: {@code Bukkit.getServicesManager().load(BeezyServerApi.class)}.
 *
 * <p>Every send method returns true when the message was handed to the player, false when the
 * player is not (or not yet) known as a Beezy player, or switched that feature off; in both cases
 * the client would not show it, so nothing is sent. Invalid input (a field over its cap, a value
 * out of range) throws {@link IllegalArgumentException} whether or not the player uses Beezy.
 * All methods are thread-safe.
 */
public interface BeezyServerApi {

    /** True once the player's client has sent its Hello on this server. */
    boolean isBeezy(UUID player);

    /** The player's Hello, or null if they are not a Beezy player. */
    Hello helloOf(UUID player);

    /** A per-player view, or null if they are not a Beezy player. */
    BeezyPlayer player(UUID player);

    /** Online players known to use Beezy. */
    Set<UUID> beezyPlayers();

    boolean sendWaypoint(UUID player, Waypoint waypoint);

    boolean removeWaypoint(UUID player, String id);

    boolean clearWaypoints(UUID player);

    boolean sendNotification(UUID player, Notification notification);

    boolean sendCooldown(UUID player, Cooldown cooldown);

    boolean removeCooldown(UUID player, String id);

    boolean clearCooldowns(UUID player);

    boolean sendTimer(UUID player, Timer timer);

    boolean removeTimer(UUID player, String id);

    boolean clearTimers(UUID player);

    /** @param dimension e.g. {@code minecraft:overworld}; empty or null for the player's current one */
    boolean sendHeightLimit(UUID player, String dimension, int minY, int maxY);

    boolean clearHeightLimit(UUID player);

    /** Replaces the player's whole team (at most 32 members). */
    boolean sendTeam(UUID player, List<TeamMember> members);

    boolean clearTeam(UUID player);

    boolean sendRichPresence(UUID player, RichPresence presence);

    boolean clearRichPresence(UUID player);

    /** Clears everything this server sent to the player. */
    boolean reset(UUID player);
}
