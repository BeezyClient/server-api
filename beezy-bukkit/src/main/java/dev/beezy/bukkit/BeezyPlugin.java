package dev.beezy.bukkit;

import dev.beezy.api.BeezyClientApi;
import dev.beezy.api.BeezyProvider;
import dev.beezy.api.event.TransferResultEvent;
import dev.beezy.api.event.UserHelloEvent;
import dev.beezy.core.BeezyCore;
import dev.beezy.core.CoreConfig;
import dev.beezy.protocol.Protocol;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

/**
 * The Beezy plugin. Registering {@code beezy:server} as an incoming channel makes the server list
 * it in the {@code REGISTER} it sends every player on join; a Beezy client answers with its Hello.
 * On join the plugin also sends a Reset, so nothing another backend of the same network sent
 * stays on the player's screen. It sends nothing else on its own except the optional default
 * presence and team sync from config.yml, and never contacts anything.
 *
 * <p>Not final only because MockBukkit loads plugins through a generated subclass; not meant to
 * be extended.
 */
public class BeezyPlugin extends JavaPlugin implements Listener, PluginMessageListener {

    private BeezyCore core;
    private PluginSettings settings;
    private TeamSync teamSync;
    private BukkitTask teamSyncTask;
    private Method addChannel;

    /** @return the API, for plugins that depend on this one directly */
    public BeezyClientApi api() {
        return core;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new PluginSettings(getConfig(), getLogger());
        checkAdventure();

        core = new BeezyCore(new BukkitPlatform(this, Schedulers.forServer(this)),
                CoreConfig.builder().policy(settings.policy).debug(settings.debug).build());
        core.events().subscribe(UserHelloEvent.class, this::onHello);
        core.events().subscribe(TransferResultEvent.class, this::onTransferResult);

        // "beezy:server" is a valid channel name on every version: a plain string under 20
        // characters on 1.8-1.12, a namespaced key on 1.13+.
        getServer().getMessenger().registerOutgoingPluginChannel(this, Protocol.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, Protocol.CHANNEL, this);
        getServer().getServicesManager().register(BeezyClientApi.class, core, this, ServicePriority.Normal);
        BeezyProvider.unregister();
        BeezyProvider.register(core);
        getServer().getPluginManager().registerEvents(this, this);

        BeezyCommand command = new BeezyCommand(this, core);
        PluginCommand beezy = getCommand("beezy");
        beezy.setExecutor(command);
        beezy.setTabCompleter(command);

        startTeamSync();

        // After a /reload or a late enable: players already online got the server's REGISTER
        // before our channel existed. Announce it again so Beezy clients send a Hello.
        for (Player player : getServer().getOnlinePlayers()) {
            sendReset(player);
            announceChannels(player);
        }
    }

    @Override
    public void onDisable() {
        if (teamSyncTask != null) {
            teamSyncTask.cancel();
        }
        if (core != null) {
            core.shutdown();
            BeezyProvider.unregister();
            getServer().getServicesManager().unregister(BeezyClientApi.class, core);
        }
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    /**
     * Logs which Adventure is in use and checks that it can serialize components: the server's
     * own (Paper 1.16.5+) or the copy in this jar (Spigot, older servers).
     */
    private void checkAdventure() {
        try {
            boolean bundled = Component.class.getClassLoader() == getClass().getClassLoader();
            GsonComponentSerializer.gson().serialize(Component.text("Beezy"));
            debug("text components use " + (bundled ? "the Adventure bundled with Beezy" : "the server's Adventure"));
        } catch (LinkageError | RuntimeException e) {
            getLogger().log(Level.SEVERE, "Adventure is not usable on this server: holograms and nametags cannot be sent", e);
        }
    }

    private void startTeamSync() {
        if (!settings.teamSync) {
            return;
        }
        if (Schedulers.isFolia()) {
            // Scoreboards and other players' positions belong to different region threads on
            // Folia; there is no safe place to read them all at once.
            getLogger().warning("team-sync is not supported on Folia and stays off");
            return;
        }
        teamSync = new TeamSync(getServer(), core, settings.teamSyncPositions);
        teamSyncTask = getServer().getScheduler().runTaskTimer(this, teamSync, settings.teamSyncInterval, settings.teamSyncInterval);
    }

    // ---- join / quit ----------------------------------------------------------------------------

    /** Clears what a previous backend of the network left on screen, before anything else. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        sendReset(event.getPlayer());
    }

    /** Ends the player's Beezy session. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        core.handleQuit(id);
        if (teamSync != null) {
            teamSync.forget(id);
        }
    }

    /**
     * Sends a Reset to a player whose client may not have registered {@code beezy:server} yet.
     * Bukkit only delivers plugin messages on channels the client registered, so the channel is
     * marked as registered for this player first ({@code CraftPlayer#addChannel}, public on every
     * version). Clients without Beezy ignore a message on an unknown channel.
     */
    private void sendReset(Player player) {
        try {
            if (addChannel == null) {
                addChannel = player.getClass().getMethod("addChannel", String.class);
            }
            addChannel.invoke(player, Protocol.CHANNEL);
        } catch (ReflectiveOperationException | RuntimeException e) {
            debug("cannot pre-register the channel for " + player.getName() + ": " + e);
        }
        player.sendPluginMessage(this, Protocol.CHANNEL, BeezyCore.resetPayload());
    }

    /** Re-sends the server's REGISTER ({@code CraftPlayer#sendSupportedChannels}). */
    private void announceChannels(Player player) {
        try {
            player.getClass().getMethod("sendSupportedChannels").invoke(player);
        } catch (ReflectiveOperationException | RuntimeException e) {
            debug("cannot re-announce channels to " + player.getName() + " (they will say Hello on rejoin): " + e);
        }
    }

    // ---- what clients send ----------------------------------------------------------------------

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (Protocol.CHANNEL.equals(channel)) {
            core.handleIncoming(player.getUniqueId(), message);
        }
    }

    private void onHello(UserHelloEvent event) {
        Player player = getServer().getPlayer(event.user().uniqueId());
        if (player == null) {
            return;
        }
        getServer().getPluginManager().callEvent(new BeezyHelloEvent(player, event.user(), event.isFirst()));
        if (event.isFirst() && settings.defaultPresence != null) {
            event.user().richPresence().set(settings.defaultPresence);
        }
    }

    private void onTransferResult(TransferResultEvent event) {
        Player player = getServer().getPlayer(event.user().uniqueId());
        if (player != null) {
            getServer().getPluginManager().callEvent(new BeezyTransferResultEvent(player, event.user(), event.result()));
        }
    }

    private void debug(String message) {
        if (settings != null && settings.debug) {
            getLogger().info("[debug] " + message);
        }
    }
}
