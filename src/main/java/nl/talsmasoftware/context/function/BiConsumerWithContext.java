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

package nl.talsmasoftware.context.function;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.WrapperWithContext;

import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * A wrapper for {@link BiConsumer} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 */
public class BiConsumerWithContext<T, U> extends WrapperWithContext<BiConsumer<T, U>> implements BiConsumer<T, U> {
    private static final Logger LOGGER = Logger.getLogger(BiConsumerWithContext.class.getName());

    public BiConsumerWithContext(ContextSnapshot snapshot, BiConsumer<T, U> delegate) {
        super(snapshot, delegate);
    }

    @Override
    public void accept(T t, U u) {
        try (Context<Void> context = snapshot.reactivate()) {
            LOGGER.log(Level.FINEST, "Delegating accept method with {0} to {1}.", new Object[]{context, delegate()});
            nonNullDelegate().accept(t, u);
        }
    }

    @Override
    public BiConsumer<T, U> andThen(BiConsumer<? super T, ? super U> after) {
        requireNonNull(after, "Cannot post-process with after bi-consumer <null>.");
        return (l, r) -> {
            try (Context<Void> context = snapshot.reactivate()) {
                LOGGER.log(Level.FINEST, "Delegating andThen method with {0} to {1}.", new Object[]{context, delegate()});
                nonNullDelegate().accept(l, r);
                after.accept(l, r);
            }
        };
    }

}
