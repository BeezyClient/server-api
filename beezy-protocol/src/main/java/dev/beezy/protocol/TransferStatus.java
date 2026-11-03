package dev.beezy.protocol;

import dev.beezy.stability.Stable;

/** The status byte of a Transfer result (kind 3). */
@Stable
public enum TransferStatus {
    /** The client is leaving for the address (sent just before it leaves). */
    CONNECTING(0),
    /** The player switched transfers off. */
    REFUSED(1),
    /** The address is invalid, or the server asked for more than one transfer a minute. */
    INVALID_ADDRESS(2);

    private final int id;

    TransferStatus(int id) {
        this.id = id;
    }

    /** @return the wire value */
    public int id() {
        return id;
    }

    /**
     * @param id a wire value
     * @return the status, or null if the protocol does not list it
     */
    public static TransferStatus byId(int id) {
        for (TransferStatus s : values()) {
            if (s.id == id) {
                return s;
            }
        }
        return null;
    }
}
