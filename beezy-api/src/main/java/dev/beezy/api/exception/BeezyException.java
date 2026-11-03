package dev.beezy.api.exception;

import dev.beezy.stability.Stable;

/** Base of every Beezy exception. */
@Stable
public class BeezyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param message what went wrong */
    public BeezyException(String message) {
        super(message);
    }
}
