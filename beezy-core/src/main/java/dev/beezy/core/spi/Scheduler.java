package dev.beezy.core.spi;

import dev.beezy.stability.Stable;
import java.time.Duration;

/**
 * Runs delayed tasks for the core: only the transfer-answer timeout. Element durations are never scheduled; the client removes those by itself. Tasks only touch the core's own
 * thread-safe state, so any thread will do (Folia: the global region or async scheduler).
 */
@Stable
public interface Scheduler {

    /**
     * @param delay how long to wait
     * @param task what to run
     * @return a way to cancel it
     */
    Task schedule(Duration delay, Runnable task);

    /** A scheduled task. */
    @Stable
    interface Task {
        /** Cancels it if it has not run yet. */
        void cancel();
    }
}
