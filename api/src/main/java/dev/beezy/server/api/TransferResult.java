package dev.beezy.server.api;

/** The client's answer to a Transfer (kind 3): the request's id and what happened. */
public final class TransferResult implements ClientMessage {

    public enum Status {
        /** The client is leaving this server for the address (sent before it leaves). */
        CONNECTING,
        /** The player switched transfers off. */
        REFUSED,
        /** The address is not valid, or the server asked for more than one transfer a minute. */
        INVALID_ADDRESS;

        /** Wire value: 0 connecting, 1 refused, 2 invalid address. */
        public int id() {
            return ordinal();
        }

        /** The status for a wire value, or null if there is none. */
        public static Status byId(int id) {
            Status[] all = values();
            return id >= 0 && id < all.length ? all[id] : null;
        }
    }

    private final String id;
    private final Status status;
    private final String address;

    public TransferResult(String id, Status status) {
        this(id, status, null);
    }

    TransferResult(String id, Status status, String address) {
        this.id = Check.id(id, BeezyProtocol.MAX_ID, "transfer id");
        this.status = Check.notNull(status, "transfer status");
        this.address = address;
    }

    /** The id of the Transfer this answers. */
    public String id() {
        return id;
    }

    public Status status() {
        return status;
    }

    /**
     * The address this server asked the player to go to, as {@link BeezyApiCore} remembered it;
     * null on a result decoded by {@link BeezyCodec} alone.
     */
    public String address() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TransferResult && id.equals(((TransferResult) o).id) && status == ((TransferResult) o).status;
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + status.hashCode();
    }

    @Override
    public String toString() {
        return "TransferResult{id=" + id + ", status=" + status + (address == null ? "" : ", address=" + address) + "}";
    }
}
