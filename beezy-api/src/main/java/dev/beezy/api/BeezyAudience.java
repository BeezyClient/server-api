package dev.beezy.api;

import dev.beezy.api.display.Beam;
import dev.beezy.api.display.BeamHandle;
import dev.beezy.api.display.Border;
import dev.beezy.api.display.BorderHandle;
import dev.beezy.api.display.HandleGroup;
import dev.beezy.api.display.Hologram;
import dev.beezy.api.display.HologramHandle;
import dev.beezy.api.display.Notification;
import dev.beezy.api.display.Waypoint;
import dev.beezy.api.display.WaypointHandle;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Several Beezy users at once: one, a collection, everyone, a world. Sending to an audience
 * sends to each user under the same rules as sending to them one by one.
 *
 * <pre>{@code
 * beezy.all().supporting(Feature.NOTIFICATIONS).notify(Notification.of("Event", "Starting in 5 minutes"));
 * HandleGroup<Beam, BeamHandle> drop = beezy.world(WorldKey.OVERWORLD).showBeam(beam);
 * drop.removeAll();
 * }</pre>
 */
@Stable
@FunctionalInterface
public interface BeezyAudience extends Iterable<BeezyUser> {

    /** @return the users, evaluated now */
    Collection<BeezyUser> users();

    @Override
    default Iterator<BeezyUser> iterator() {
        return users().iterator();
    }

    /**
     * @param users users
     * @return a fixed audience of them
     */
    static BeezyAudience of(Collection<? extends BeezyUser> users) {
        final List<BeezyUser> copy = Collections.unmodifiableList(new ArrayList<BeezyUser>(Validate.notNull(users, "users")));
        return () -> copy;
    }

    /**
     * @param user a user
     * @return an audience of one
     */
    static BeezyAudience of(BeezyUser user) {
        return of(Collections.singletonList(Validate.notNull(user, "user")));
    }

    /** @return no one */
    static BeezyAudience empty() {
        return Collections::emptyList;
    }

    /**
     * @param filter which users to keep
     * @return a filtered view, evaluated each time
     */
    default BeezyAudience filter(Predicate<? super BeezyUser> filter) {
        return () -> {
            List<BeezyUser> out = new ArrayList<BeezyUser>();
            for (BeezyUser u : users()) {
                if (filter.test(u)) {
                    out.add(u);
                }
            }
            return out;
        };
    }

    /**
     * @param feature a feature
     * @return the users who have it on
     */
    default BeezyAudience supporting(Feature feature) {
        return filter(u -> u.supports(feature));
    }

    /** @return how many users, now */
    default int size() {
        return users().size();
    }

    /**
     * Sends to each user.
     *
     * @param send what to do per user
     * @return the outcome per user
     */
    default DeliveryReport sendEach(Function<? super BeezyUser, SendResult> send) {
        Map<UUID, SendResult> m = new LinkedHashMap<UUID, SendResult>();
        for (BeezyUser u : users()) {
            m.put(u.uniqueId(), send.apply(u));
        }
        return new DeliveryReport(m);
    }

    /**
     * @param notification the notification
     * @return the outcome per user
     */
    default DeliveryReport notify(Notification notification) {
        return sendEach(u -> u.notifications().send(notification));
    }

    /**
     * @param waypoint the waypoint
     * @return the handles
     */
    default HandleGroup<Waypoint, WaypointHandle> showWaypoint(Waypoint waypoint) {
        List<WaypointHandle> hs = new ArrayList<WaypointHandle>();
        for (BeezyUser u : users()) {
            hs.add(u.waypoints().show(waypoint));
        }
        return new HandleGroup<Waypoint, WaypointHandle>(hs);
    }

    /**
     * @param beam the beam
     * @return the handles
     */
    default HandleGroup<Beam, BeamHandle> showBeam(Beam beam) {
        List<BeamHandle> hs = new ArrayList<BeamHandle>();
        for (BeezyUser u : users()) {
            hs.add(u.beams().show(beam));
        }
        return new HandleGroup<Beam, BeamHandle>(hs);
    }

    /**
     * @param border the border
     * @return the handles
     */
    default HandleGroup<Border, BorderHandle> showBorder(Border border) {
        List<BorderHandle> hs = new ArrayList<BorderHandle>();
        for (BeezyUser u : users()) {
            hs.add(u.borders().show(border));
        }
        return new HandleGroup<Border, BorderHandle>(hs);
    }

    /**
     * @param hologram the hologram
     * @return the handles
     */
    default HandleGroup<Hologram, HologramHandle> showHologram(Hologram hologram) {
        List<HologramHandle> hs = new ArrayList<HologramHandle>();
        for (BeezyUser u : users()) {
            hs.add(u.holograms().show(hologram));
        }
        return new HandleGroup<Hologram, HologramHandle>(hs);
    }

    /** @return the outcome per user of clearing everything this server sent */
    default DeliveryReport reset() {
        return sendEach(BeezyUser::reset);
    }
}
