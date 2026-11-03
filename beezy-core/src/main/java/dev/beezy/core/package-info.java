/**
 * The implementation of {@code beezy-api}: {@link dev.beezy.core.BeezyCore} keeps a session per
 * Beezy player, tracks what was shown (so handles can update, remove, expire, re-send), gates
 * every message on the player's features, enforces the client's limits under the configured
 * {@link dev.beezy.api.DeliveryPolicy}, and talks to the platform only through
 * {@link dev.beezy.core.spi}.
 *
 * <p>Not part of the API: platform plugins use it, other plugins should not.
 */
@Internal
package dev.beezy.core;

import dev.beezy.stability.Internal;
