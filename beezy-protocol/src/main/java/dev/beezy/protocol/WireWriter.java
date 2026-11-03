package dev.beezy.protocol;

import dev.beezy.stability.Internal;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Big-endian writer of the protocol's field types. Used by the packets' {@code write}. */
@Internal
public final class WireWriter {

    private final ByteArrayOutputStream buf = new ByteArrayOutputStream(64);

    WireWriter() {
    }

    /**
     * @param v a byte, 0-255
     * @return this
     */
    public WireWriter u8(int v) {
        buf.write(v & 0xFF);
        return this;
    }

    /**
     * @param v 0-65535
     * @return this
     */
    public WireWriter u16(int v) {
        buf.write((v >>> 8) & 0xFF);
        buf.write(v & 0xFF);
        return this;
    }

    /**
     * @param v any int
     * @return this
     */
    public WireWriter i32(int v) {
        buf.write((v >>> 24) & 0xFF);
        buf.write((v >>> 16) & 0xFF);
        buf.write((v >>> 8) & 0xFF);
        buf.write(v & 0xFF);
        return this;
    }

    /**
     * @param v 0 to 2^32-1
     * @return this
     */
    public WireWriter u32(long v) {
        return i32((int) Validate.range(v, 0, 0xFFFFFFFFL, "u32"));
    }

    /**
     * @param v any long
     * @return this
     */
    public WireWriter i64(long v) {
        i32((int) (v >>> 32));
        return i32((int) v);
    }

    /**
     * @param v a float
     * @return this
     */
    public WireWriter f32(float v) {
        return i32(Float.floatToIntBits(v));
    }

    /**
     * @param v a double
     * @return this
     */
    public WireWriter f64(double v) {
        return i64(Double.doubleToLongBits(v));
    }

    /**
     * @param u a UUID
     * @return this
     */
    public WireWriter uuid(UUID u) {
        return i64(u.getMostSignificantBits()).i64(u.getLeastSignificantBits());
    }

    /**
     * @param s a string of at most 255 UTF-8 bytes (callers check their own cap)
     * @return this
     */
    public WireWriter str(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        u8(Validate.range(bytes.length, 0, 255, "string length"));
        buf.write(bytes, 0, bytes.length);
        return this;
    }

    /**
     * @param text a component
     * @return this
     */
    public WireWriter component(JsonText text) {
        byte[] bytes = text.json().getBytes(StandardCharsets.UTF_8);
        u16(bytes.length);
        buf.write(bytes, 0, bytes.length);
        return this;
    }

    byte[] toByteArray() {
        return buf.toByteArray();
    }

    int size() {
        return buf.size();
    }
}
