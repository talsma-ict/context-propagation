/*
 * Copyright 2016-2024 Talsma ICT
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
import nl.talsmasoftware.context.core.ContextManagers;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * {@linkplain Consumer} that {@linkplain ContextSnapshot#reactivate() reactivates a context snapshot}
 * while accepting the value.
 *
 * <p>
 * Implemented as a {@linkplain nl.talsmasoftware.context.core.delegation.Wrapper wrapper} for {@link Consumer}.
 *
 * <p>
 * The reactivated context snapshot will be safely closed after the delegate consumer has been called.
 *
 * @param <T> the type of the input to the operation.
 * @author Sjoerd Talsma
 */
public class ConsumerWithContext<T> extends WrapperWithContextAndConsumer<Consumer<T>> implements Consumer<T> {
    private static final Logger LOGGER = Logger.getLogger(ConsumerWithContext.class.getName());

    /**
     * Creates a new consumer that performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>accept the value by passing it to the delegate consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     *
     * @param snapshot Context snapshot to run the delegate task in.
     * @param delegate The delegate consumer to pass the value to.
     */
    public ConsumerWithContext(ContextSnapshot snapshot, Consumer<T> delegate) {
        this(snapshot, delegate, null);
    }

    /**
     * Creates a new consumer that performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>accept the value by passing it to the delegate consumer
     * <li><em>if snapshot consumer is non-null,</em>
     * pass it a {@linkplain ContextManagers#createContextSnapshot() new context snapshot}
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     *
     * @param snapshot         Context snapshot to run the delegate task in.
     * @param delegate         The delegate consumer to pass the value to.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    public ConsumerWithContext(ContextSnapshot snapshot, Consumer<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshot, delegate, snapshotConsumer);
    }

    /**
     * Protected constructor for use with a snapshot 'holder' object that acts as both snapshot supplier and -consumer.
     *
     * <p>
     * This constructor is not for general use. Care must be taken to capture the context snapshot <em>before</em> the
     * consumer is called, otherwise the snapshot being reactivated would effectively update the context
     * to the same values just captured.
     *
     * @param snapshotSupplier Supplier for the context snapshot that was previously captured.
     * @param delegate         The delegate consumer to pass the value to.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    protected ConsumerWithContext(Supplier<ContextSnapshot> snapshotSupplier, Consumer<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshotSupplier, delegate, snapshotConsumer);
    }

    /**
     * Accept the value by passing it to the delegate consumer within a reactivated context snapshot.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>accept the value by passing it to the delegate consumer
     * <li><em>if snapshot consumer is non-null,</em>
     * pass it a {@linkplain ContextManagers#createContextSnapshot() new context snapshot}
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     *
     * @param value the value to be passed to the delegate consumer.
     */
    @Override
    public void accept(T value) {
        try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
            try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                LOGGER.log(Level.FINEST, "Delegating accept method with {0} to {1}.", new Object[]{context, delegate()});
                delegate().accept(value);
            } finally {
                if (contextSnapshotConsumer != null) {
                    ContextSnapshot resultSnapshot = ContextSnapshot.capture();
                    LOGGER.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
                    contextSnapshotConsumer.accept(resultSnapshot);
                }
            }
        }
    }

    /**
     * Constructs a new consumer that accepts values by passing it to the delegate consumer within a
     * reactivated context snapshot, however <em>before closing the reactivation</em> the {@code after} operation is
     * also called within the reactivated context.
     *
     * <p>
     * The resulting consumer performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>accept the value by:
     * <ol>
     *     <li>passing it to the delegate consumer
     *     <li>passing it to the {@code after} consumer
     * </ol>
     * <li><em>if snapshot consumer is non-null,</em>
     * pass it a {@linkplain ContextManagers#createContextSnapshot() new context snapshot}
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     *
     * @param after the operation to perform after this operation
     * @return new consumer that also applies the {@code after} operation within the reactivated context snapshot.
     */
    @Override
    public Consumer<T> andThen(Consumer<? super T> after) {
        requireNonNull(after, "Cannot follow ConsumerWithContext with after consumer <null>.");
        return (T t) -> {
            try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
                try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                    LOGGER.log(Level.FINEST, "Delegating andThen method with {0} to {1}.", new Object[]{context, delegate()});
                    delegate().accept(t);
                    after.accept(t);
                } finally {
                    if (contextSnapshotConsumer != null) {
                        ContextSnapshot resultSnapshot = ContextSnapshot.capture();
                        LOGGER.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
                        contextSnapshotConsumer.accept(resultSnapshot);
                    }
                }
            }
        };
    }
}
