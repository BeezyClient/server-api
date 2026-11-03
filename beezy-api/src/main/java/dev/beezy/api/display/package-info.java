/**
 * What a server can show: immutable value objects with builders
 * ({@link dev.beezy.api.display.Waypoint}, {@link dev.beezy.api.display.Hologram}, ...) and the
 * {@link dev.beezy.api.display.Handle}s that services return for them.
 *
 * <p>Values validate themselves against the protocol limits when built (string caps are UTF-8
 * bytes): bad input throws {@link java.lang.IllegalArgumentException}, nothing is truncated.
 * Text lines (holograms, nametags) are Adventure components; {@code §} strings are converted.
 */
@Stable
package dev.beezy.api.display;

import dev.beezy.stability.Stable;
