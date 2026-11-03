package dev.beezy.server.api;

import java.nio.charset.StandardCharsets;

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
}
