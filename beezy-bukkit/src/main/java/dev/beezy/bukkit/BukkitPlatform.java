package dev.beezy.bukkit;

import dev.beezy.api.value.WorldKey;
import dev.beezy.core.spi.PlatformAdapter;
import dev.beezy.core.spi.Scheduler;
import dev.beezy.protocol.Protocol;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** The core's view of a Bukkit server. */
final class BukkitPlatform implements PlatformAdapter {

    private final Plugin plugin;
    private final Scheduler scheduler;

    BukkitPlatform(Plugin plugin, Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public void send(UUID player, byte[] payload) {
        Player p = plugin.getServer().getPlayer(player);
        if (p != null && p.isOnline()) {
            p.sendPluginMessage(plugin, Protocol.CHANNEL, payload);
        }
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public Logger logger() {
        return plugin.getLogger();
    }

    @Override
    public Optional<String> name(UUID player) {
        Player p = plugin.getServer().getPlayer(player);
        return p == null ? Optional.<String>empty() : Optional.of(p.getName());
    }

    @Override
    public Optional<WorldKey> world(UUID player) {
        Player p = plugin.getServer().getPlayer(player);
        return p == null ? Optional.<WorldKey>empty() : Optional.of(BeezyBukkit.world(p.getWorld()));
    }
}
