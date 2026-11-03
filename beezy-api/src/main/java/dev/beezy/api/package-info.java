/**
 * The Beezy server API: what a server plugin uses to show things to players running Beezy
 * Client.
 *
 * <p>Start at {@link dev.beezy.api.BeezyClientApi} (from {@code BeezyClientApi.get()} or the
 * platform's service registry). Each Beezy player is a {@link dev.beezy.api.BeezyUser}; its
 * per-module services ({@code user.waypoints()}, {@code user.holograms()}, ...) return handles
 * for what they show, which can be updated or removed later and expire by themselves when
 * given a duration. Several users at once are a {@link dev.beezy.api.BeezyAudience}.
 *
 * <pre>{@code
 * BeezyClientApi beezy = BeezyClientApi.get();
 * beezy.user(player.getUniqueId()).ifPresent(user -> {
 *     HologramHandle shop = user.holograms().show(Hologram.builder("shop")
 *             .at(Position.of(10.5, 72, -3.5)).world(WorldKey.OVERWORLD)
 *             .line(Component.text("Shop", NamedTextColor.GOLD)).line("§7Right-click to trade")
 *             .duration(Duration.ofMinutes(10)).build());
 *     user.transfer("lobby.example.net").thenAccept(r -> getLogger().info("transfer: " + r.status()));
 * });
 * }</pre>
 *
 * <p>Everything here is thread-safe. Types marked {@link dev.beezy.stability.NonExtendable}
 * are implemented by Beezy, not by plugins.
 */
@Stable
package dev.beezy.api;

import dev.beezy.stability.Stable;
