package dev.beezy.api.value;

import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Text helpers. Legacy strings use {@code §} codes ({@code "§6Shop"}); a string without codes is
 * plain text. MiniMessage is not used: parse it yourself and pass the component.
 */
@Stable
public final class Texts {

    private Texts() {
    }

    /**
     * @param legacy a string with {@code §} colour and format codes, or plain text
     * @return the component
     */
    public static Component legacy(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(Validate.notNull(legacy, "text"));
    }

    /**
     * @param lines components
     * @return the components, as an unmodifiable list
     */
    public static List<Component> components(List<? extends ComponentLike> lines) {
        Validate.notNull(lines, "lines");
        List<Component> out = new ArrayList<Component>(lines.size());
        for (ComponentLike line : lines) {
            out.add(Validate.notNull(line, "line").asComponent());
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * @param lines legacy strings
     * @return the components, as an unmodifiable list
     */
    public static List<Component> legacy(String... lines) {
        List<Component> out = new ArrayList<Component>(lines.length);
        for (String line : lines) {
            out.add(legacy(line));
        }
        return Collections.unmodifiableList(out);
    }
}
