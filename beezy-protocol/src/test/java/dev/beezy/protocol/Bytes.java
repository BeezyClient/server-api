package dev.beezy.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Builds expected payloads by hand, independently of WireWriter. */
final class Bytes {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    static Bytes msg(int kind) {
        return new Bytes().u8(1).u8(kind);
    }

    Bytes u8(int v) {
        out.write(v);
        return this;
    }

    Bytes raw(int... bytes) {
        for (int b : bytes) {
            out.write(b);
        }
        return this;
    }

    Bytes u16(int v) {
        return raw(v >> 8 & 0xFF, v & 0xFF);
    }

    Bytes i32(int v) {
        byte[] b = ByteBuffer.allocate(4).putInt(v).array();
        out.write(b, 0, 4);
        return this;
    }

    Bytes f32(float v) {
        byte[] b = ByteBuffer.allocate(4).putFloat(v).array();
        out.write(b, 0, 4);
        return this;
    }

    Bytes f64(double v) {
        byte[] b = ByteBuffer.allocate(8).putDouble(v).array();
        out.write(b, 0, 8);
        return this;
    }

    Bytes uuid(UUID u) {
        byte[] b = ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();
        out.write(b, 0, 16);
        return this;
    }

    Bytes str(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.write(b.length);
        out.write(b, 0, b.length);
        return this;
    }

    Bytes component(String json) {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        u16(b.length);
        out.write(b, 0, b.length);
        return this;
    }

    byte[] done() {
        return out.toByteArray();
    }
}
