package dev.beezy.api.display;

import dev.beezy.api.value.Texts;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;

/**
 * The nametag above a player, for one viewer only: 0-5 component lines, top first; no lines
 * hides it. Its id is the target's UUID. 128 per viewer.
 */
@Stable
public final class Nametag implements Element {

    private final UUID target;
    private final List<Component> lines;

    private Nametag(UUID target, List<Component> lines) {
        this.target = Validate.notNull(target, "nametag target");
        Validate.range(lines.size(), 0, Protocol.MAX_NAMETAG_LINES, "nametag line count");
        this.lines = lines;
    }

    /**
     * @param target the player
     * @param lines 0-5 components
     * @return the nametag
     */
    public static Nametag of(UUID target, List<? extends ComponentLike> lines) {
        return new Nametag(target, Texts.components(lines));
    }

    /**
     * @param target the player
     * @param lines 0-5 components
     * @return the nametag
     */
    public static Nametag of(UUID target, ComponentLike... lines) {
        return of(target, Arrays.asList(lines));
    }

    /**
     * @param target the player
     * @param lines 0-5 {@code §} strings or plain text
     * @return the nametag
     */
    public static Nametag legacy(UUID target, String... lines) {
        return new Nametag(target, Texts.legacy(lines));
    }

    /**
     * @param target the player
     * @return a nametag with no lines (hidden)
     */
    public static Nametag hidden(UUID target) {
        return of(target, new ComponentLike[0]);
    }

    @Override
    public String id() {
        return target.toString();
    }

    /** @return the player whose nametag this is */
    public UUID target() {
        return target;
    }

    /** @return the lines, empty when hidden */
    public List<Component> lines() {
        return lines;
    }
}
