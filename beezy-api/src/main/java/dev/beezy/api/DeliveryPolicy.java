package dev.beezy.api;

import dev.beezy.stability.Stable;

/**
 * What happens when something cannot be sent: the player switched the feature off, a client
 * limit was reached (notifications per 10 s, transfers per minute, elements of a kind) or the
 * player left. Set server-wide by the platform (Bukkit: {@code delivery-policy} in config.yml).
 */
@Stable
public enum DeliveryPolicy {
    /**
     * Drop it and report it: methods return a {@link SendResult} other than
     * {@link SendResult#SENT}, handles are inactive. Nothing is thrown. The default.
     */
    SKIP,
    /**
     * Throw a {@link dev.beezy.api.exception.BeezyException} subclass
     * ({@code FeatureUnavailableException}, {@code RateLimitedException},
     * {@code LimitExceededException}, {@code UserDisconnectedException}). Transfers complete
     * their stage exceptionally instead.
     */
    THROW
}
