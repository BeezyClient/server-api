/**
 * Platform-neutral events on {@link dev.beezy.api.event.EventBus}
 * ({@code BeezyClientApi.get().events()}). Platforms also fire their own (Bukkit:
 * {@code BeezyHelloEvent}, {@code BeezyTransferResultEvent}). Handlers run on the thread that
 * received the client's message (Bukkit: the main thread; Folia: the player's region thread).
 */
@Stable
package dev.beezy.api.event;

import dev.beezy.stability.Stable;
