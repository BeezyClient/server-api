package dev.beezy.api.display;

import dev.beezy.stability.Stable;

/** Something shown under an id; showing another element with the same id replaces it. */
@Stable
public interface Element {

    /** @return the id, unique per kind and player */
    String id();
}
