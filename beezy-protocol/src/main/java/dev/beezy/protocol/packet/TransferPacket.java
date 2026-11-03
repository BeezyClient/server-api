package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 170: sends the player to another server. */
@Stable
public final class TransferPacket extends AbstractPacket {

    private final String id;
    private final String address;

    /**
     * @param id 1-32 bytes, echoed in the Transfer result
     * @param address {@code host} or {@code host:port}, 1-255 bytes
     */
    public TransferPacket(String id, String address) {
        super(PacketKind.TRANSFER);
        this.id = Validate.id(id, Protocol.MAX_ID, "transfer id");
        this.address = Validate.id(address, Protocol.MAX_ADDRESS, "transfer address");
    }

    /** @return the id */
    public String id() {
        return id;
    }

    /** @return the address */
    public String address() {
        return address;
    }

    @Override
    public void write(WireWriter out) {
        out.str(id).str(address);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static TransferPacket read(PacketKind kind, WireReader in) {
        return new TransferPacket(in.str(), in.str());
    }
}
