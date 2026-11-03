package dev.beezy.protocol;

import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;

/**
 * One {@code beezy:server} message. Packets are immutable and validated when built: a packet
 * that exists can always be encoded. Encode and decode with {@link PacketCodec}.
 */
@Stable
@NonExtendable
public interface Packet {

    /** @return what kind of message this is */
    PacketKind kind();

    /**
     * Writes the body (everything after the kind byte).
     *
     * @param out the writer
     */
    void write(WireWriter out);
}
