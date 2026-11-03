package dev.beezy.api.display;

import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.time.Duration;

/** A countdown or stopwatch on the HUD. 8 per player. */
@Stable
public final class Timer implements Element {

    /** How it counts. */
    @Stable
    public enum Mode {
        /** Down from the start value to 0. */
        COUNTDOWN,
        /** Up from the start value. */
        STOPWATCH
    }

    private final String id;
    private final String label;
    private final Mode mode;
    private final Duration start;
    private final boolean running;

    private Timer(String id, String label, Mode mode, Duration start, boolean running) {
        this.id = Lifetimes.id(id, "timer id");
        this.label = Validate.str(label, Protocol.MAX_LABEL, "timer label");
        this.mode = Validate.notNull(mode, "timer mode");
        Validate.notNull(start, "timer start");
        Validate.range(start.toMillis(), 0, 0xFFFFFFFFL, "timer start ms");
        this.start = start;
        this.running = running;
    }

    /**
     * @param id id
     * @param label label
     * @param timeLeft time left
     * @return a running countdown
     */
    public static Timer countdown(String id, String label, Duration timeLeft) {
        return new Timer(id, label, Mode.COUNTDOWN, timeLeft, true);
    }

    /**
     * @param id id
     * @param label label
     * @return a running stopwatch from 0
     */
    public static Timer stopwatch(String id, String label) {
        return new Timer(id, label, Mode.STOPWATCH, Duration.ZERO, true);
    }

    /**
     * @param value time left (countdown) or elapsed (stopwatch)
     * @return the same timer from that value
     */
    public Timer withStart(Duration value) {
        return new Timer(id, label, mode, value, running);
    }

    /** @return the same timer, paused */
    public Timer paused() {
        return new Timer(id, label, mode, start, false);
    }

    /** @return the same timer, running */
    public Timer running() {
        return new Timer(id, label, mode, start, true);
    }

    @Override
    public String id() {
        return id;
    }

    /** @return the label */
    public String label() {
        return label;
    }

    /** @return the mode */
    public Mode mode() {
        return mode;
    }

    /** @return the start value */
    public Duration start() {
        return start;
    }

    /** @return whether it runs */
    public boolean isRunning() {
        return running;
    }
}
