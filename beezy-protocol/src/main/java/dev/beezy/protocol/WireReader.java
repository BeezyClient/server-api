package dev.beezy.protocol;

import dev.beezy.stability.Internal;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Big-endian reader of the protocol's field types. Reading past the end throws
 * {@link BufferUnderflowException}; invalid UTF-8 throws {@link IllegalArgumentException}. The
 * codec turns both into {@link MalformedPacketException}.
 */
@Internal
public final class WireReader {

    private final ByteBuffer in;

    WireReader(ByteBuffer in) {
        this.in = in;
    }

    /** @return a u8 */
    public int u8() {
        return in.get() & 0xFF;
    }

    /** @return a u16 */
    public int u16() {
        return in.getShort() & 0xFFFF;
    }

    /** @return an i32 */
    public int i32() {
        return in.getInt();
    }

    /** @return a u32 */
    public long u32() {
        return in.getInt() & 0xFFFFFFFFL;
    }

    /** @return an f32 */
    public float f32() {
        return in.getFloat();
    }

    /** @return an f64 */
    public double f64() {
        return in.getDouble();
    }

    /** @return a uuid */
    public UUID uuid() {
        return new UUID(in.getLong(), in.getLong());
    }

    /** @return a u8-length string */
    public String str() {
        return utf8(u8());
    }

    /** @return a u16-length component */
    public JsonText component() {
        return JsonText.of(utf8(u16()));
    }

    private String utf8(int len) {
        if (len > in.remaining()) {
            throw new BufferUnderflowException();
        }
        byte[] bytes = new byte[len];
        in.get(bytes);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("invalid UTF-8");
        }
    }
}
