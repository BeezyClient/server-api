package dev.beezy.api;

import dev.beezy.api.exception.FeatureUnavailableException;
import dev.beezy.api.service.BeamService;
import dev.beezy.api.service.BorderService;
import dev.beezy.api.service.CooldownService;
import dev.beezy.api.service.FoodService;
import dev.beezy.api.service.GlowService;
import dev.beezy.api.service.HeightLimitService;
import dev.beezy.api.service.HologramService;
import dev.beezy.api.service.NametagService;
import dev.beezy.api.service.NickHiderService;
import dev.beezy.api.service.NotificationService;
import dev.beezy.api.service.RichPresenceService;
import dev.beezy.api.service.SettingsService;
import dev.beezy.api.service.TeamService;
import dev.beezy.api.service.TimerService;
import dev.beezy.api.service.TntService;
import dev.beezy.api.service.WaypointService;
import dev.beezy.api.transfer.TransferResult;
import dev.beezy.api.value.WorldKey;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * One player's Beezy session on this server, from their Hello until they leave. The server
 * remembers what it showed them, so handles can be updated or removed and everything can be
 * re-sent ({@link #resync()}) or cleared ({@link #reset()}).
 *
 * <p>A server can never look: it knows only the Hello (versions, features that are on) and the
 * answers to its own transfers.
 */
@Stable
@NonExtendable
public interface BeezyUser {

    /** @return the player's UUID */
    UUID uniqueId();

    /** @return the player's name, if the platform knows it */
    Optional<String> name();

    /** @return what the client said in its last Hello */
    ClientInfo client();

    /** @return the features that are on */
    default Set<Feature> features() {
        return client().features();
    }

    /**
     * @param feature a feature
     * @return whether it is on
     */
    default boolean supports(Feature feature) {
        return client().has(feature);
    }

    /**
     * @param feature a feature
     * @throws FeatureUnavailableException if it is off
     */
    default void require(Feature feature) {
        if (!supports(feature)) {
            throw new FeatureUnavailableException(uniqueId(), feature);
        }
    }

    /** @return whether the player is still online with Beezy */
    boolean isConnected();

    /** @return the dimension the player is in, if the platform can tell */
    Optional<WorldKey> world();

    /** @return waypoints */
    WaypointService waypoints();

    /** @return beams */
    BeamService beams();

    /** @return borders */
    BorderService borders();

    /** @return holograms */
    HologramService holograms();

    /** @return glowing entities */
    GlowService glow();

    /** @return nametags */
    NametagService nametags();

    /** @return notifications */
    NotificationService notifications();

    /** @return cooldown bars */
    CooldownService cooldowns();

    /** @return timers */
    TimerService timers();

    /** @return team mates */
    TeamService team();

    /** @return Discord rich presence */
    RichPresenceService richPresence();

    /** @return module settings overrides */
    SettingsService settings();

    /** @return the Nick Hider name */
    NickHiderService nickHider();

    /** @return food values */
    FoodService food();

    /** @return the TNT fuse length */
    TntService tnt();

    /** @return the build height */
    HeightLimitService heightLimit();

    /**
     * Sends the player to another server, like the vanilla 1.20.5+ transfer but on every
     * version. At most one a minute.
     *
     * @param address {@code host} or {@code host:port}
     * @return completes with the client's answer; exceptionally with
     *     {@link FeatureUnavailableException} (transfers off), a
     *     {@link dev.beezy.api.exception.RateLimitedException},
     *     {@link dev.beezy.api.exception.UserDisconnectedException} (left before answering) or
     *     {@link dev.beezy.api.exception.TransferTimeoutException}
     * @throws IllegalArgumentException if the address is empty or over 255 bytes
     */
    CompletionStage<TransferResult> transfer(String address);

    /**
     * Clears everything this server sent (settings are given back) and forgets it.
     *
     * @return the outcome
     */
    SendResult reset();

    /**
     * Sends a Reset followed by everything still active, e.g. after the client lost its state.
     * Elements whose duration has passed are not sent again.
     *
     * @return the outcome of the Reset
     */
    SendResult resync();
}
