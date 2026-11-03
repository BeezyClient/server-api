/**
 * The {@code beezy:server} wire format, format 1 (PROTOCOL.md): the kind registry
 * ({@link dev.beezy.protocol.PacketKind}), the codec ({@link dev.beezy.protocol.PacketCodec}),
 * the field limits ({@link dev.beezy.protocol.Protocol}) and the big-endian readers and writers.
 * The packets themselves are in {@link dev.beezy.protocol.packet}.
 *
 * <p>This module has no dependencies and no platform types: colours are ARGB {@code int}s,
 * worlds are dimension-id strings, text components are already-serialized JSON
 * ({@link dev.beezy.protocol.JsonText}). Plugins normally use {@code beezy-api} instead.
 */
@Stable
package dev.beezy.protocol;

import dev.beezy.stability.Stable;
