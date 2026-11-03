package dev.beezy.server.bukkit;

import dev.beezy.server.api.TransferResult;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player's client answers a transfer this server asked for
 * ({@code BeezyServerApi#transfer}). {@link #getId()} is the id the transfer call returned or was
 * given. {@code CONNECTING} means the player is leaving for the address; {@code REFUSED} that they
 * switched transfers off; {@code INVALID_ADDRESS} that the address was not valid or a transfer was
 * already asked for in the last minute; {@code FAILED} that the client could not connect.
 */
public final class BeezyTransferResultEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final TransferResult result;

    public BeezyTransferResultEvent(Player player, TransferResult result) {
        this.player = player;
        this.result = result;
    }

    public Player getPlayer() {
        return player;
    }

    public TransferResult getResult() {
        return result;
    }

    /** The transfer's id. */
    public String getId() {
        return result.id();
    }

    public TransferResult.Status getStatus() {
        return result.status();
    }

    /** The address the player was asked to join. */
    public String getAddress() {
        return result.address();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
