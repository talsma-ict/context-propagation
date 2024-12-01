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

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * {@linkplain Predicate} that {@linkplain ContextSnapshot#reactivate() reactivates a context snapshot}
 * when testing the predicate.
 *
 * <p>
 * Implemented as a {@linkplain nl.talsmasoftware.context.core.delegation.Wrapper wrapper} for {@link Predicate}.
 *
 * <p>
 * The reactivated context snapshot will be safely closed after the delegate function has been applied.
 *
 * @param <T> the type of the argument to the function.
 * @author Sjoerd Talsma
 */
public class PredicateWithContext<T> extends WrapperWithContextAndConsumer<Predicate<T>> implements Predicate<T> {
    private static final Logger LOGGER = Logger.getLogger(PredicateWithContext.class.getName());

    /**
     * Creates a new predicate with context.
     *
     * <p>
     * This predicate performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>test the delegate predicate and get the outcome
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return predicate outcome (or throw runtime exception if the delegate did).
     * </ol>
     *
     * @param snapshot Context snapshot to apply the delegate function in.
     * @param delegate The delegate function to apply.
     */
    public PredicateWithContext(ContextSnapshot snapshot, Predicate<T> delegate) {
        this(snapshot, delegate, null);
    }

    /**
     * Creates a new predicate with context.
     *
     * <p>
     * This predicate performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>test the delegate predicate and get the outcome
     * <li><em>if snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return predicate outcome (or throw runtime exception if the delegate did).
     * </ol>
     *
     * @param snapshot         Context snapshot to apply the delegate function in.
     * @param delegate         The delegate function to apply.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate function was applied
     *                         (optional, may be {@code null}).
     */
    public PredicateWithContext(ContextSnapshot snapshot, Predicate<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshot, delegate, snapshotConsumer);
    }

    /**
     * Protected constructor for use with a snapshot 'holder' object that acts as both snapshot supplier and -consumer.
     *
     * <p>
     * This constructor is not for general use. Care must be taken to capture the context snapshot <em>before</em> the
     * predicate is called, otherwise the snapshot being reactivated would effectively update the context
     * to the same values just captured.
     *
     * @param snapshotSupplier Supplier for the context snapshot that was previously captured.
     * @param delegate         The delegate predicate to test.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    protected PredicateWithContext(Supplier<ContextSnapshot> snapshotSupplier, Predicate<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshotSupplier, delegate, snapshotConsumer);
    }

    /**
     * Test the delegate predicate in a reactivated context snapshot.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>test the delegate predicate and get the outcome
     * <li><em>if context snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the outcome</li>
     * </ol>
     *
     * @param value the input argument
     * @return {@code true} if the input argument matches the delegate predicate, otherwise {@code false}.
     */
    @Override
    public boolean test(T value) {
        try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
            try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                LOGGER.log(Level.FINEST, "Delegating test method with {0} to {1}.", new Object[]{context, delegate()});
                return delegate().test(value);
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
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another,
     * both in a single reactivated context snapshot.
     * When evaluating the composed predicate, if this predicate is {@code false},
     * then the other predicate is not evaluated.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>test the delegate predicate and get the outcome
     * <li><em>if the outcome is true, </em> test the {@code other} predicate and return that outcome</li>
     * <li><em>if context snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the final outcome</li>
     * </ol>
     *
     * <p>
     * Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the other predicate will not be evaluated.
     * In either case, the reactivated context will still be properly closed.
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND
     * of this predicate and the other predicate, both within the reactivated context snapshot.
     */
    @Override
    public Predicate<T> and(Predicate<? super T> other) {
        requireNonNull(other, "Cannot combine predicate with 'and' <null>.");
        return (t) -> {
            try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
                try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                    LOGGER.log(Level.FINEST, "Delegating 'and' method with {0} to {1}.", new Object[]{context, delegate()});
                    return delegate().test(t) && other.test(t);
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

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another,
     * both in a single reactivated context snapshot.
     * When evaluating the composed predicate, if this predicate is {@code true},
     * then the other predicate is not evaluated.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>test the delegate predicate and get the outcome
     * <li><em>if the outcome is false, </em> test the {@code other} predicate and return that outcome</li>
     * <li><em>if context snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextSnapshot#capture() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * <li>return the final outcome</li>
     * </ol>
     * <p>
     * Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the other predicate will not be evaluated.
     * In either case, the reactivated context will still be properly closed.
     *
     * @param other a predicate that will be logically-ORed with this predicate,
     * @return a composed predicate that represents the short-circuiting logical OR
     * of this predicate and the other predicate, both within the reactivated context snapshot.
     */
    @Override
    public Predicate<T> or(Predicate<? super T> other) {
        requireNonNull(other, "Cannot combine predicate with 'or' <null>.");
        return (t) -> {
            try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
                try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                    LOGGER.log(Level.FINEST, "Delegating 'or' method with {0} to {1}.", new Object[]{context, delegate()});
                    return delegate().test(t) || other.test(t);
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
