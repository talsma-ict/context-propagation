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

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * {@linkplain BiFunction} that {@linkplain ContextSnapshot#reactivate() reactivates a context snapshot}
 * when {@linkplain #apply(Object, Object) applying} the function.
 *
 * <p>
 * Implemented as a {@linkplain nl.talsmasoftware.context.core.delegation.Wrapper wrapper} for {@link BiFunction}.
 *
 * <p>
 * The reactivated context snapshot will be safely closed after the delegate function has been applied.
 *
 * @param <IN1> the type of the first argument to the function.
 * @param <IN2> the type of the second argument to the function.
 * @param <OUT> the type of the result of the function.
 * @author Sjoerd Talsma
 */
public class BiFunctionWithContext<IN1, IN2, OUT> extends WrapperWithContextAndConsumer<BiFunction<IN1, IN2, OUT>> implements BiFunction<IN1, IN2, OUT> {
    private static final Logger LOGGER = Logger.getLogger(BiFunctionWithContext.class.getName());

    /**
     * Creates a new bi-function with context.
     *
     * <p>
     * This function performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>apply the delegate function
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the result from the delegate function call (or throw runtime exception if the delegate did).
     * </ol>
     *
     * @param snapshot Context snapshot to apply the delegate function in.
     * @param delegate The delegate bi-function to apply.
     */
    public BiFunctionWithContext(ContextSnapshot snapshot, BiFunction<IN1, IN2, OUT> delegate) {
        this(snapshot, delegate, null);
    }

    /**
     * Creates a new bi-function with context.
     *
     * <p>
     * This function performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>apply the delegate function
     * <li><em>if snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the result from the delegate function call (or throw runtime exception if the delegate did).
     * </ol>
     *
     * @param snapshot         Context snapshot to apply the delegate function in.
     * @param delegate         The delegate bi-function to apply.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate function was applied
     *                         (optional, may be {@code null}).
     */
    public BiFunctionWithContext(ContextSnapshot snapshot, BiFunction<IN1, IN2, OUT> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshot, delegate, snapshotConsumer);
    }

    /**
     * Protected constructor for use with a snapshot 'holder' object that acts as both snapshot supplier and -consumer.
     *
     * <p>
     * This constructor is not for general use. Care must be taken to capture the context snapshot <em>before</em> the
     * bi-function is called, otherwise the snapshot being reactivated would effectively update the context
     * to the same values just captured.
     *
     * @param snapshotSupplier Supplier for the context snapshot that was previously captured.
     * @param delegate         The delegate bi-function to apply.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    protected BiFunctionWithContext(Supplier<ContextSnapshot> snapshotSupplier, BiFunction<IN1, IN2, OUT> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshotSupplier, delegate, snapshotConsumer);
    }

    /**
     * Applies the delegate bi-function within reactivated context snapshot.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>apply the delegate bi-function and get the result
     * <li><em>if context snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the result</li>
     * </ol>
     *
     * @param in1 the first argument to pass to the delegate bi-function.
     * @param in2 the second argument to pass to the delegate bi-function.
     * @return the result of the delegate bi-function.
     * @throws RuntimeException if the delegate bi-function throws a runtime exception.
     */
    @Override
    public OUT apply(IN1 in1, IN2 in2) {
        try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
            try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                LOGGER.log(Level.FINEST, "Delegating apply method with {0} to {1}.", new Object[]{context, delegate()});
                return delegate().apply(in1, in2);
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
     * Constructs a new function that first the delegate bi-function within a reactivated context snapshot,
     * and then applies the {@code after} function to its result, still within the reactivated context.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>apply the delegate bi-function and get the result
     * <li>apply the {@code after} function to the result to get the end result
     * <li><em>if context snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the end result</li>
     * </ol>
     *
     * @param after the function to apply to the result of this bi-function.
     * @return a composed function that first applies this bi-function and then applies the after function,
     * all within a reactivated context snapshot.
     */
    @Override
    public <V> BiFunction<IN1, IN2, V> andThen(Function<? super OUT, ? extends V> after) {
        requireNonNull(after, "Cannot post-process bi-function with after function <null>.");
        return (IN1 in1, IN2 in2) -> {
            try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
                try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                    LOGGER.log(Level.FINEST, "Delegating andThen method with {0} to {1}.", new Object[]{context, delegate()});
                    return after.apply(delegate().apply(in1, in2));
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
