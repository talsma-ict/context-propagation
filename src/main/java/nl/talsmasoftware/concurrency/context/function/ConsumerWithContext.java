/*
 * Copyright (C) 2016 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.talsmasoftware.concurrency.context.function;

import nl.talsmasoftware.concurrency.context.Context;
import nl.talsmasoftware.concurrency.context.ContextSnapshot;

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
public class ConsumerWithContext<T> implements Consumer<T> {
    private static final Logger LOGGER = Logger.getLogger(ConsumerWithContext.class.getName());

    private final ContextSnapshot snapshot;
    private final Consumer<T> delegate;

    public ConsumerWithContext(ContextSnapshot snapshot, Consumer<T> delegate) {
        this.snapshot = requireNonNull(snapshot, "No context snapshot provided to ConsumerWithContext.");
        this.delegate = requireNonNull(delegate, "No delegate provided to ConsumerWithContext.");
    }


    @Override
    public void accept(T t) {
        try (Context<Void> context = snapshot.reactivate()) {
            LOGGER.log(Level.FINEST, "Delegating accept method with {0} to {1}.", new Object[]{context, delegate});
            delegate.accept(t);
        }
    }

    @Override
    public Consumer<T> andThen(Consumer<? super T> after) {
        requireNonNull(after, "Cannot follow consumer with after consumer <null>.");
        return (T t) -> {
            try (Context<Void> context = snapshot.reactivate()) {
                LOGGER.log(Level.FINEST, "Delegating andThen method with {0} to {1}.", new Object[]{context, delegate});
                delegate.accept(t);
                after.accept(t);
            }
        };
    }
}
