package dev.beezy.api.settings;

import dev.beezy.api.value.Color;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;

/**
 * A module setting: Beezy module id (such as {@code zoom}), option id (empty for the module's
 * on/off switch) and value type. Two keys are equal when module and option are.
 *
 * <pre>{@code
 * static final SettingKey<Integer> MAX_ZOOM = SettingKey.integer("zoom", "max_zoom");
 * static final SettingKey<Boolean> FREELOOK = SettingKey.moduleSwitch("freelook");
 * }</pre>
 *
 * @param <T> the value type
 */
@Stable
public final class SettingKey<T> {

    private final String module;
    private final String option;
    private final SettingType<T> type;

    private SettingKey(String module, String option, SettingType<T> type) {
        this.module = Validate.id(module, Protocol.MAX_MODULE, "setting module");
        this.option = Validate.str(option, Protocol.MAX_OPTION, "setting option");
        this.type = Validate.notNull(type, "setting type");
        if (option.isEmpty() && type != SettingType.BOOL) {
            throw new IllegalArgumentException("a module switch (empty option) is a bool");
        }
    }

    /**
     * @param module module id
     * @param option option id
     * @param type value type
     * @param <T> the type
     * @return the key
     */
    public static <T> SettingKey<T> of(String module, String option, SettingType<T> type) {
        return new SettingKey<T>(module, option, type);
    }

    /**
     * @param module module id
     * @return the module's on/off switch
     */
    public static SettingKey<Boolean> moduleSwitch(String module) {
        return of(module, "", SettingType.BOOL);
    }

    /**
     * @param module module id
     * @param option option id
     * @return a bool key
     */
    public static SettingKey<Boolean> bool(String module, String option) {
        return of(module, option, SettingType.BOOL);
    }

    /**
     * @param module module id
     * @param option option id
     * @return an int key
     */
    public static SettingKey<Integer> integer(String module, String option) {
        return of(module, option, SettingType.INT);
    }

    /**
     * @param module module id
     * @param option option id
     * @return a decimal key
     */
    public static SettingKey<Double> decimal(String module, String option) {
        return of(module, option, SettingType.DECIMAL);
    }

    /**
     * @param module module id
     * @param option option id
     * @return a text key
     */
    public static SettingKey<String> text(String module, String option) {
        return of(module, option, SettingType.TEXT);
    }

    /**
     * @param module module id
     * @param option option id
     * @return a colour key
     */
    public static SettingKey<Color> color(String module, String option) {
        return of(module, option, SettingType.COLOR);
    }

    /** @return the module id */
    public String module() {
        return module;
    }

    /** @return the option id, empty for the switch */
    public String option() {
        return option;
    }

    /** @return the value type */
    public SettingType<T> type() {
        return type;
    }

    /** @return whether this is the module's on/off switch */
    public boolean isModuleSwitch() {
        return option.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SettingKey && ((SettingKey<?>) o).module.equals(module) && ((SettingKey<?>) o).option.equals(option);
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
