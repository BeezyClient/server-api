package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 1, client to server: Minecraft version, Beezy version, feature bits. */
@Stable
public final class HelloPacket extends AbstractPacket {

    private final String minecraftVersion;
    private final String beezyVersion;
    private final int features;

    /**
     * @param minecraftVersion at most 32 bytes
     * @param beezyVersion at most 32 bytes
     * @param features the {@code u32} feature bits
     */
    public HelloPacket(String minecraftVersion, String beezyVersion, int features) {
        super(PacketKind.HELLO);
        this.minecraftVersion = Validate.str(minecraftVersion, Protocol.MAX_VERSION, "minecraft version");
        this.beezyVersion = Validate.str(beezyVersion, Protocol.MAX_VERSION, "beezy version");
        this.features = features;
    }

    /** @return the Minecraft version */
    public String minecraftVersion() {
        return minecraftVersion;
    }

    /** @return the Beezy version */
    public String beezyVersion() {
        return beezyVersion;
    }

    /** @return the raw feature bits, unknown bits kept */
    public int features() {
        return features;
    }

    @Override
    public void write(WireWriter out) {
        out.str(minecraftVersion).str(beezyVersion).i32(features);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static HelloPacket read(PacketKind kind, WireReader in) {
        return new HelloPacket(in.str(), in.str(), in.i32());
    }
}
