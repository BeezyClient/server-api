/**
 * The exception model. Every exception is a {@link dev.beezy.api.exception.BeezyException}
 * (unchecked). They are thrown only under {@link dev.beezy.api.DeliveryPolicy#THROW}, or used
 * to complete a transfer's stage exceptionally; invalid values throw
 * {@link java.lang.IllegalArgumentException} under either policy.
 */
@Stable
package dev.beezy.api.exception;

import dev.beezy.stability.Stable;
