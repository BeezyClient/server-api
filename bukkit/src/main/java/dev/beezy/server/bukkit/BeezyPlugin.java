package dev.beezy.server.bukkit;

import dev.beezy.server.api.BeezyApiCore;
import dev.beezy.server.api.BeezyCodec;
import dev.beezy.server.api.BeezyProtocol;
import dev.beezy.server.api.BeezyServerApi;
import dev.beezy.server.api.ClientMessage;
import dev.beezy.server.api.Hello;
import dev.beezy.server.api.RichPresence;
import dev.beezy.server.api.TransferResult;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
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
 * The Beezy plugin for Bukkit, Spigot, Paper and Folia, 1.8.8 to latest.
 *
 * <p>Registering {@code beezy:server} as an incoming channel makes the server list it in the
 * {@code REGISTER} it sends every player on join; a Beezy client answers with its Hello. On join
 * the plugin also sends a Reset, so nothing another backend of the same network sent stays on the
 * player's screen. The plugin sends nothing else on its own except the optional default presence
 * and team sync from config.yml, and never contacts anything.
 */
public final class BeezyPlugin extends JavaPlugin implements Listener, PluginMessageListener {

    private BeezyApiCore api;
    /** Players who said Hello during this visit, so a repeated Hello is not "first". */
    private final Set<UUID> greeted = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private boolean debug;
    private RichPresence defaultPresence;
    private TeamSync teamSync;
    private BukkitTask teamSyncTask;
    private Method addChannel;

    /** The API, for plugins that depend on this one directly instead of using the services manager. */
    public BeezyServerApi api() {
        return api;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        api = new BeezyApiCore((uuid, payload) -> {
            Player player = getServer().getPlayer(uuid);
            if (player != null) {
                player.sendPluginMessage(this, BeezyProtocol.CHANNEL, payload);
            }
        });

        // "beezy:server" is a valid channel name on every version: a plain string under 20
        // characters on 1.8-1.12, a namespaced key on 1.13+.
        getServer().getMessenger().registerOutgoingPluginChannel(this, BeezyProtocol.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, BeezyProtocol.CHANNEL, this);
        getServer().getServicesManager().register(BeezyServerApi.class, api, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(this, this);

        BeezyCommand command = new BeezyCommand(this);
        PluginCommand beezy = getCommand("beezy");
        beezy.setExecutor(command);
        beezy.setTabCompleter(command);
        getServer().getPluginManager().registerEvents(command, this);

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
        if (api != null) {
            api.forgetAll();
        }
        greeted.clear();
    }

    private void loadSettings() {
        FileConfiguration config = getConfig();
        debug = config.getBoolean("debug", false);

        defaultPresence = null;
        String game = config.getString("rich-presence.game", "");
        if (game != null && !game.isEmpty()) {
            try {
                defaultPresence = RichPresence.builder(game)
                        .mode(config.getString("rich-presence.mode", ""))
                        .state(config.getString("rich-presence.state", ""))
                        .build();
            } catch (IllegalArgumentException e) {
                getLogger().warning("default rich presence disabled, invalid config: " + e.getMessage());
            }
        }
    }

    private void startTeamSync() {
        if (!getConfig().getBoolean("team-sync.enabled", false)) {
            return;
        }
        if (isFolia()) {
            // Scoreboards and other players' positions belong to different region threads on
            // Folia; there is no safe place to read them all at once.
            getLogger().warning("team-sync is not supported on Folia and stays off");
            return;
        }
        long interval = Math.max(10L, getConfig().getLong("team-sync.interval-ticks", 40L));
        teamSync = new TeamSync(api, getConfig().getBoolean("team-sync.positions", true));
        teamSyncTask = getServer().getScheduler().runTaskTimer(this, teamSync, interval, interval);
    }

    // ---- join / quit -----------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        // Before anything else: clear what a previous backend of the network left on screen.
        sendReset(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        api.forget(id);
        greeted.remove(id);
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
            addChannel.invoke(player, BeezyProtocol.CHANNEL);
        } catch (ReflectiveOperationException | RuntimeException e) {
            debug("cannot pre-register the channel for " + player.getName() + ": " + e);
        }
        player.sendPluginMessage(this, BeezyProtocol.CHANNEL, BeezyCodec.reset());
    }

    /** Re-sends the server's REGISTER ({@code CraftPlayer#sendSupportedChannels}). */
    private void announceChannels(Player player) {
        try {
            player.getClass().getMethod("sendSupportedChannels").invoke(player);
        } catch (ReflectiveOperationException | RuntimeException e) {
            debug("cannot re-announce channels to " + player.getName() + " (they will say Hello on rejoin): " + e);
        }
    }

    // ---- Hello and transfer results -----------------------------------------------------------

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!BeezyProtocol.CHANNEL.equals(channel)) {
            return;
        }
        ClientMessage incoming = api.handleIncoming(player.getUniqueId(), message);
        if (incoming instanceof Hello) {
            onHello(player, (Hello) incoming);
        } else if (incoming instanceof TransferResult) {
            TransferResult result = (TransferResult) incoming;
            debug(player.getName() + " answers transfer " + result.id() + " to " + result.address() + ": " + result.status());
            getServer().getPluginManager().callEvent(new BeezyTransferResultEvent(player, result));
        } else {
            debug("ignored a malformed or unexpected message from " + player.getName() + " (" + message.length + " bytes)");
        }
    }

    private void onHello(Player player, Hello hello) {
        boolean first = greeted.add(player.getUniqueId());
        debug(player.getName() + " says Hello: " + hello + (first ? "" : " (again)"));
        getServer().getPluginManager().callEvent(new BeezyHelloEvent(player, api.player(player.getUniqueId()), hello, first));
        if (first && defaultPresence != null) {
            api.sendRichPresence(player.getUniqueId(), defaultPresence);
        }
    }

    void debug(String message) {
        if (debug) {
            getLogger().info("[debug] " + message);
        }
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
