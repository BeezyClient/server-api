package dev.beezy.api.exception;

import dev.beezy.stability.Stable;
import java.util.UUID;

/** The player is no longer a connected Beezy player (left, or the plugin was disabled). */
@Stable
public class UserDisconnectedException extends BeezyException {

    private static final long serialVersionUID = 1L;

    /** @param user the player */
    public UserDisconnectedException(UUID user) {
        super(user + " is not connected");
    }
}
