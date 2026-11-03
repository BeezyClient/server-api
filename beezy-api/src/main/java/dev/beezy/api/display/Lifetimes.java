package dev.beezy.api.display;

import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import java.time.Duration;

/** Checks a {@code duration} field: null or zero is "until removed", else 1 ms to 24 h. */
final class Lifetimes {

    private Lifetimes() {
    }

    static Duration check(Duration d, String field) {
        if (d == null || d.isZero()) {
            return null;
        }
        if (d.isNegative()) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        if (d.compareTo(Duration.ofMillis(Protocol.MAX_DURATION_MS)) > 0) {
            throw new IllegalArgumentException(field + " is " + d + ", at most 24 hours allowed");
        }
        return d;
    }

    static String id(String id, String field) {
        return Validate.id(id, Protocol.MAX_ID, field);
    }
}
