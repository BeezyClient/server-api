package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.api.display.Nametag;
import dev.beezy.api.display.NametagHandle;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.ComponentLike;

/**
 * Nametags above other players, for this user only: 128 at once, keyed by the target's UUID.
 * {@link #remove(String)} (or {@link #reset(UUID)}) gives the vanilla nametag back.
 */
@Stable
@NonExtendable
public interface NametagService extends ElementService<Nametag, NametagHandle> {

    /**
     * @param target the player
     * @param lines 0-5 components, top first
     * @return the handle
     */
    default NametagHandle set(UUID target, List<? extends ComponentLike> lines) {
        return show(Nametag.of(target, lines));
    }

    /**
     * @param target the player
     * @param lines 0-5 components
     * @return the handle
     */
    default NametagHandle set(UUID target, ComponentLike... lines) {
        return show(Nametag.of(target, lines));
    }

    /**
     * @param target the player
     * @param lines 0-5 {@code §} strings or plain text
     * @return the handle
     */
    default NametagHandle set(UUID target, String... lines) {
        return show(Nametag.legacy(target, lines));
    }

    /**
     * @param target the player
     * @return the handle of a hidden nametag
     */
    default NametagHandle hide(UUID target) {
        return show(Nametag.hidden(target));
    }

    /**
     * @param target the player
     * @return the outcome of giving the vanilla nametag back
     */
    default SendResult reset(UUID target) {
        return remove(target.toString());
    }

    /**
     * @param target the player
     * @return the active handle
     */
    default Optional<NametagHandle> get(UUID target) {
        return get(target.toString());
    }
}
