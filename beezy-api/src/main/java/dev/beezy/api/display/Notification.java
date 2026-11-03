package dev.beezy.api.display;

import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.time.Duration;

/**
 * A toast. Title at most 32 bytes, text at most 160 (plain: the client removes {@code §} codes),
 * shown 1-30 s. The client shows at most 3 at once and accepts 5 every 10 s.
 */
@Stable
public final class Notification {

    /** How it looks. */
    @Stable
    public enum Level {
        /** Neutral. */
        INFO,
        /** Green. */
        SUCCESS,
        /** Yellow. */
        WARNING,
        /** Red. */
        ERROR
    }

    private final String title;
    private final String text;
    private final Level level;
    private final int seconds;

    private Notification(Builder b) {
        this.title = Validate.str(b.title, Protocol.MAX_NOTIFICATION_TITLE, "notification title");
        this.text = Validate.str(b.text, Protocol.MAX_NOTIFICATION_TEXT, "notification text");
        this.level = Validate.notNull(b.level, "notification level");
        this.seconds = Validate.range(b.seconds, Protocol.MIN_NOTIFICATION_SECONDS, Protocol.MAX_NOTIFICATION_SECONDS, "notification seconds");
    }

    /**
     * @param title title
     * @param text text
     * @return an info notification shown 5 s
     */
    public static Notification of(String title, String text) {
        return builder(title, text).build();
    }

    /**
     * @param title title
     * @param text text
     * @return a builder
     */
    public static Builder builder(String title, String text) {
        return new Builder(title, text);
    }

    /** @return the title */
    public String title() {
        return title;
    }

    /** @return the text */
    public String text() {
        return text;
    }

    /** @return the level */
    public Level level() {
        return level;
    }

    /** @return how long it is shown */
    public Duration duration() {
        return Duration.ofSeconds(seconds);
    }

    /** Builds a {@link Notification}. */
    public static final class Builder {
        private final String title;
        private final String text;
        private Level level = Level.INFO;
        private int seconds = 5;

        private Builder(String title, String text) {
            this.title = title;
            this.text = text;
        }

        /**
         * @param level the level
         * @return this
         */
        public Builder level(Level level) {
            this.level = level;
            return this;
        }

        /**
         * @param duration 1-30 s, rounded to whole seconds
         * @return this
         */
        public Builder duration(Duration duration) {
            Validate.notNull(duration, "notification duration");
            this.seconds = (int) Math.min(Integer.MAX_VALUE, (duration.toMillis() + 500) / 1000);
            return this;
        }

        /**
         * @param seconds 1-30
         * @return this
         */
        public Builder seconds(int seconds) {
            this.seconds = seconds;
            return this;
        }

        /** @return the notification */
        public Notification build() {
            return new Notification(this);
        }
    }
}
