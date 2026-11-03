package dev.beezy.api;

import dev.beezy.stability.Stable;

/** The outcome of one send under {@link DeliveryPolicy#SKIP}. */
@Stable
public enum SendResult {
    /** Sent to the client. */
    SENT,
    /** The player switched that feature off (or their Beezy has no such feature). */
    FEATURE_UNAVAILABLE,
    /** Over a client rate limit (5 notifications every 10 s, 1 transfer a minute). */
    RATE_LIMITED,
    /** Over the client's count for that kind (64 waypoints, 32 beams, ...). */
    LIMIT_REACHED,
    /** The player is no longer a connected Beezy player. */
    DISCONNECTED;

    /** @return whether it was sent */
    public boolean isSent() {
        return this == SENT;
    }
}
