package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.api.display.HeightLimit;
import dev.beezy.api.value.WorldKey;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.Collection;

/** The build height shown by the Height Limit HUD, one per dimension. */
@Stable
@NonExtendable
public interface HeightLimitService {

    /**
     * @param limit the limit
     * @return the outcome
     */
    SendResult set(HeightLimit limit);

    /**
     * @param world the dimension
     * @param minY lowest
     * @param maxY highest
     * @return the outcome
     */
    default SendResult set(WorldKey world, int minY, int maxY) {
        return set(HeightLimit.of(world, minY, maxY));
    }

    /** @return the outcome of clearing every limit */
    SendResult clear();

    /** @return the limits sent */
    Collection<HeightLimit> current();
}
