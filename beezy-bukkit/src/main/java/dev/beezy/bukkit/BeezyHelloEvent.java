package dev.beezy.bukkit;

import dev.beezy.api.BeezyUser;
import dev.beezy.api.ClientInfo;
import dev.beezy.stability.Stable;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * A player's Beezy client said Hello: from now on they are a {@link BeezyUser}. Fired on the
 * thread that received the message (main thread; Folia: the player's region thread).
 */
@Stable
public final class BeezyHelloEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BeezyUser user;
    private final boolean first;

    BeezyHelloEvent(Player player, BeezyUser user, boolean first) {
        super(player);
        this.user = user;
        this.first = first;
    }

    /** @return the Beezy user */
    public BeezyUser getUser() {
        return user;
    }

    /** @return versions and features */
    public ClientInfo getClientInfo() {
        return user.client();
    }

    /** @return false when the same client says Hello again during the visit (what was sent is kept) */
    public boolean isFirst() {
        return first;
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
