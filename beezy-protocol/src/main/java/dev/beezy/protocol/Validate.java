package dev.beezy.protocol;

import dev.beezy.stability.Internal;
import java.nio.charset.StandardCharsets;

/**
 * Field checks shared by the packets and the API value classes. Every failure is an
 * {@link IllegalArgumentException} naming the field: nothing is ever truncated silently.
 */
@Internal
public final class Validate {

    private Validate() {
    }

    /**
     * @param value the value
     * @param field the field name for the message
     * @param <T> the type
     * @return the value
     */
    public static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    /**
     * A string that fits {@code cap} UTF-8 bytes; null is rejected.
     *
     * @param value the value
     * @param cap the cap in bytes
     * @param field the field name
     * @return the value
     */
    public static String str(String value, int cap, String field) {
        notNull(value, field);
        int len = utf8Length(value);
        if (len > cap) {
            throw new IllegalArgumentException(field + " is " + len + " UTF-8 bytes, at most " + cap + " allowed");
        }
        return value;
    }

    /**
     * Like {@link #str} but empty is rejected too (the client drops a message with an empty id).
     *
     * @param value the value
     * @param cap the cap in bytes
     * @param field the field name
     * @return the value
     */
    public static String id(String value, int cap, String field) {
        str(value, cap, field);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return value;
    }

    /**
     * @param value the value
     * @param min the smallest allowed
     * @param max the largest allowed
     * @param field the field name
     * @return the value
     */
    public static int range(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " is " + value + ", must be " + min + ".." + max);
        }
        return value;
    }

    /**
     * @param value the value
     * @param min the smallest allowed
     * @param max the largest allowed
     * @param field the field name
     * @return the value
     */
    public static long range(long value, long min, long max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " is " + value + ", must be " + min + ".." + max);
        }
        return value;
    }

    /**
     * @param value the value
     * @param field the field name
     * @return the value, if finite
     */
    public static double finite(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(field + " must be a finite number, is " + value);
        }
        return value;
    }

    /**
     * A {@code duration} field: 0 (until removed) to 24 h.
     *
     * @param ms milliseconds
     * @param field the field name
     * @return the value
     */
    public static long duration(long ms, String field) {
        return range(ms, 0, Protocol.MAX_DURATION_MS, field);
    }

    /**
     * @param value a string
     * @return its length in UTF-8 bytes
     */
    public static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
