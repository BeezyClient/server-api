package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_ID;
import static dev.beezy.server.api.BeezyProtocol.MAX_LABEL;

/**
 * A HUD timer (kind 40): a countdown or a stopwatch, running or paused. The client shows at most 8.
 * To pause or resume, send the same id again with the current time and the new state.
 *
 * <pre>{@code
 * Timer round = Timer.countdown("round", "Round ends", 5 * 60_000);
 * Timer elapsed = Timer.stopwatch("game", "Game time", 0);
 * }</pre>
 */
public final class Timer {

    public enum Mode {
        COUNTDOWN, STOPWATCH;

        /** Wire value: 0 countdown, 1 stopwatch. */
        public int id() {
            return ordinal();
        }
    }

    private final String id;
    private final String label;
    private final Mode mode;
    private final long startMs;
    private final boolean running;

    /**
     * @param startMs countdown: time left; stopwatch: time already elapsed. 0 to 4 294 967 295.
     * @throws IllegalArgumentException if a field is over its cap or out of range
     */
    public Timer(String id, String label, Mode mode, long startMs, boolean running) {
        this.id = Check.id(id, MAX_ID, "timer id");
        this.label = Check.str(label, MAX_LABEL, "timer label");
        this.mode = Check.notNull(mode, "timer mode");
        this.startMs = Check.range(startMs, 0, 0xFFFFFFFFL, "timer start ms");
        this.running = running;
    }

    /** A running countdown with {@code leftMs} to go. */
    public static Timer countdown(String id, String label, long leftMs) {
        return new Timer(id, label, Mode.COUNTDOWN, leftMs, true);
    }

    /** A running stopwatch that already shows {@code elapsedMs}. */
    public static Timer stopwatch(String id, String label, long elapsedMs) {
        return new Timer(id, label, Mode.STOPWATCH, elapsedMs, true);
    }

    /** The same timer, paused at {@code atMs}. */
    public Timer pausedAt(long atMs) {
        return new Timer(id, label, mode, atMs, false);
    }

    /** The same timer, running again from {@code fromMs}. */
    public Timer runningFrom(long fromMs) {
        return new Timer(id, label, mode, fromMs, true);
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public Mode mode() {
        return mode;
    }

    public long startMs() {
        return startMs;
    }

    public boolean running() {
        return running;
    }
}
