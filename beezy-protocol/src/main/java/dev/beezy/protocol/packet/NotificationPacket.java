package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;

/** Kind 20: a toast notification. */
@Stable
public final class NotificationPacket extends AbstractPacket {

    private final String title;
    private final String text;
    private final int level;
    private final int seconds;

    /**
     * @param title at most 32 bytes
     * @param text at most 160 bytes
     * @param level 0 info, 1 success, 2 warning, 3 error
     * @param seconds 1-30
     */
    public NotificationPacket(String title, String text, int level, int seconds) {
        super(PacketKind.NOTIFICATION);
        this.title = Validate.str(title, Protocol.MAX_NOTIFICATION_TITLE, "notification title");
        this.text = Validate.str(text, Protocol.MAX_NOTIFICATION_TEXT, "notification text");
        this.level = Validate.range(level, 0, 3, "notification level");
        this.seconds = Validate.range(seconds, Protocol.MIN_NOTIFICATION_SECONDS, Protocol.MAX_NOTIFICATION_SECONDS, "notification seconds");
    }

    /** @return the title */
    public String title() {
        return title;
    }

    /** @return the text */
    public String text() {
        return text;
    }

    /** @return the level */
    public int level() {
        return level;
    }

    /** @return the seconds */
    public int seconds() {
        return seconds;
    }

    @Override
    public void write(WireWriter out) {
        out.str(title).str(text).u8(level).u16(seconds);
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static NotificationPacket read(PacketKind kind, WireReader in) {
        return new NotificationPacket(in.str(), in.str(), in.u8(), in.u16());
    }
}
