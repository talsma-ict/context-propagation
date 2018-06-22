/*
 * Copyright 2016-2018 Talsma ICT
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
package nl.talsmasoftware.context.functions;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.WrapperWithContext;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * A wrapper for {@link Consumer} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 */
public class ConsumerWithContext<T> extends WrapperWithContext<Consumer<T>> implements Consumer<T> {
    private static final Logger LOGGER = Logger.getLogger(ConsumerWithContext.class.getName());

    public ConsumerWithContext(ContextSnapshot snapshot, Consumer<T> delegate) {
        this(snapshot, delegate, null);
    }

    public ConsumerWithContext(ContextSnapshot snapshot, Consumer<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshot, delegate, snapshotConsumer == null ? null : snapshotConsumer::accept);
    }

    @Override
    public void accept(T t) {
        try (Context<Void> context = snapshot.reactivate()) {
            try {
                LOGGER.log(Level.FINEST, "Delegating accept method with {0} to {1}.", new Object[]{context, delegate()});
                nonNullDelegate().accept(t);
            } finally {
                if (consumer != null) {
                    ContextSnapshot resultSnapshot = ContextManagers.createContextSnapshot();
                    LOGGER.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
                    consumer.accept(resultSnapshot);
                }
            }
        }
    }

    @Override
    public Consumer<T> andThen(Consumer<? super T> after) {
        requireNonNull(after, "Cannot follow consumer with after consumer <null>.");
        return (T t) -> {
            try (Context<Void> context = snapshot.reactivate()) {
                try {
                    LOGGER.log(Level.FINEST, "Delegating andThen method with {0} to {1}.", new Object[]{context, delegate()});
                    nonNullDelegate().accept(t);
                    after.accept(t);
                } finally {
                    if (consumer != null) {
                        ContextSnapshot resultSnapshot = ContextManagers.createContextSnapshot();
                        LOGGER.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
                        consumer.accept(resultSnapshot);
                    }
                }
            }
        };
    }
}
