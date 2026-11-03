package dev.beezy.api.service;

import dev.beezy.api.display.Timer;
import dev.beezy.api.display.TimerHandle;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;

/** Timers: 8 per user. */
@Stable
@NonExtendable
public interface TimerService extends ElementService<Timer, TimerHandle> {
}
