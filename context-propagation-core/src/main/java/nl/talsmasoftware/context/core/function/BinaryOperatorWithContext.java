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

import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@linkplain BinaryOperator Binary operator} that {@linkplain ContextSnapshot#reactivate() reactivates a context snapshot}
 * when {@linkplain BiFunctionWithContext#apply(Object, Object) applying} the operator.
 *
 * <p>
 * Implemented as a {@linkplain nl.talsmasoftware.context.core.delegation.Wrapper wrapper} for {@link BinaryOperator}.
 *
 * <p>
 * The reactivated context snapshot will be safely closed after the delegate function has been applied.
 *
 * @param <T> the type of the operands and result of the operator.
 * @author Sjoerd Talsma
 */
public class BinaryOperatorWithContext<T> extends BiFunctionWithContext<T, T, T> implements BinaryOperator<T> {
    /**
     * Creates a new binary operator with context.
     *
     * <p>
     * This binary operator performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>apply the delegate binary operator
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the result from the delegate operator call (or throw runtime exception if the delegate did).
     * </ol>
     *
     * @param snapshot Context snapshot to apply the delegate operator in.
     * @param delegate The delegate binary operator to apply.
     */
    public BinaryOperatorWithContext(ContextSnapshot snapshot, BinaryOperator<T> delegate) {
        this(snapshot, delegate, null);
    }

    /**
     * Creates a new binary operator with context.
     *
     * <p>
     * This binary operator performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>apply the delegate binary operator
     * <li><em>if snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the result from the delegate operator call (or throw runtime exception if the delegate did).
     * </ol>
     *
     * @param snapshot         Context snapshot to apply the delegate operator in.
     * @param delegate         The delegate binary operator to apply.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate operator was applied
     *                         (optional, may be {@code null}).
     */
    public BinaryOperatorWithContext(ContextSnapshot snapshot, BinaryOperator<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
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
     * @param delegate         The delegate binary operator to apply.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    protected BinaryOperatorWithContext(Supplier<ContextSnapshot> snapshotSupplier, BinaryOperator<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshotSupplier, delegate, snapshotConsumer);
    }
}
