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

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * A wrapper for {@link Supplier} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 */
public class SupplierWithContext<T> implements Supplier<T> {
    private static final Logger LOGGER = Logger.getLogger(SupplierWithContext.class.getName());

    private final ContextSnapshot snapshot;
    private final Supplier<T> delegate;

    public SupplierWithContext(ContextSnapshot snapshot, Supplier<T> delegate) {
        this.snapshot = requireNonNull(snapshot, "No context snapshot provided to SupplierWithContext.");
        this.delegate = requireNonNull(delegate, "No delegate provided to SupplierWithContext.");
    }

    @Override
    public T get() {
        try (Context<Void> context = snapshot.reactivate()) {
            LOGGER.log(Level.FINEST, "Delegating get method with {0} to {1}.", new Object[]{context, delegate});
            return delegate.get();
        }
    }
}
