package dev.beezy.protocol.packet;

import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;

/**
 * One module setting override: module, option (empty for the module's on/off switch), type and
 * value. Type 0 bool, 1 int ({@code i32}), 2 decimal ({@code f64}), 3 text ({@code str}, at most
 * 64 bytes), 4 colour ({@code color}, an ARGB int).
 */
@Stable
public final class SettingEntry {

    /** Type 0. */
    public static final int TYPE_BOOL = 0;
    /** Type 1. */
    public static final int TYPE_INT = 1;
    /** Type 2. */
    public static final int TYPE_DECIMAL = 2;
    /** Type 3. */
    public static final int TYPE_TEXT = 3;
    /** Type 4. */
    public static final int TYPE_COLOR = 4;

    private final String module;
    private final String option;
    private final int type;
    private final Object value;

    private SettingEntry(String module, String option, int type, Object value) {
        this.module = validModule(module);
        this.option = Validate.str(option, Protocol.MAX_OPTION, "setting option");
        this.type = type;
        this.value = value;
    }

    static String validModule(String module) {
        return Validate.id(module, Protocol.MAX_MODULE, "setting module");
    }

    /**
     * @param module module id
     * @param option option id, empty for the switch
     * @param value the value
     * @return the entry
     */
    public static SettingEntry bool(String module, String option, boolean value) {
        return new SettingEntry(module, option, TYPE_BOOL, value);
    }

    /**
     * @param module module id
     * @param option option id
     * @param value the value
     * @return the entry
     */
    public static SettingEntry integer(String module, String option, int value) {
        return new SettingEntry(module, option, TYPE_INT, value);
    }

    /**
     * @param module module id
     * @param option option id
     * @param value finite
     * @return the entry
     */
    public static SettingEntry decimal(String module, String option, double value) {
        return new SettingEntry(module, option, TYPE_DECIMAL, Validate.finite(value, "setting value"));
    }

    /**
     * @param module module id
     * @param option option id
     * @param value at most 64 bytes
     * @return the entry
     */
    public static SettingEntry text(String module, String option, String value) {
        return new SettingEntry(module, option, TYPE_TEXT, Validate.str(value, Protocol.MAX_SETTING_TEXT, "setting text"));
    }

    /**
     * @param module module id
     * @param option option id
     * @param argb colour
     * @return the entry
     */
    public static SettingEntry color(String module, String option, int argb) {
        return new SettingEntry(module, option, TYPE_COLOR, argb);
    }

    /** @return the module */
    public String module() {
        return module;
    }

    /** @return the option, empty for the switch */
    public String option() {
        return option;
    }

    /** @return the type byte */
    public int type() {
        return type;
    }

    /** @return a Boolean, Integer, Double, String or Integer (ARGB), by type */
    public Object value() {
        return value;
    }
}
