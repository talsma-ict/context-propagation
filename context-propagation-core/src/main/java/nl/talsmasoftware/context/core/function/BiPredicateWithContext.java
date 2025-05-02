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

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

import static java.util.Objects.requireNonNull;

/**
 * {@linkplain BiPredicate} that {@linkplain ContextSnapshot#reactivate() reactivates a context snapshot}
 * when testing the predicate.
 *
 * <p>
 * Implemented as a {@linkplain nl.talsmasoftware.context.core.delegation.Wrapper wrapper} for {@link BiPredicate}.
 *
 * <p>
 * The reactivated context snapshot will be safely closed after the delegate function has been applied.
 *
 * @param <IN1> the type of the first argument to the function.
 * @param <IN2> the type of the second argument to the function.
 * @author Sjoerd Talsma
 */
public class BiPredicateWithContext<IN1, IN2> extends WrapperWithContextAndConsumer<BiPredicate<IN1, IN2>> implements BiPredicate<IN1, IN2> {
    /**
     * Creates a new bi-predicate with context.
     *
     * <p>
     * This bi-predicate performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>test the delegate bi-predicate and get the outcome
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return bi-predicate outcome (or throw runtime exception if the delegate did).
     * </ol>
     *
     * @param snapshot Context snapshot to apply the delegate function in.
     * @param delegate The delegate function to apply.
     */
    public BiPredicateWithContext(ContextSnapshot snapshot, BiPredicate<IN1, IN2> delegate) {
        this(snapshot, delegate, null);
    }

    /**
     * Creates a new bi-predicate with context.
     *
     * <p>
     * This bi-predicate performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>test the delegate bi-predicate and get the outcome
     * <li><em>if snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return bi-predicate outcome (or throw runtime exception if the delegate did).
     * </ol>
     *
     * @param snapshot         Context snapshot to apply the delegate function in.
     * @param delegate         The delegate function to apply.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate function was applied
     *                         (optional, may be {@code null}).
     */
    public BiPredicateWithContext(ContextSnapshot snapshot, BiPredicate<IN1, IN2> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshot, delegate, snapshotConsumer);
    }

    /**
     * Protected constructor for use with a snapshot 'holder' object that acts as both snapshot supplier and consumer.
     *
     * <p>
     * This constructor is not for general use. Care must be taken to capture the context snapshot <em>before</em> the
     * bi-predicate is called, otherwise the snapshot being reactivated would effectively update the context
     * to the same values just captured.
     *
     * @param snapshotSupplier Supplier for the context snapshot that was previously captured.
     * @param delegate         The delegate bi-predicate to test.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    protected BiPredicateWithContext(Supplier<ContextSnapshot> snapshotSupplier, BiPredicate<IN1, IN2> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshotSupplier, delegate, snapshotConsumer);
    }

    /**
     * Test the delegate bi-predicate in a reactivated context snapshot.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>test the bi-delegate predicate and get the outcome
     * <li><em>if context snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the outcome</li>
     * </ol>
     *
     * @param in1 the first input argument.
     * @param in2 the second input argument.
     * @return {@code true} if the input arguments match the delegate bi-predicate, otherwise {@code false}.
     */
    @Override
    public boolean test(IN1 in1, IN2 in2) {
        try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
            try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                logger.log(Level.FINEST, "Delegating test method with {0} to {1}.", new Object[]{context, delegate()});
                return delegate().test(in1, in2);
            } finally {
                captureResultSnapshotIfRequired();
            }
        }
    }

    /**
     * Returns a composed bi-predicate that represents a short-circuiting logical AND of this bi-predicate and another,
     * both in a single reactivated context snapshot.
     * When evaluating the composed bi-predicate, if this bi-predicate is {@code false},
     * then the other is not evaluated.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>test the delegate bi-predicate and get the outcome
     * <li><em>if the outcome is true, </em> test the {@code other} bi-predicate and return that outcome</li>
     * <li><em>if context snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the outcome</li>
     * </ol>
     *
     * <p>
     * Any exceptions thrown during evaluation of either bi-predicate are relayed to the caller;
     * if evaluation of this bi-predicate throws an exception, the other predicate will not be evaluated.
     * In either case, the reactivated context will still be properly closed.
     *
     * @param other a bi-predicate that will be logically-ANDed with this bi-predicate
     * @return a composed bi-predicate that represents the short-circuiting logical AND
     * of this bi-predicate and the other bi-predicate, both within the reactivated context snapshot.
     */
    @Override
    public BiPredicate<IN1, IN2> and(BiPredicate<? super IN1, ? super IN2> other) {
        requireNonNull(other, "Cannot combine bi-predicate with 'and' <null>.");
        return (IN1 in1, IN2 in2) -> {
            try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
                try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                    logger.log(Level.FINEST, "Delegating 'and' method with {0} to {1}.", new Object[]{context, delegate()});
                    return delegate().test(in1, in2) && other.test(in1, in2);
                } finally {
                    captureResultSnapshotIfRequired();
                }
            }
        };
    }

    /**
     * Returns a composed bi-predicate that represents a short-circuiting logical OR of this bi-predicate and another,
     * both in a single reactivated context snapshot.
     * When evaluating the composed bi-predicate, if this bi-predicate is {@code true},
     * then the other bi-predicate is not evaluated.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>test the delegate bi-predicate and get the outcome
     * <li><em>if the outcome is false, </em> test the {@code other} vi-predicate and return that outcome</li>
     * <li><em>if context snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the outcome</li>
     * </ol>
     * <p>
     * Any exceptions thrown during evaluation of either bi-predicate are relayed to the caller;
     * if evaluation of this bi-predicate throws an exception, the other bi-predicate will not be evaluated.
     * In either case, the reactivated context will still be properly closed.
     *
     * @param other a bi-predicate that will be logically-ORed with this predicate,
     * @return a composed bi-predicate that represents the short-circuiting logical OR
     * of this bi-predicate and the other bi-predicate, both within the reactivated context snapshot.
     */
    @Override
    public BiPredicate<IN1, IN2> or(BiPredicate<? super IN1, ? super IN2> other) {
        requireNonNull(other, "Cannot combine bi-predicate with 'or' <null>.");
        return (IN1 in1, IN2 in2) -> {
            try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
                try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                    logger.log(Level.FINEST, "Delegating 'or' method with {0} to {1}.", new Object[]{context, delegate()});
                    return delegate().test(in1, in2) || other.test(in1, in2);
                } finally {
                    captureResultSnapshotIfRequired();
                }
            }
        };
    }
}
