package dev.beezy.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/**
 * Component to the protocol's {@code component} JSON. Uses whatever Adventure is on the class
 * path: the server's own on Paper, the copy shaded into the Beezy plugin elsewhere. Only
 * methods present since Adventure 4.0 are called, so old Paper builds work too.
 */
final class ComponentJson {

    private ComponentJson() {
    }

    static String gson(Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }
}
