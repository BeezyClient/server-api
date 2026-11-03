package dev.beezy.bukkit;

import dev.beezy.api.DeliveryPolicy;
import dev.beezy.api.display.RichPresence;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

/** config.yml, read once on enable. Invalid values turn that feature off with a warning. */
final class PluginSettings {

    final boolean debug;
    final DeliveryPolicy policy;
    final RichPresence defaultPresence;
    final boolean teamSync;
    final long teamSyncInterval;
    final boolean teamSyncPositions;

    PluginSettings(FileConfiguration config, Logger logger) {
        debug = config.getBoolean("debug", false);
        String p = config.getString("delivery-policy", "skip");
        DeliveryPolicy policy = DeliveryPolicy.SKIP;
        try {
            policy = DeliveryPolicy.valueOf(p.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning("delivery-policy must be skip or throw, not " + p + "; using skip");
        }
        this.policy = policy;

        RichPresence presence = null;
        String game = config.getString("rich-presence.game", "");
        if (game != null && !game.isEmpty()) {
            try {
                presence = RichPresence.builder(game)
                        .mode(config.getString("rich-presence.mode", ""))
                        .state(config.getString("rich-presence.state", ""))
                        .build();
            } catch (IllegalArgumentException e) {
                logger.warning("default rich presence disabled, invalid config: " + e.getMessage());
            }
        }
        this.defaultPresence = presence;

        teamSync = config.getBoolean("team-sync.enabled", false);
        teamSyncInterval = Math.max(10L, config.getLong("team-sync.interval-ticks", 40L));
        teamSyncPositions = config.getBoolean("team-sync.positions", true);
    }
}
