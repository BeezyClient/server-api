package dev.beezy.api.exception;

import dev.beezy.stability.Stable;

/** The client already shows as many of that kind as it allows (64 waypoints, 32 beams, ...). */
@Stable
public class LimitExceededException extends BeezyException {

    private static final long serialVersionUID = 1L;

    private final int limit;

    /**
     * @param what the kind
     * @param limit the client's limit
     */
    public LimitExceededException(String what, int limit) {
        super(what + ": at most " + limit + " at once");
        this.limit = limit;
    }

    /** @return the limit */
    public int limit() {
        return limit;
    }
}
