package dev.beezy.protocol.packet;

import dev.beezy.protocol.Packet;
import dev.beezy.protocol.PacketCodec;
import dev.beezy.protocol.PacketKind;
import dev.beezy.stability.Internal;
import java.util.Arrays;

/** Base of every packet: kind, and equality by encoded bytes. */
@Internal
public abstract class AbstractPacket implements Packet {

    private final PacketKind kind;

    AbstractPacket(PacketKind kind) {
        this.kind = kind;
    }

    @Override
    public final PacketKind kind() {
        return kind;
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof Packet && ((Packet) o).kind() == kind
                && Arrays.equals(PacketCodec.encode(this), PacketCodec.encode((Packet) o));
    }

    @Override
    public final int hashCode() {
        return Arrays.hashCode(PacketCodec.encode(this));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + kind + "}";
    }
}
