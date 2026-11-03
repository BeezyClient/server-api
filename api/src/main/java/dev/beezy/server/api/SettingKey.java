package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_MODULE;
import static dev.beezy.server.api.BeezyProtocol.MAX_OPTION;

/**
 * A Beezy module setting: a module id such as {@code zoom} and an option id of that module, or
 * the empty option for the module's on/off switch.
 */
public final class SettingKey {

    private final String module;
    private final String option;

    /**
     * @param module module id (≤ 32 bytes, not empty)
     * @param option option id (≤ 32 bytes); empty or null for the module's on/off switch
     */
    public SettingKey(String module, String option) {
        this.module = Check.id(module, MAX_MODULE, "setting module");
        this.option = Check.optStr(option, MAX_OPTION, "setting option");
    }

    /** The module's on/off switch. */
    public static SettingKey of(String module) {
        return new SettingKey(module, "");
    }

    public static SettingKey of(String module, String option) {
        return new SettingKey(module, option);
    }

    public String module() {
        return module;
    }

    /** Empty for the on/off switch. */
    public String option() {
        return option;
    }

    public boolean isSwitch() {
        return option.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SettingKey)) {
            return false;
        }
        SettingKey k = (SettingKey) o;
        return module.equals(k.module) && option.equals(k.option);
    }

    @Override
    public int hashCode() {
        return module.hashCode() * 31 + option.hashCode();
    }

    @Override
    public String toString() {
        return option.isEmpty() ? module : module + "." + option;
    }
}
