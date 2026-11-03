package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.Optional;

/** The name Beezy's Nick Hider shows for the user themselves (for servers with {@code /nick}). */
@Stable
@NonExtendable
public interface NickHiderService {

    /**
     * @param nick 1-16 bytes
     * @return the outcome
     */
    SendResult set(String nick);

    /** @return the outcome */
    SendResult clear();

    /** @return the nick last sent */
    Optional<String> current();
}
