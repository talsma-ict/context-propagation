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

import java.io.Closeable;
import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * A wrapper for {@link BiPredicate} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 */
public class BiPredicateWithContext<IN1, IN2> extends WrapperWithContextAndConsumer<BiPredicate<IN1, IN2>> implements BiPredicate<IN1, IN2> {
    private static final Logger LOGGER = Logger.getLogger(BiPredicateWithContext.class.getName());

    public BiPredicateWithContext(ContextSnapshot snapshot, BiPredicate<IN1, IN2> delegate) {
        this(snapshot, delegate, null);
    }

    public BiPredicateWithContext(ContextSnapshot snapshot, BiPredicate<IN1, IN2> delegate, Consumer<ContextSnapshot> consumer) {
        super(snapshot, delegate, consumer);
    }

    protected BiPredicateWithContext(Supplier<ContextSnapshot> supplier, BiPredicate<IN1, IN2> delegate, Consumer<ContextSnapshot> consumer) {
        super(supplier, delegate, consumer);
    }

    @Override
    public boolean test(IN1 in1, IN2 in2) {
        try (Closeable context = snapshot().reactivate()) {
            try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                LOGGER.log(Level.FINEST, "Delegating test method with {0} to {1}.", new Object[]{context, delegate()});
                return delegate().test(in1, in2);
            } finally {
                if (contextSnapshotConsumer != null) {
                    ContextSnapshot resultSnapshot = ContextManagers.createContextSnapshot();
                    LOGGER.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
                    contextSnapshotConsumer.accept(resultSnapshot);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public BiPredicate<IN1, IN2> and(BiPredicate<? super IN1, ? super IN2> other) {
        requireNonNull(other, "Cannot combine bi-predicate with 'and' <null>.");
        return (IN1 in1, IN2 in2) -> {
            try (Closeable context = snapshot().reactivate()) {
                try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                    LOGGER.log(Level.FINEST, "Delegating 'and' method with {0} to {1}.", new Object[]{context, delegate()});
                    return delegate().test(in1, in2) && other.test(in1, in2);
                } finally {
                    if (contextSnapshotConsumer != null) {
                        ContextSnapshot resultSnapshot = ContextManagers.createContextSnapshot();
                        LOGGER.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
                        contextSnapshotConsumer.accept(resultSnapshot);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }

    @Override
    public BiPredicate<IN1, IN2> or(BiPredicate<? super IN1, ? super IN2> other) {
        requireNonNull(other, "Cannot combine bi-predicate with 'or' <null>.");
        return (IN1 in1, IN2 in2) -> {
            try (Closeable context = snapshot().reactivate()) {
                try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                    LOGGER.log(Level.FINEST, "Delegating 'or' method with {0} to {1}.", new Object[]{context, delegate()});
                    return delegate().test(in1, in2) || other.test(in1, in2);
                } finally {
                    if (contextSnapshotConsumer != null) {
                        ContextSnapshot resultSnapshot = ContextManagers.createContextSnapshot();
                        LOGGER.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
                        contextSnapshotConsumer.accept(resultSnapshot);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }
}
