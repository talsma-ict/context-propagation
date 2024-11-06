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
package nl.talsmasoftware.context.core.delegation;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Abstract base-class that makes it a little easier to schedule tasks
 * ({@link Runnable Runnable} or {@link Callable} objects) using an existing {@link ExecutorService} while
 * providing a custom {@link #wrap(Callable) mapping} for all tasks <em>before</em> they get scheduled.
 *
 * @author Sjoerd Talsma
 */
public abstract class CallMappingExecutorService extends DelegatingExecutorService {

    /**
     * Constructor to create a new wrapper around the specified {@link ExecutorService service delegate}.
     *
     * @param delegate The delegate executor service that does the heavy lifting of executing all tasks once they are mapped.
     */
    protected CallMappingExecutorService(ExecutorService delegate) {
        super(delegate);
    }

    /**
     * The call mapping that needs to be implemented: map the given callable object into a desired variant before
     * scheduling.
     *
     * @param callable The callable to be mapped.
     * @param <V>      The type of result being returned by the callable object.
     * @return The mapped callable object.
     */
    protected abstract <V> Callable<V> map(Callable<V> callable);

    /**
     * Wrapping a callable object is delegated to the abstract {@link #map(Callable)} method.
     *
     * @param callable The callable to be mapped.
     * @param <V>      The type of result being returned by the callable object.
     * @return The mapped callable object.
     * @see #map(Callable)
     */
    @Override
    protected final <V> Callable<V> wrap(Callable<V> callable) {
        return map(callable);
    }

    /**
     * Default implementation to wrap {@link Runnable} objects before scheduling:
     * {@link #wrap(Callable) wrap} it into a {@link Callable} object and return an
     * unwrapped {@link Runnable} implementation that simply runs by calling the mapped {@link Callable} object.
     *
     * @param runnable The runnable object to be wrapped.
     * @return The wrapped runnable (the default implementation re-uses the callable mapping).
     * @see #wrap(Callable)
     */
    @Override
    protected Runnable wrap(final Runnable runnable) {
        if (runnable != null) {
            final Callable<?> callable = Executors.callable(runnable);
            final Callable<?> wrapped = wrap(callable);
            // Only return adapter if the wrapping resulted in a different object:
            if (!callable.equals(wrapped)) return new CallableToRunnable(wrapped);
        }
        return runnable;
    }

}
