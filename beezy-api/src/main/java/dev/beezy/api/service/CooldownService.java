package dev.beezy.api.service;

import dev.beezy.api.display.Cooldown;
import dev.beezy.api.display.CooldownHandle;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;

/** Cooldown bars: 16 per user; each one ends (and its handle turns inactive) when its time is up. */
@Stable
@NonExtendable
public interface CooldownService extends ElementService<Cooldown, CooldownHandle> {
}
