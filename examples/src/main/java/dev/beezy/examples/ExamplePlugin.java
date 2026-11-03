package dev.beezy.examples;

import dev.beezy.api.BeezyClientApi;
import dev.beezy.api.BeezyUser;
import dev.beezy.api.Feature;
import dev.beezy.api.display.Border;
import dev.beezy.api.display.Cooldown;
import dev.beezy.api.display.Hologram;
import dev.beezy.api.display.HologramHandle;
import dev.beezy.api.display.Notification;
import dev.beezy.api.display.RichPresence;
import dev.beezy.api.display.Timer;
import dev.beezy.api.display.TimerHandle;
import dev.beezy.api.display.Waypoint;
import dev.beezy.api.event.UserHelloEvent;
import dev.beezy.api.settings.SettingKey;
import dev.beezy.api.transfer.TransferResult;
import dev.beezy.api.value.Color;
import dev.beezy.api.value.Position;
import dev.beezy.api.value.WorldKey;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Greets Beezy players with a hologram and a waypoint, and offers {@code /lobby} (a Beezy
 * transfer with a fallback) and {@code /round} (an arena border, a timer and a cooldown).
 */
public final class ExamplePlugin extends JavaPlugin {

    /** Typed setting key: Beezy's zoom module, its maximum zoom. */
    private static final SettingKey<Integer> MAX_ZOOM = SettingKey.integer("zoom", "max_zoom");

    private BeezyClientApi beezy;

    @Override
    public void onEnable() {
        beezy = BeezyClientApi.get(); // or getServer().getServicesManager().load(BeezyClientApi.class)
        beezy.events().subscribe(UserHelloEvent.class, this::welcome);
    }

    private void welcome(UserHelloEvent event) {
        if (!event.isFirst()) {
            return; // the same client said Hello again: what we sent is still there
        }
        BeezyUser user = event.user();
        user.notifications().send(Notification.builder("Welcome", "Beezy features are on here")
                .level(Notification.Level.SUCCESS).seconds(8).build());

        HologramHandle sign = user.holograms().show(Hologram.builder("welcome")
                .at(Position.of(0.5, 66, 0.5)).world(WorldKey.OVERWORLD)
                .line(Component.text("Welcome!", NamedTextColor.GOLD))
                .line("§7Have fun, " + user.name().orElse("player"))
                .duration(Duration.ofMinutes(5)) // the client removes it by itself
                .build());
        getLogger().info("hologram shown: " + sign.isActive());

        user.waypoints().show(Waypoint.builder("spawn").name("Spawn").at(0, 65, 0)
                .world(WorldKey.OVERWORLD).color(Color.GREEN).beam(true).build());
        user.richPresence().set(RichPresence.builder("Example Server").state("In the lobby").build());
        user.settings().set(MAX_ZOOM, 4);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        final Player player = (Player) sender;
        BeezyUser user = beezy.user(player.getUniqueId()).orElse(null);
        if (user == null) {
            player.sendMessage(ChatColor.GRAY + "Install Beezy Client for this feature.");
            return true;
        }
        if (label.equalsIgnoreCase("lobby")) {
            if (!user.supports(Feature.TRANSFER)) {
                player.sendMessage(ChatColor.GRAY + "Transfers are off in your Beezy settings.");
                return true;
            }
            user.transfer("lobby.example.net").whenComplete((result, error) -> {
                if (error != null || result.status() != TransferResult.Status.CONNECTING) {
                    player.sendMessage(ChatColor.RED + "Could not send you to the lobby.");
                }
            });
        } else if (label.equalsIgnoreCase("round")) {
            user.borders().show(Border.builder("arena").world(WorldKey.OVERWORLD).area(-50, -50, 50, 50)
                    .color(Color.RED).solid(true).onlyWhenNear(true).duration(Duration.ofMinutes(10)).build());
            TimerHandle timer = user.timers().show(Timer.countdown("round", "Round ends", Duration.ofMinutes(10)));
            user.cooldowns().show(Cooldown.of("pearl", "Ender Pearl", Duration.ofSeconds(15)).withIcon("minecraft:ender_pearl"));
            getLogger().info(player.getName() + ": round timer at " + timer.current().getSeconds() + " s");
        }
        return true;
    }
}
