package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.api.display.RichPresence;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.Optional;

/** The user's Discord rich presence while on this server. */
@Stable
@NonExtendable
public interface RichPresenceService {

    /**
     * @param presence the presence
     * @return the outcome
     */
    SendResult set(RichPresence presence);

    /** @return the outcome */
    SendResult clear();

    /** @return the presence last sent */
    Optional<RichPresence> current();
}
