package dev.beezy.server.bukkit;

import dev.beezy.server.api.BeezyPlayer;
import dev.beezy.server.api.Hello;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player's Beezy client says Hello: from here on the API can send to them. A good
 * place to send waypoints, presence or a team. {@link #isFirst()} is false when the client says
 * Hello again during the same session (the client may do so after a respawn into another world).
 */
public final class BeezyHelloEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final BeezyPlayer beezyPlayer;
    private final Hello hello;
    private final boolean first;

    public BeezyHelloEvent(Player player, BeezyPlayer beezyPlayer, Hello hello, boolean first) {
        this.player = player;
        this.beezyPlayer = beezyPlayer;
        this.hello = hello;
        this.first = first;
    }

    public Player getPlayer() {
        return player;
    }

    public BeezyPlayer getBeezyPlayer() {
        return beezyPlayer;
    }

    public Hello getHello() {
        return hello;
    }

    public boolean isFirst() {
        return first;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
