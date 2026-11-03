package dev.beezy.server.api;

import java.awt.Color;
import java.time.Duration;
import java.util.Collection;
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
 * Colours are {@link Color}s sent opaque; null means the client's default. Worlds are the keys the
 * client knows ({@code minecraft:overworld}); empty or null means any. All methods are
 * thread-safe.
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

    // ---- waypoints (10-12) ----

    boolean sendWaypoint(UUID player, Waypoint waypoint);

    boolean removeWaypoint(UUID player, String id);

    boolean clearWaypoints(UUID player);

    // ---- notifications (20) ----

    boolean sendNotification(UUID player, Notification notification);

    // ---- cooldowns (30-32) ----

    boolean sendCooldown(UUID player, Cooldown cooldown);

    boolean removeCooldown(UUID player, String id);

    boolean clearCooldowns(UUID player);

    // ---- timers (40-42) ----

    boolean sendTimer(UUID player, Timer timer);

    boolean removeTimer(UUID player, String id);

    boolean clearTimers(UUID player);

    // ---- height limit (50-51) ----

    /** @param world world key, e.g. {@code minecraft:overworld}; empty or null for the one the player is in */
    boolean sendHeightLimit(UUID player, String world, int minY, int maxY);

    boolean clearHeightLimit(UUID player);

    // ---- team (60-61) ----

    /** Replaces the player's whole team (at most 32 members). */
    boolean sendTeam(UUID player, List<TeamMember> members);

    boolean clearTeam(UUID player);

    // ---- rich presence (70-71) ----

    boolean sendRichPresence(UUID player, RichPresence presence);

    boolean clearRichPresence(UUID player);

    // ---- beams (80-82) ----

    boolean sendBeam(UUID player, Beam beam);

    boolean removeBeam(UUID player, String id);

    boolean clearBeams(UUID player);

    // ---- borders (90-92) ----

    boolean sendBorder(UUID player, Border border);

    boolean removeBorder(UUID player, String id);

    boolean clearBorders(UUID player);

    // ---- glow (100-102) ----

    /** Outlines that entity for this player only; {@code color} null for the client's default. */
    boolean sendGlow(UUID player, UUID entity, Color color);

    boolean removeGlow(UUID player, UUID entity);

    boolean clearGlow(UUID player);

    // ---- holograms (110-112) ----

    boolean sendHologram(UUID player, Hologram hologram);

    boolean removeHologram(UUID player, String id);

    boolean clearHolograms(UUID player);

    // ---- module settings (120-121) ----

    /** Overrides those settings while the player stays on this server. */
    boolean overrideSettings(UUID player, ModuleSettings settings);

    /** Gives those settings (1 to 64) back to the player. */
    boolean releaseSettings(UUID player, Collection<SettingKey> keys);

    /** Gives every setting this server overrode back to the player. */
    boolean releaseAllSettings(UUID player);

    // ---- nametags (130-132) ----

    /** Replaces the nametag above {@code nametag.player()} for this player only. */
    boolean sendNametag(UUID player, Nametag nametag);

    /** Back to the vanilla nametag above {@code target}. */
    boolean resetNametag(UUID player, UUID target);

    boolean clearNametags(UUID player);

    // ---- nick hider (140-141) ----

    /** The name Nick Hider shows for the player themselves (1 to 16 bytes), for servers with {@code /nick}. */
    boolean sendNick(UUID player, String nick);

    boolean clearNick(UUID player);

    // ---- food values (150-151) ----

    /** What the Saturation display shows for those items (1 to 64). */
    boolean sendFoodValues(UUID player, List<FoodValue> values);

    boolean clearFoodValues(UUID player);

    // ---- TNT countdown (160-161) ----

    /** The fuse length the TNT Timer counts from on this server, 1 to 1200 ticks (vanilla 80). */
    boolean sendTntCountdown(UUID player, int ticks);

    /** {@link #sendTntCountdown(UUID, int)} with the fuse rounded to whole ticks (50 ms). */
    boolean sendTntCountdown(UUID player, Duration fuse);

    boolean clearTntCountdown(UUID player);

    // ---- transfer (170, answered by kind 3) ----

    /**
     * Asks the client to leave this server and join {@code address} ({@code host} or
     * {@code host:port}). The client answers with a {@link TransferResult} carrying the returned
     * id (on Bukkit, a {@code BeezyTransferResultEvent}). Clients allow one transfer a minute.
     *
     * @return the request's id, or null if nothing was sent
     */
    String transfer(UUID player, String address);

    /** Like {@link #transfer(UUID, String)} with an id of your own (1 to 32 bytes). */
    boolean transfer(UUID player, String id, String address);

    // ---- reset (2) ----

    /** Clears everything this server sent to the player, overridden settings included. */
    boolean reset(UUID player);
}
