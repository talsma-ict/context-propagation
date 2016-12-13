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

package nl.talsmasoftware.concurrency.executors;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Abstract base-class that makes it a little easier to schedule tasks
 * ({@link java.lang.Runnable Runnable} or {@link Callable} objects) using an existing {@link ExecutorService} while
 * providing a custom {@link #map(Callable) mapping} for all tasks <em>before</em> they get scheduled.
 *
 * @author Sjoerd Talsma
 * @see nl.talsmasoftware.context.delegation.CallMappingExecutorService
 * @deprecated Please switch to <code>nl.talsmasoftware.context.delegation.CallMappingExecutorService</code>
 */
public abstract class CallMappingExecutorService extends nl.talsmasoftware.context.delegation.CallMappingExecutorService {

    /**
     * Constructor to create a new wrapper around the specified {@link ExecutorService service delegate}.
     *
     * @param delegate The delegate executor service that does the heavy lifting of executing all tasks once they are mapped.
     */
    protected CallMappingExecutorService(ExecutorService delegate) {
        super(delegate);
    }

    /**
     * Default implementation to map {@link Runnable} objects before scheduling: wrap it into a {@link Callable} object,
     * {@link #map(Callable) map the callable} and return an unwrapped {@link Runnable} implementation that simply runs
     * by calling the mapped {@link Callable} object.
     *
     * @param runnable The runnable object to be mapped.
     * @return The mapped runnable (the default implementation re-uses the callable mapping).
     * @see #wrap(Runnable)
     * @deprecated This method was replaced by {@link #wrap(Runnable)}.
     */
    protected Runnable map(final Runnable runnable) {
        return super.wrap(runnable);
    }

    /**
     * Default way of mapping a {@link Collection} of {@link Callable} objects:
     * Create a new collection and add each {@link #map(Callable) individually mapped} object into it.
     *
     * @param tasks The tasks to be mapped.
     * @param <T>   The common result type for the collection of tasks.
     * @return A collection with each individual task mapped.
     * @see #wrapTasks(Collection)
     * @deprecated This method was replaced by {@link #wrapTasks(Collection)}
     */
    protected <T> Collection<? extends Callable<T>> map(Collection<? extends Callable<T>> tasks) {
        return super.wrapTasks(tasks);
    }

}
