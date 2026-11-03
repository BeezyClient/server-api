package dev.beezy.server.api;

import java.util.UUID;

/** How a platform delivers a payload on {@code beezy:server} to one player. */
@FunctionalInterface
public interface Transport {

    /**
     * Sends {@code payload} to the player, if they are online. Must not throw for an offline
     * player; the payload is complete (format byte included) and already validated.
     */
    void send(UUID player, byte[] payload);
}
