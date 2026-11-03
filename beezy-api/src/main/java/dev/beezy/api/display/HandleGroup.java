package dev.beezy.api.display;

import dev.beezy.api.DeliveryReport;
import dev.beezy.api.SendResult;
import dev.beezy.stability.Stable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * The handles of one element shown to an audience, one per user.
 *
 * @param <V> the value type
 * @param <H> the handle type
 */
@Stable
public final class HandleGroup<V extends Element, H extends Handle<V>> {

    private final List<H> handles;

    /** @param handles the handles, one per user */
    public HandleGroup(List<H> handles) {
        this.handles = Collections.unmodifiableList(new ArrayList<H>(handles));
    }

    /** @return the handles */
    public List<H> handles() {
        return handles;
    }

    /** @return the outcome per user */
    public DeliveryReport report() {
        Map<UUID, SendResult> m = new LinkedHashMap<UUID, SendResult>();
        for (H h : handles) {
            m.put(h.user().uniqueId(), h.result());
        }
        return new DeliveryReport(m);
    }

    /** @return whether any is active */
    public boolean anyActive() {
        for (H h : handles) {
            if (h.isActive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param value the new value, same id
     * @return the outcome per user
     */
    public DeliveryReport updateAll(V value) {
        Map<UUID, SendResult> m = new LinkedHashMap<UUID, SendResult>();
        for (H h : handles) {
            m.put(h.user().uniqueId(), h.update(value));
        }
        return new DeliveryReport(m);
    }

    /** @return the outcome per user */
    public DeliveryReport removeAll() {
        Map<UUID, SendResult> m = new LinkedHashMap<UUID, SendResult>();
        for (H h : handles) {
            m.put(h.user().uniqueId(), h.remove());
        }
        return new DeliveryReport(m);
    }
}
