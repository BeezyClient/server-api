package dev.beezy.core;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/** A sliding window: at most {@code burst} events per {@code window}. Not thread-safe. */
final class RateLimiter {

    private final Clock clock;
    private final int burst;
    private final long windowMs;
    private final Deque<Long> times = new ArrayDeque<Long>();

    RateLimiter(Clock clock, int burst, Duration window) {
        this.clock = clock;
        this.burst = burst;
        this.windowMs = window.toMillis();
    }

    /** @return zero if allowed (and counted), else how long until it would be */
    Duration tryAcquire() {
        long now = clock.millis();
        while (!times.isEmpty() && now - times.peekFirst() >= windowMs) {
            times.pollFirst();
        }
        if (times.size() >= burst) {
            return Duration.ofMillis(Math.max(1, windowMs - (now - times.peekFirst())));
        }
        times.addLast(now);
        return Duration.ZERO;
    }

    void clear() {
        times.clear();
    }
}
