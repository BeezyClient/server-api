package dev.beezy.api.event;

import dev.beezy.api.BeezyUser;
import dev.beezy.api.transfer.TransferResult;
import dev.beezy.stability.Internal;
import dev.beezy.stability.Stable;

/** A client answered a transfer this server asked for (its stage completes too). */
@Stable
public final class TransferResultEvent implements BeezyEvent {

    private final BeezyUser user;
    private final TransferResult result;

    /**
     * @param user the user
     * @param result the answer
     */
    @Internal
    public TransferResultEvent(BeezyUser user, TransferResult result) {
        this.user = user;
        this.result = result;
    }

    @Override
    public BeezyUser user() {
        return user;
    }

    /** @return the answer */
    public TransferResult result() {
        return result;
    }
}
