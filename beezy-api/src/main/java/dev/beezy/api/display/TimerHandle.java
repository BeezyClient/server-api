package dev.beezy.api.display;

import dev.beezy.api.SendResult;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.time.Duration;

/** A {@link Timer} shown to a user, which the server can pause and resume. */
@Stable
@NonExtendable
public interface TimerHandle extends Handle<Timer> {

    /**
     * @return the value the client shows now: time left (countdown, not below 0) or time
     *     elapsed (stopwatch), computed from when it was sent
     */
    Duration current();

    /** @return the outcome of sending the timer paused at {@link #current()} */
    SendResult pause();

    /** @return the outcome of sending the timer running from {@link #current()} */
    SendResult resume();
}
