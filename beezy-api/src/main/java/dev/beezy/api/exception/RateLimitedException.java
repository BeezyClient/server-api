package dev.beezy.api.exception;

import dev.beezy.stability.Stable;
import java.time.Duration;

/** Over a client rate limit: 5 notifications every 10 seconds, 1 transfer a minute. */
@Stable
public class RateLimitedException extends BeezyException {

    private static final long serialVersionUID = 1L;

    private final Duration retryAfter;

    /**
     * @param what what was limited
     * @param retryAfter when the next one would pass
     */
    public RateLimitedException(String what, Duration retryAfter) {
        super(what + " rate-limited, retry in " + retryAfter.toMillis() + " ms");
        this.retryAfter = retryAfter;
    }

    /** @return when the next one would pass */
    public Duration retryAfter() {
        return retryAfter;
    }
}
