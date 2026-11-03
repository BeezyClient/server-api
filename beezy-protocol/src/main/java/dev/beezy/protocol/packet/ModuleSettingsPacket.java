package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Kind 120: overrides module settings while the player stays (1-64 entries). */
@Stable
public final class ModuleSettingsPacket extends AbstractPacket {

    private final List<SettingEntry> entries;

    /** @param entries 1-64 entries */
    public ModuleSettingsPacket(List<SettingEntry> entries) {
        super(PacketKind.MODULE_SETTINGS);
        Validate.notNull(entries, "module settings");
        Validate.range(entries.size(), 1, Protocol.MAX_SETTINGS, "module setting count");
        for (SettingEntry e : entries) {
            Validate.notNull(e, "module setting");
        }
        this.entries = Collections.unmodifiableList(new ArrayList<SettingEntry>(entries));
    }

    /** @return the entries */
    public List<SettingEntry> entries() {
        return entries;
    }

    @Override
    public void write(WireWriter out) {
        out.u8(entries.size());
        for (SettingEntry e : entries) {
            out.str(e.module()).str(e.option()).u8(e.type());
            switch (e.type()) {
                case SettingEntry.TYPE_BOOL:
                    out.u8(((Boolean) e.value()) ? 1 : 0);
                    break;
                case SettingEntry.TYPE_INT:
                case SettingEntry.TYPE_COLOR:
                    out.i32((Integer) e.value());
                    break;
                case SettingEntry.TYPE_DECIMAL:
                    out.f64((Double) e.value());
                    break;
                default:
                    out.str((String) e.value());
            }
        }
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static ModuleSettingsPacket read(PacketKind kind, WireReader in) {
        int count = Validate.range(in.u8(), 1, Protocol.MAX_SETTINGS, "module setting count");
        List<SettingEntry> entries = new ArrayList<SettingEntry>(count);
        for (int i = 0; i < count; i++) {
            String module = in.str();
            String option = in.str();
            int type = in.u8();
            switch (type) {
                case SettingEntry.TYPE_BOOL:
                    entries.add(SettingEntry.bool(module, option, Validate.range(in.u8(), 0, 1, "setting bool") == 1));
                    break;
                case SettingEntry.TYPE_INT:
                    entries.add(SettingEntry.integer(module, option, in.i32()));
                    break;
                case SettingEntry.TYPE_DECIMAL:
                    entries.add(SettingEntry.decimal(module, option, in.f64()));
                    break;
                case SettingEntry.TYPE_TEXT:
                    entries.add(SettingEntry.text(module, option, in.str()));
                    break;
                case SettingEntry.TYPE_COLOR:
                    entries.add(SettingEntry.color(module, option, in.i32()));
                    break;
                default:
                    throw new IllegalArgumentException("setting type " + type + " is not listed");
            }
        }
        return new ModuleSettingsPacket(entries);
    }
}
