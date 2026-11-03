package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_NOTIFICATION_SECONDS;
import static dev.beezy.server.api.BeezyProtocol.MAX_NOTIFICATION_TEXT;
import static dev.beezy.server.api.BeezyProtocol.MAX_NOTIFICATION_TITLE;
import static dev.beezy.server.api.BeezyProtocol.MIN_NOTIFICATION_SECONDS;

/**
 * A toast notification (kind 20). The client shows at most 3 at once and at most 5 every 10
 * seconds, and strips {@code §} formatting codes.
 *
 * <pre>{@code
 * Notification n = Notification.builder("Bed Wars", "Your bed was destroyed!")
 *         .level(Notification.Level.WARNING).seconds(8).build();
 * }</pre>
 */
public final class Notification {

    public enum Level {
        INFO, SUCCESS, WARNING, ERROR;

        /** Wire value: 0 info, 1 success, 2 warning, 3 error. */
        public int id() {
            return ordinal();
        }
    }

    private final String title;
    private final String text;
    private final Level level;
    private final int seconds;

    private Notification(Builder b) {
        this.title = Check.str(b.title, MAX_NOTIFICATION_TITLE, "notification title");
        this.text = Check.str(b.text, MAX_NOTIFICATION_TEXT, "notification text");
        this.level = Check.notNull(b.level, "notification level");
        this.seconds = Check.range(b.seconds, MIN_NOTIFICATION_SECONDS, MAX_NOTIFICATION_SECONDS, "notification seconds");
    }

    /** Info level, 5 seconds. */
    public static Notification of(String title, String text) {
        return builder(title, text).build();
    }

    public static Builder builder(String title, String text) {
        return new Builder(title, text);
    }

    public String title() {
        return title;
    }

    public String text() {
        return text;
    }

    public Level level() {
        return level;
    }

    public int seconds() {
        return seconds;
    }

    public static final class Builder {
        private final String title;
        private final String text;
        private Level level = Level.INFO;
        private int seconds = 5;

        private Builder(String title, String text) {
            this.title = title;
            this.text = text;
        }

        public Builder level(Level level) {
            this.level = level;
            return this;
        }

        /** 1 to 30. Default 5. */
        public Builder seconds(int seconds) {
            this.seconds = seconds;
            return this;
        }

        /** @throws IllegalArgumentException if a field is over its cap or out of range */
        public Notification build() {
            return new Notification(this);
        }
    }
}
