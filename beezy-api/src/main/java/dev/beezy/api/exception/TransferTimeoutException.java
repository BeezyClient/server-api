package dev.beezy.api.exception;

import dev.beezy.stability.Stable;
import java.time.Duration;

/** The client did not answer a transfer in time (an old client, or the message was lost). */
@Stable
public class TransferTimeoutException extends BeezyException {

    private static final long serialVersionUID = 1L;

    /** @param after how long it waited */
    public TransferTimeoutException(Duration after) {
        super("no transfer result after " + after.getSeconds() + " s");
    }
}
