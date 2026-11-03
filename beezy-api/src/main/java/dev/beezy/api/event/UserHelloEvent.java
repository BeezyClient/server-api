package dev.beezy.api.event;

import dev.beezy.api.BeezyUser;
import dev.beezy.stability.Internal;
import dev.beezy.stability.Stable;

/** A client said Hello: from now on it is a Beezy user and can be sent to. */
@Stable
public final class UserHelloEvent implements BeezyEvent {

    private final BeezyUser user;
    private final boolean first;

    /**
     * @param user the user
     * @param first false when the same client says Hello again during the visit
     */
    @Internal
    public UserHelloEvent(BeezyUser user, boolean first) {
        this.user = user;
        this.first = first;
    }

    @Override
    public BeezyUser user() {
        return user;
    }

    /** @return false when the same client says Hello again during the visit (state is kept) */
    public boolean isFirst() {
        return first;
    }
}
