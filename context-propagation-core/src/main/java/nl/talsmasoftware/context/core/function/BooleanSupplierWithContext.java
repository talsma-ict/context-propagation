/*
 * Copyright 2016-2025 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.context.core.function;

import nl.talsmasoftware.context.api.ContextSnapshot;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * {@linkplain BooleanSupplier Boolean supplier} that {@linkplain ContextSnapshot#reactivate() reactivates a context snapshot}
 * when {@linkplain #getAsBoolean() getting} the outcome.
 *
 * <p>
 * Implemented as a {@linkplain nl.talsmasoftware.context.core.delegation.Wrapper wrapper} for {@link BooleanSupplier}.
 *
 * <p>
 * The reactivated context snapshot will be safely closed after the delegate function has been applied.
 *
 * @author Sjoerd Talsma
 */
public class BooleanSupplierWithContext extends WrapperWithContextAndConsumer<BooleanSupplier> implements BooleanSupplier {
    /**
     * Creates a new boolean supplier with context.
     *
     * <p>
     * This function performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>getting the outcome from the delegate boolean supplier
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the outcome from the delegate boolean supplier call (or throw runtime exception if the delegate did).
     * </ol>
     *
     * @param snapshot Context snapshot to apply the delegate function in.
     * @param delegate The delegate boolean supplier.
     */
    public BooleanSupplierWithContext(ContextSnapshot snapshot, BooleanSupplier delegate) {
        this(snapshot, delegate, null);
    }

    /**
     * Creates a new boolean supplier with context.
     *
     * <p>
     * This function performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>getting the outcome from the delegate boolean supplier
     * <li><em>if snapshot consumer is non-null,</em>
     * pass it a {@linkplain ContextSnapshot#capture() new context snapshot}
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the outcome from the delegate boolean supplier call (or throw runtime exception if the delegate did).
     * </ol>
     *
     * @param snapshot         Context snapshot to apply the delegate function in.
     * @param delegate         The delegate boolean supplier.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate function was applied
     *                         (optional, may be {@code null}).
     */
    public BooleanSupplierWithContext(ContextSnapshot snapshot, BooleanSupplier delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshot, delegate, snapshotConsumer);
    }

    /**
     * Protected constructor for use with a snapshot 'holder' object that acts as both snapshot supplier and consumer.
     *
     * <p>
     * This constructor is not for general use. Care must be taken to capture the context snapshot <em>before</em> the
     * function is called, otherwise the snapshot being reactivated would effectively update the context
     * to the same values just captured.
     *
     * @param snapshotSupplier Supplier for the context snapshot that was previously captured.
     * @param delegate         The delegate boolean supplier.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    protected BooleanSupplierWithContext(Supplier<ContextSnapshot> snapshotSupplier, BooleanSupplier delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshotSupplier, delegate, snapshotConsumer);
    }

    /**
     * Gets the outcome from the delegate boolean supplier within a reactivated context snapshot.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>get the outcome from the delegate boolean supplier
     * <li><em>if context snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the outcome</li>
     * </ol>
     */
    @Override
    public boolean getAsBoolean() {
        try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
            try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                logger.log(Level.FINEST, "Delegating getAsBoolean method with {0} to {1}.", new Object[]{context, delegate()});
                return delegate().getAsBoolean();
            } finally {
                captureResultSnapshotIfRequired();
            }
        }
    }

}
