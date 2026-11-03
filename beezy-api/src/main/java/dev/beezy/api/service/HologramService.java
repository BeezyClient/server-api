package dev.beezy.api.service;

import dev.beezy.api.display.Hologram;
import dev.beezy.api.display.HologramHandle;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;

/** Holograms: 64 per user, 1-8 component lines each. */
@Stable
@NonExtendable
public interface HologramService extends ElementService<Hologram, HologramHandle> {
}
