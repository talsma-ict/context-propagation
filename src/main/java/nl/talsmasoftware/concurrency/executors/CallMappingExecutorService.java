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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Abstract base-class that makes it a little easier to schedule tasks
 * ({@link java.lang.Runnable Runnable} or {@link Callable} objects) using an existing {@link ExecutorService} while
 * providing a custom {@link #map(Callable) mapping} for all tasks <em>before</em> they get scheduled.
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
     * Default implementation to map {@link Runnable} objects before scheduling: wrap it into a {@link Callable} object,
     * {@link #map(Callable) map the callable} and return an unwrapped {@link Runnable} implementation that simply runs
     * by calling the mapped {@link Callable} object.
     *
     * @param runnable The runnable object to be mapped.
     * @return The mapped runnable (the default implementation re-uses the callable mapping).
     * @see #map(Callable)
     */
    protected Runnable map(final Runnable runnable) {
        if (runnable != null) {
            final Callable<?> callable = Executors.callable(runnable);
            final Callable<?> mapped = map(callable);
            // Only return adapter if the mapping resulted in a different object:
            if (!callable.equals(mapped)) return new RunnableAdapter(mapped);
        }
        return runnable;
    }

    /**
     * Default way of mapping a {@link Collection} of {@link Callable} objects:
     * Create a new collection and add each {@link #map(Callable) individually mapped} object into it.
     *
     * @param tasks The tasks to be mapped.
     * @param <T>   The common result type for the collection of tasks.
     * @return A collection with each individual task mapped.
     * @see #map(Callable)
     */
    protected <T> Collection<? extends Callable<T>> map(Collection<? extends Callable<T>> tasks) {
        Collection<? extends Callable<T>> result = tasks;
        if (tasks != null && !tasks.isEmpty()) {
            final List<Callable<T>> copy = new ArrayList<Callable<T>>(tasks.size());
            for (Callable<T> task : tasks) copy.add(map(task));
            result = copy;
        }
        return result;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(map(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(map(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(map(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return super.invokeAll(map(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return super.invokeAll(map(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return super.invokeAny(map(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return super.invokeAny(map(tasks), timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(map(command));
    }

}
