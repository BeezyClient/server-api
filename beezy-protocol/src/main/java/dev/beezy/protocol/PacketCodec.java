package dev.beezy.protocol;

import dev.beezy.stability.Stable;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Turns packets into plugin-message payloads and back: {@code u8 format, u8 kind, body}.
 *
 * <pre>{@code
 * byte[] payload = PacketCodec.encode(new NotificationPacket("Welcome", "Have fun!", 0, 5));
 * Packet p = PacketCodec.decode(payload);
 * }</pre>
 */
@Stable
public final class PacketCodec {

    private PacketCodec() {
    }

    /**
     * @param packet a packet
     * @return the complete payload
     * @throws IllegalArgumentException if it would exceed 32 768 bytes
     */
    public static byte[] encode(Packet packet) {
        Validate.notNull(packet, "packet");
        WireWriter out = new WireWriter();
        out.u8(Protocol.FORMAT).u8(packet.kind().id());
        packet.write(out);
        if (out.size() > Protocol.MAX_MESSAGE_BYTES) {
            throw new IllegalArgumentException(packet.kind() + " message is " + out.size()
                    + " bytes, at most " + Protocol.MAX_MESSAGE_BYTES + " allowed");
        }
        return out.toByteArray();
    }

    /**
     * Decodes any kind, in either direction. Bytes after a known kind's body are ignored, as the
     * protocol says (a later revision may append fields).
     *
     * @param payload a payload
     * @return the packet
     * @throws MalformedPacketException if it is not a valid format-1 message, or the kind is
     *     unknown ({@link MalformedPacketException#isUnknownKind()})
     */
    public static Packet decode(byte[] payload) throws MalformedPacketException {
        if (payload == null || payload.length < 2) {
            throw new MalformedPacketException("payload shorter than 2 bytes", false);
        }
        if (payload.length > Protocol.MAX_MESSAGE_BYTES) {
            throw new MalformedPacketException("payload is " + payload.length + " bytes", false);
        }
        int format = payload[0] & 0xFF;
        if (format != Protocol.FORMAT) {
            throw new MalformedPacketException("unknown format " + format, false);
        }
        PacketKind kind = PacketKind.byId(payload[1] & 0xFF);
        if (kind == null) {
            throw new MalformedPacketException("unknown kind " + (payload[1] & 0xFF), true);
        }
        ByteBuffer buf = ByteBuffer.wrap(payload, 2, payload.length - 2);
        try {
            return kind.read(new WireReader(buf));
        } catch (BufferUnderflowException e) {
            throw new MalformedPacketException(kind + ": cut short", false);
        } catch (IllegalArgumentException e) {
            throw new MalformedPacketException(kind + ": " + e.getMessage(), false);
        }
    }

    /**
     * Decodes a payload that must travel in {@code direction} (a server only accepts
     * {@link Direction#SERVERBOUND} kinds from a client).
     *
     * @param payload a payload
     * @param direction the expected direction
     * @return the packet
     * @throws MalformedPacketException if invalid, unknown, or of the other direction
     */
    public static Packet decode(byte[] payload, Direction direction) throws MalformedPacketException {
        Packet p = decode(payload);
        if (p.kind().direction() != direction) {
            throw new MalformedPacketException(p.kind() + " does not travel " + direction, false);
        }
        return p;
    }
}
