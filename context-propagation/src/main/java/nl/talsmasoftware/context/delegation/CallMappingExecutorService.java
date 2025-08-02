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
package nl.talsmasoftware.context.delegation;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Abstract base-class that makes it a little easier to schedule tasks
 * ({@link java.lang.Runnable Runnable} or {@link Callable} objects) using an existing {@link ExecutorService} while
 * providing a custom {@link #wrap(Callable) mapping} for all tasks <em>before</em> they get scheduled.
 *
 * @author Sjoerd Talsma
 * @deprecated This will be replace by {@code Supplier<ContextSnapshot>} from Java 8.
 */
@Deprecated
public abstract class CallMappingExecutorService extends nl.talsmasoftware.context.core.delegation.CallMappingExecutorService {

    /**
     * Constructor to create a new wrapper around the specified {@link ExecutorService service delegate}.
     *
     * @param delegate The delegate executor service that does the heavy lifting of executing all tasks once they are mapped.
     */
    protected CallMappingExecutorService(ExecutorService delegate) {
        super(delegate);
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
        return super.wrap(runnable);
    }

}
