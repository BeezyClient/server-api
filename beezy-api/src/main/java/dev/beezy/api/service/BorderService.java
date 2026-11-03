package dev.beezy.api.service;

import dev.beezy.api.display.Border;
import dev.beezy.api.display.BorderHandle;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;

/** Borders: 16 per user. */
@Stable
@NonExtendable
public interface BorderService extends ElementService<Border, BorderHandle> {
}
