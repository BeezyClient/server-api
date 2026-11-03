package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.TransferStatus;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 3, client to server: the answer to a Transfer. */
@Stable
public final class TransferResultPacket extends AbstractPacket {

    private final String id;
    private final TransferStatus status;

    /**
     * @param id the Transfer's id
     * @param status what happened
     */
    public TransferResultPacket(String id, TransferStatus status) {
        super(PacketKind.TRANSFER_RESULT);
        this.id = Validate.id(id, Protocol.MAX_ID, "transfer id");
        this.status = Validate.notNull(status, "transfer status");
    }

    /** @return the id of the Transfer this answers */
    public String id() {
        return id;
    }

    /** @return the status */
    public TransferStatus status() {
        return status;
    }

    @Override
    public void write(WireWriter out) {
        out.str(id).u8(status.id());
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static TransferResultPacket read(PacketKind kind, WireReader in) {
        String id = in.str();
        int s = in.u8();
        TransferStatus status = TransferStatus.byId(s);
        if (status == null) {
            throw new IllegalArgumentException("transfer status " + s + " is not listed");
        }
        return new TransferResultPacket(id, status);
    }
}
