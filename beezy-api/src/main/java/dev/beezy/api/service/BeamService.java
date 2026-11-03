package dev.beezy.api.service;

import dev.beezy.api.display.Beam;
import dev.beezy.api.display.BeamHandle;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;

/** Beams: 32 per user. */
@Stable
@NonExtendable
public interface BeamService extends ElementService<Beam, BeamHandle> {
}
