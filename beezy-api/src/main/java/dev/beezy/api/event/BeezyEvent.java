package dev.beezy.api.event;

import dev.beezy.api.BeezyUser;
import dev.beezy.stability.Stable;

/** Something that happened to a Beezy user. */
@Stable
public interface BeezyEvent {

    /** @return the user */
    BeezyUser user();
}
