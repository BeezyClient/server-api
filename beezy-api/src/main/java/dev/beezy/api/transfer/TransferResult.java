package dev.beezy.api.transfer;

import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;

/**
 * The client's answer to a transfer. It answers once, before leaving. If the new server cannot
 * be reached the player ends up on their multiplayer screen, which this server is not told.
 */
@Stable
public final class TransferResult {

    /** What happened. */
    @Stable
    public enum Status {
        /** The client is leaving for the address. */
        CONNECTING,
        /** The player switched transfers off: fall back to your proxy's own command. */
        REFUSED,
        /** The address is invalid, or more than one transfer was asked for in a minute. */
        INVALID_ADDRESS
    }

    private final String requestId;
    private final String address;
    private final Status status;

    /**
     * @param requestId the transfer's wire id
     * @param address the address asked for
     * @param status what happened
     */
    public TransferResult(String requestId, String address, Status status) {
        this.requestId = Validate.notNull(requestId, "request id");
        this.address = Validate.notNull(address, "address");
        this.status = Validate.notNull(status, "status");
    }

    /** @return the id sent with the transfer */
    public String requestId() {
        return requestId;
    }

    /** @return the address asked for */
    public String address() {
        return address;
    }

    /** @return what happened */
    public Status status() {
        return status;
    }

    /** @return whether the player is on their way */
    public boolean isConnecting() {
        return status == Status.CONNECTING;
    }

    @Override
    public String toString() {
        return "TransferResult{" + requestId + " -> " + address + ": " + status + "}";
    }
}
