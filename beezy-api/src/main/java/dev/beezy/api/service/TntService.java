package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.time.Duration;
import java.util.OptionalInt;

/** The TNT fuse length the TNT Timer module counts from on this server (vanilla: 80 ticks). */
@Stable
@NonExtendable
public interface TntService {

    /**
     * @param ticks 1-1200
     * @return the outcome
     */
    SendResult setFuseTicks(int ticks);

    /**
     * @param fuse rounded to the nearest tick (50 ms), 1 tick to 60 s
     * @return the outcome
     */
    SendResult setFuse(Duration fuse);

    /** @return the outcome */
    SendResult clear();

    /** @return the fuse last sent, in ticks */
    OptionalInt current();
}
