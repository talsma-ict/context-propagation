/*
 * Copyright 2016-2026 Talsma ICT
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

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

import static java.util.Objects.requireNonNull;

/**
 * {@linkplain BiConsumer} that {@linkplain ContextSnapshot#reactivate() reactivates a context snapshot}
 * while passing the values.
 *
 * <p>
 * Implemented as a {@linkplain nl.talsmasoftware.context.core.delegation.Wrapper wrapper} for {@link BiConsumer}.
 *
 * <p>
 * The reactivated context snapshot will be safely closed after the delegate function has been applied.
 *
 * @param <T> the type of the first argument to the operation.
 * @param <U> the type of the second argument to the operation.
 * @author Sjoerd Talsma
 */
public class BiConsumerWithContext<T, U> extends WrapperWithContextAndConsumer<BiConsumer<T, U>> implements BiConsumer<T, U> {
    /**
     * Creates a new bi-consumer that performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>accept the values by passing them to the delegate bi-consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     *
     * @param snapshot Context snapshot to run the delegate task in.
     * @param delegate The delegate bi-consumer to pass the values to.
     */
    public BiConsumerWithContext(ContextSnapshot snapshot, BiConsumer<T, U> delegate) {
        this(snapshot, delegate, null);
    }

    /**
     * Creates a new bi-consumer that performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>accept the values by passing them to the delegate bi-consumer
     * <li><em>if snapshot consumer is non-null,</em>
     * pass it a {@linkplain ContextSnapshot#capture() new context snapshot}
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     *
     * @param snapshot         Context snapshot to run the delegate task in.
     * @param delegate         The delegate bi-consumer to pass the values to.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    public BiConsumerWithContext(ContextSnapshot snapshot, BiConsumer<T, U> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshot, delegate, snapshotConsumer);
    }

    /**
     * Protected constructor for use with a snapshot 'holder' object that acts as both snapshot supplier and consumer.
     *
     * <p>
     * This constructor is not for general use. Care must be taken to capture the context snapshot <em>before</em> the
     * bi-consumer is called, otherwise the snapshot being reactivated would effectively update the context
     * to the same values just captured.
     *
     * @param snapshotSupplier Supplier for the context snapshot that was previously captured.
     * @param delegate         The delegate bi-consumer to pass the values to.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    protected BiConsumerWithContext(Supplier<ContextSnapshot> snapshotSupplier, BiConsumer<T, U> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshotSupplier, delegate, snapshotConsumer);
    }

    /**
     * Accept the values by passing them to the delegate bi-consumer within a reactivated context snapshot.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>accept the values by passing them to the delegate bi-consumer
     * <li><em>if snapshot consumer is non-null,</em>
     * pass it a {@linkplain ContextSnapshot#capture() new context snapshot}
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     *
     * @param in1 the first input argument to be passed to the delegate bi-consumer.
     * @param in2 the second input argument to be passed to the delegate bi-consumer.
     */
    @Override
    public void accept(T in1, U in2) {
        try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
            try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                logger.log(Level.FINEST, "Delegating accept method with {0} to {1}.", new Object[]{context, delegate()});
                delegate().accept(in1, in2);
            } finally {
                captureResultSnapshotIfRequired();
            }
        }
    }

    /**
     * Constructs a new bi-consumer that accepts values by passing them to the delegate bi-consumer within a
     * reactivated context snapshot, however <em>before closing the reactivation</em> the {@code after} operation is
     * also called within the reactivated context.
     *
     * <p>
     * The resulting bi-consumer performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>accept the values by:
     * <ol>
     *     <li>passing them to the delegate bi-consumer
     *     <li>passing them to the {@code after} bi-consumer
     * </ol>
     * <li><em>if snapshot consumer is non-null,</em>
     * pass it a {@linkplain ContextSnapshot#capture() new context snapshot}
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     *
     * @param after the operation to perform after this operation
     * @return new bi-consumer that also applies the {@code after} operation within the reactivated context snapshot.
     */
    @Override
    public BiConsumer<T, U> andThen(BiConsumer<? super T, ? super U> after) {
        requireNonNull(after, "Cannot post-process with after bi-consumer <null>.");
        return (l, r) -> {
            try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
                try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                    logger.log(Level.FINEST, "Delegating andThen method with {0} to {1}.", new Object[]{context, delegate()});
                    delegate().accept(l, r);
                    after.accept(l, r);
                } finally {
                    captureResultSnapshotIfRequired();
                }
            }
        };
    }

}
