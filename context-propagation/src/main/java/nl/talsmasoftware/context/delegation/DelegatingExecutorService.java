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
package nl.talsmasoftware.context.delegation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Abstract baseclass that makes it a little easier to wrap existing {@link ExecutorService} implementations by
 * forwarding all methods to a {@link Wrapper#delegate() delegate} executor service.<br>
 * The class also provides overridable <code>wrapper</code> methods for all complex input (e.g. {@link Callable}, {@link Runnable})
 * and result types (e.g. {@link Future}).
 * <p>
 * Although this class does implements <em>all</em> required methods of {@link ExecutorService} it is still declared
 * as an <em>abstract</em> class.<br>
 * This is because it does not provide any value in itself.
 *
 * @author Sjoerd Talsma
 */
public abstract class DelegatingExecutorService extends Wrapper<ExecutorService> implements ExecutorService {

    /**
     * Creates a new executor service that delegates all methods to the specified <code>delegate</code>.
     *
     * @param delegate The delegate ExecutorService being wrapped.
     *                 This may <strong>only</strong> be <code>null</code> if the <code>delegate()</code> method is
     *                 overridden to provide an alternative non-<code>null</code> result.
     * @see #delegate()
     */
    protected DelegatingExecutorService(ExecutorService delegate) {
        super(delegate);
    }

    protected <T> Callable<T> wrap(Callable<T> source) {
        return source;
    }

    protected Runnable wrap(Runnable source) {
        return source;
    }

    protected <T> Future<T> wrap(Future<T> source) {
        return source;
    }

    /**
     * Default way of mapping a {@link Collection} of {@link Callable} objects:
     * Create a new collection and add each {@link #wrap(Callable) individually wrapped} object into it.
     *
     * @param tasks The tasks to be mapped.
     * @param <T>   The common result type for the collection of tasks.
     * @return A collection with each individual task wrapped.
     * @see #wrap(Callable)
     */
    protected <T> Collection<? extends Callable<T>> wrapTasks(Collection<? extends Callable<T>> tasks) {
        Collection<? extends Callable<T>> wrappedTasks = tasks;
        if (tasks != null && !tasks.isEmpty()) {
            boolean modification = false;
            final List<Callable<T>> copy = new ArrayList<Callable<T>>(tasks.size());
            for (Callable<T> task : tasks) {
                final Callable<T> wrapped = wrap(task);
                modification |= (task == wrapped || (task != null && task.equals(wrapped))); // TODO Objects.equals
                copy.add(wrapped);
            }
            if (modification) wrappedTasks = copy;
        }
        return wrappedTasks;
    }

    /**
     * Default way of mapping a {@link Collection} of {@link Future} objects:
     * Create a new list and add each {@link #wrap(Future) individually wrapped} object into it.
     *
     * @param futures The futures to be mapped.
     * @param <T>     The common result type for the collection of futures.
     * @return A list with each individual future wrapped.
     * @see #wrap(Future)
     */
    protected <T> List<Future<T>> wrapFutures(Collection<? extends Future<T>> futures) {
        if (futures == null) return null;
        final List<Future<T>> wrappedFutures = new ArrayList<Future<T>>(futures.size());
        for (Future<T> future : futures) wrappedFutures.add(wrap(future));
        return wrappedFutures;
    }

    public void shutdown() {
        delegate().shutdown();
    }

    public List<Runnable> shutdownNow() {
        return delegate().shutdownNow();
    }

    public boolean isShutdown() {
        return delegate().isShutdown();
    }

    public boolean isTerminated() {
        return delegate().isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate().awaitTermination(timeout, unit);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return wrap(delegate().submit(wrap(task)));
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return wrap(delegate().submit(wrap(task), result));
    }

    public Future<?> submit(Runnable task) {
        return wrap(delegate().submit(wrap(task)));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        final List<Future<T>> futures = delegate().invokeAll(wrapTasks(tasks));
        return wrapFutures(futures);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        final List<Future<T>> futures = delegate().invokeAll(wrapTasks(tasks), timeout, unit);
        return wrapFutures(futures);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate().invokeAny(wrapTasks(tasks));
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate().invokeAny(wrapTasks(tasks), timeout, unit);
    }

    public void execute(Runnable command) {
        delegate().execute(wrap(command));
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other != null && getClass().equals(other.getClass())
                && delegate().equals(((DelegatingExecutorService) other).delegate()));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + delegate() + '}';
    }

}
