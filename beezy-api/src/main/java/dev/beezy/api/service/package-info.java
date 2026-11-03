/**
 * Per-module services, reached from a {@link dev.beezy.api.BeezyUser} ({@code user.waypoints()},
 * {@code user.holograms()}, ...). Services for things with an id extend
 * {@link dev.beezy.api.service.ElementService} and return handles; the others set or clear one
 * value per user. Every method follows the server's {@link dev.beezy.api.DeliveryPolicy}.
 */
@Stable
package dev.beezy.api.service;

import dev.beezy.stability.Stable;
