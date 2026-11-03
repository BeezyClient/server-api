package dev.beezy.api.exception;

import dev.beezy.api.Feature;
import dev.beezy.stability.Stable;
import java.util.UUID;

/** The player switched that feature off, or their Beezy does not have it. */
@Stable
public class FeatureUnavailableException extends BeezyException {

    private static final long serialVersionUID = 1L;

    private final transient Feature feature;
    private final UUID user;

    /**
     * @param user the player
     * @param feature the feature
     */
    public FeatureUnavailableException(UUID user, Feature feature) {
        super("feature " + feature + " is off for " + user);
        this.user = user;
        this.feature = feature;
    }

    /** @return the feature */
    public Feature feature() {
        return feature;
    }

    /** @return the player */
    public UUID user() {
        return user;
    }
}
