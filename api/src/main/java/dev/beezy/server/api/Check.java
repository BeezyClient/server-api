package dev.beezy.server.api;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Argument checks shared by the value classes and the encoder. */
final class Check {

    private Check() {
    }

    static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    /** A string that must fit {@code cap} UTF-8 bytes. Null is rejected. */
    static String str(String value, int cap, String field) {
        notNull(value, field);
        int len = value.getBytes(StandardCharsets.UTF_8).length;
        if (len > cap) {
            throw new IllegalArgumentException(field + " is " + len + " UTF-8 bytes, at most " + cap + " allowed");
        }
        return value;
    }

    /** An id: like {@link #str}, but empty is rejected too (the client drops a message with an empty id). */
    static String id(String value, int cap, String field) {
        str(value, cap, field);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return value;
    }

    /** Like {@link #str} but null means empty. */
    static String optStr(String value, int cap, String field) {
        return str(value == null ? "" : value, cap, field);
    }

    static int range(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " is " + value + ", must be " + min + ".." + max);
        }
        return value;
    }

    static long range(long value, long min, long max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " is " + value + ", must be " + min + ".." + max);
        }
        return value;
    }

    static double finite(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(field + " must be a finite number");
        }
        return value;
    }

    /** A colour on the wire: opaque ARGB ({@code 0xFFrrggbb}), or 0 (the client's default) for null. Alpha is ignored. */
    static int argb(Color color) {
        return color == null ? 0 : 0xFF000000 | (color.getRGB() & 0xFFFFFF);
    }

    /**
     * A {@code duration} field in milliseconds: null or zero means "until removed" (0); otherwise
     * 1 ms to 24 h. A positive duration under 1 ms counts as 1 ms, so it never turns into "forever".
     */
    static long durationMs(Duration duration, String field) {
        if (duration == null || duration.isZero()) {
            return 0;
        }
        if (duration.isNegative()) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        if (duration.compareTo(Duration.ofMillis(BeezyProtocol.MAX_DURATION_MS)) > 0) {
            throw new IllegalArgumentException(field + " is " + duration + ", at most 24 hours allowed");
        }
        return Math.max(1L, duration.toMillis());
    }
}
