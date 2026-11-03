package dev.beezy.api.event;

import dev.beezy.api.BeezyUser;
import dev.beezy.stability.Internal;
import dev.beezy.stability.Stable;

/** A Beezy user left; their handles are inactive and their client forgets everything. */
@Stable
public final class UserDisconnectEvent implements BeezyEvent {

    private final BeezyUser user;

    /** @param user the user */
    @Internal
    public UserDisconnectEvent(BeezyUser user) {
        this.user = user;
    }

    @Override
    public BeezyUser user() {
        return user;
    }
}
