package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 40: adds or replaces a timer. */
@Stable
public final class TimerSetPacket extends AbstractPacket {

    /** Mode 0: counts down from {@code startMs}. */
    public static final int MODE_COUNTDOWN = 0;
    /** Mode 1: counts up from {@code startMs}. */
    public static final int MODE_STOPWATCH = 1;

    private final String id;
    private final String label;
    private final int mode;
    private final long startMs;
    private final boolean running;

    /**
     * @param id 1-32 bytes
     * @param label at most 32 bytes
     * @param mode {@link #MODE_COUNTDOWN} or {@link #MODE_STOPWATCH}
     * @param startMs countdown: time left; stopwatch: time elapsed (u32)
     * @param running false for paused
     */
    public TimerSetPacket(String id, String label, int mode, long startMs, boolean running) {
        super(PacketKind.TIMER_SET);
        this.id = Validate.id(id, Protocol.MAX_ID, "timer id");
        this.label = Validate.str(label, Protocol.MAX_LABEL, "timer label");
        this.mode = Validate.range(mode, MODE_COUNTDOWN, MODE_STOPWATCH, "timer mode");
        this.startMs = Validate.range(startMs, 0, 0xFFFFFFFFL, "timer start ms");
        this.running = running;
    }

    /** @return the id */
    public String id() {
        return id;
    }

    /** @return the label */
    public String label() {
        return label;
    }

    /** @return the mode */
    public int mode() {
        return mode;
    }

    /** @return the start value */
    public long startMs() {
        return startMs;
    }

    /** @return whether it runs */
    public boolean running() {
        return running;
    }

    @Override
    public void write(WireWriter out) {
        out.str(id).str(label).u8(mode).u32(startMs).u8(running ? 1 : 0);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static TimerSetPacket read(PacketKind kind, WireReader in) {
        String id = in.str();
        String label = in.str();
        int mode = in.u8();
        long start = in.u32();
        int running = Validate.range(in.u8(), 0, 1, "timer running");
        return new TimerSetPacket(id, label, mode, start, running == 1);
    }
}
