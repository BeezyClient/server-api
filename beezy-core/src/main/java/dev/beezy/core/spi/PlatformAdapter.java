package dev.beezy.core.spi;

import dev.beezy.api.value.WorldKey;
import dev.beezy.stability.Stable;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The platform under the core. Implementations must be thread-safe: the core calls them from
 * whatever thread the API is used on.
 */
@Stable
public interface PlatformAdapter {

    /**
     * Sends a payload on {@code beezy:server} to the player, if online. Must not block; must
     * keep the order of calls for the same player.
     *
     * @param player the player
     * @param payload the complete message
     */
    void send(UUID player, byte[] payload);

    /** @return the scheduler for transfer timeouts */
    Scheduler scheduler();

    /** @return where the core logs */
    Logger logger();

    /**
     * @param player a player
     * @return their name, if online
     */
    default Optional<String> name(UUID player) {
        return Optional.empty();
    }

    /**
     * @param player a player
     * @return the dimension they are in, if the platform can tell
     */
    default Optional<WorldKey> world(UUID player) {
        return Optional.empty();
    }
}
