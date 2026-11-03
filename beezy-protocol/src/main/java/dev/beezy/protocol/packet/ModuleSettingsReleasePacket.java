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

/** Kind 121: gives settings back to the player; no keys releases all. */
@Stable
public final class ModuleSettingsReleasePacket extends AbstractPacket {

    /** A module and option (empty option: the module's switch). */
    @Stable
    public static final class Key {
        private final String module;
        private final String option;

        /**
         * @param module module id
         * @param option option id, empty for the switch
         */
        public Key(String module, String option) {
            this.module = SettingEntry.validModule(module);
            this.option = Validate.str(option, Protocol.MAX_OPTION, "setting option");
        }

        /** @return the module */
        public String module() {
            return module;
        }

        /** @return the option */
        public String option() {
            return option;
        }
    }

    private final List<Key> keys;

    /** @param keys 0-64 keys; empty releases every override */
    public ModuleSettingsReleasePacket(List<Key> keys) {
        super(PacketKind.MODULE_SETTINGS_RELEASE);
        Validate.notNull(keys, "setting keys");
        Validate.range(keys.size(), 0, Protocol.MAX_SETTINGS, "setting key count");
        for (Key k : keys) {
            Validate.notNull(k, "setting key");
        }
        this.keys = Collections.unmodifiableList(new ArrayList<Key>(keys));
    }

    /** @return a release of every override (count 0) */
    public static ModuleSettingsReleasePacket all() {
        return new ModuleSettingsReleasePacket(Collections.<Key>emptyList());
    }

    /** @return the keys, empty for all */
    public List<Key> keys() {
        return keys;
    }

    @Override
    public void write(WireWriter out) {
        out.u8(keys.size());
        for (Key k : keys) {
            out.str(k.module).str(k.option);
        }
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static ModuleSettingsReleasePacket read(PacketKind kind, WireReader in) {
        int count = Validate.range(in.u8(), 0, Protocol.MAX_SETTINGS, "setting key count");
        List<Key> keys = new ArrayList<Key>(count);
        for (int i = 0; i < count; i++) {
            keys.add(new Key(in.str(), in.str()));
        }
        return new ModuleSettingsReleasePacket(keys);
    }
}
