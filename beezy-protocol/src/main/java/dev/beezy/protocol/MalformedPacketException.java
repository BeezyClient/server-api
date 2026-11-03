package dev.beezy.protocol;

import dev.beezy.stability.Stable;

/**
 * A payload that is not a valid format-1 message: cut short, invalid UTF-8, a field over its
 * cap or out of range, an unknown format or kind. The receiver drops such a message whole.
 */
@Stable
public final class MalformedPacketException extends Exception {

    private static final long serialVersionUID = 1L;

    private final boolean unknownKind;

    /**
     * @param message what is wrong
     * @param unknownKind true when the only problem is a kind this library does not know
     */
    public MalformedPacketException(String message, boolean unknownKind) {
        super(message);
        this.unknownKind = unknownKind;
    }

    /**
     * True when the payload carries a kind this library does not know. The protocol says such
     * messages are ignored (a newer peer), not an error.
     *
     * @return whether the kind is unknown
     */
    public boolean isUnknownKind() {
        return unknownKind;
    }
}
