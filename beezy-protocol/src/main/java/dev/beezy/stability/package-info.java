/**
 * Stability markers for Beezy's public types, in the spirit of JetBrains' {@code @ApiStatus}
 * but without a dependency.
 *
 * <ul>
 *   <li>{@link dev.beezy.stability.Stable}: covered by semantic versioning; only a major
 *       release may change or remove it.</li>
 *   <li>{@link dev.beezy.stability.Experimental}: may change in any minor release.</li>
 *   <li>{@link dev.beezy.stability.Internal}: public for technical reasons only; do not use.</li>
 *   <li>{@link dev.beezy.stability.NonExtendable}: call it, do not implement or extend it;
 *       new abstract methods may be added in a minor release.</li>
 * </ul>
 */
package dev.beezy.stability;
