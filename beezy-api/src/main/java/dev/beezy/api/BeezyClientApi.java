package dev.beezy.api;

import dev.beezy.api.event.EventBus;
import dev.beezy.api.value.WorldKey;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * The entry point: Beezy users, audiences and events. Get it with {@link #get()} or from the
 * platform's service registry (Bukkit: {@code ServicesManager}).
 */
@Stable
@NonExtendable
public interface BeezyClientApi {

    /**
     * @return the running API
     * @throws IllegalStateException if the Beezy plugin is not enabled
     */
    static BeezyClientApi get() {
        return BeezyProvider.get();
    }

    /**
     * @param player a player's UUID
     * @return the user, if that player is online and said Hello (uses Beezy)
     */
    Optional<BeezyUser> user(UUID player);

    /**
     * @param player a player's UUID
     * @return whether they are a connected Beezy user
     */
    boolean isBeezy(UUID player);

    /** @return every connected Beezy user (a snapshot) */
    Collection<BeezyUser> users();

    /** @return every Beezy user, evaluated each time it is used */
    BeezyAudience all();

    /**
     * @param players player UUIDs; those who are not Beezy users are left out
     * @return the audience
     */
    BeezyAudience audience(Collection<UUID> players);

    /**
     * @param world a dimension
     * @return the Beezy users in it, evaluated each time it is used
     */
    BeezyAudience world(WorldKey world);

    /** @return the event bus */
    EventBus events();

    /** @return what happens when something cannot be sent */
    DeliveryPolicy deliveryPolicy();
}
