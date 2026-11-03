package dev.beezy.bukkit;

import dev.beezy.api.BeezyUser;
import dev.beezy.api.transfer.TransferResult;
import dev.beezy.stability.Stable;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * A client answered a transfer this server asked for. The stage returned by
 * {@code user.transfer(...)} completes with the same result.
 */
@Stable
public final class BeezyTransferResultEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BeezyUser user;
    private final TransferResult result;

    BeezyTransferResultEvent(Player player, BeezyUser user, TransferResult result) {
        super(player);
        this.user = user;
        this.result = result;
    }

    /** @return the Beezy user */
    public BeezyUser getUser() {
        return user;
    }

    /** @return the answer */
    public TransferResult getResult() {
        return result;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /** @return the handler list */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
