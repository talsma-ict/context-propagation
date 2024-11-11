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
 * A wrapper for {@link Consumer} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 */
public class ConsumerWithContext<T> extends WrapperWithContextAndConsumer<Consumer<T>> implements Consumer<T> {
    private static final Logger LOGGER = Logger.getLogger(ConsumerWithContext.class.getName());

    public ConsumerWithContext(ContextSnapshot snapshot, Consumer<T> delegate) {
        this(snapshot, delegate, null);
    }

    public ConsumerWithContext(ContextSnapshot snapshot, Consumer<T> delegate, Consumer<ContextSnapshot> consumer) {
        super(snapshot, delegate, consumer);
    }

    protected ConsumerWithContext(Supplier<ContextSnapshot> supplier, Consumer<T> delegate, Consumer<ContextSnapshot> consumer) {
        super(supplier, delegate, consumer);
    }

    @Override
    public void accept(T t) {
        try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
            try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                LOGGER.log(Level.FINEST, "Delegating accept method with {0} to {1}.", new Object[]{context, delegate()});
                delegate().accept(t);
            } finally {
                if (contextSnapshotConsumer != null) {
                    ContextSnapshot resultSnapshot = ContextManagers.createContextSnapshot();
                    LOGGER.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
                    contextSnapshotConsumer.accept(resultSnapshot);
                }
            }
        }
    }

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
                        ContextSnapshot resultSnapshot = ContextManagers.createContextSnapshot();
                        LOGGER.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
                        contextSnapshotConsumer.accept(resultSnapshot);
                    }
                }
            }
        };
    }
}
