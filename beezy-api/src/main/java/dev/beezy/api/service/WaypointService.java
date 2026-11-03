package dev.beezy.api.service;

import dev.beezy.api.display.Waypoint;
import dev.beezy.api.display.WaypointHandle;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;

/** Waypoints: 64 per user. */
@Stable
@NonExtendable
public interface WaypointService extends ElementService<Waypoint, WaypointHandle> {
}
