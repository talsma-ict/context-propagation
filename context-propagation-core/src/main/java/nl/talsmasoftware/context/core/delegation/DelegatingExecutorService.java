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
package nl.talsmasoftware.context.core.delegation;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.stream.Collectors.toList;

/**
 * {@linkplain java.util.concurrent.ExecutorService ExecutorService} delegating all scheduling operations,
 * providing a consistent way to {@code wrap} the scheduled tasks and resulting futures.
 *
 * <p>
 * This base class makes it easier to wrap existing {@link ExecutorService} implementations by forwarding all methods
 * to a {@link Wrapper#delegate() delegate} executor service.<br>
 * It also provides overridable <em>wrap</em> methods for scheduled tasks and resulting futures.
 *
 * <p>
 * Although this class implements <em>all</em> required methods, it is still declared <em>abstract</em>
 * because it only provides value if at least one method is overridden.
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

    /**
     * Wrap the given task.
     *
     * <p>
     * By default, the given task is returned as-is. Overriding this method provides subclasses
     * a consistent way to manipulate the tasks being scheduled.
     *
     * @param task The task to schedule.
     * @param <T>  The result of the task.
     * @return The wrapped task.
     * @see #wrap(Runnable)
     */
    protected <T> Callable<T> wrap(Callable<T> task) {
        return task;
    }

    /**
     * Wrap the given task.
     *
     * <p>
     * By default, the given task is returned as-is. Overriding this method provides subclasses
     * a consistent way to manipulate the tasks being scheduled.
     *
     * @param task The task to schedule.
     * @return The wrapped task.
     * @see #wrap(Callable)
     */
    protected Runnable wrap(Runnable task) {
        return task;
    }

    /**
     * Wrap the given future.
     *
     * <p>
     * By default, the given future is returned as-is. Overriding this method provides subclasses
     * a consistent way to manipulate a task's future result before returning it.
     *
     * @param future The future to be returned.
     * @param <T>    The result of the future.
     * @return The wrapped future.
     */
    protected <T> Future<T> wrap(Future<T> future) {
        return future;
    }

    /**
     * Default way of wrapping a {@link Collection} of {@link Callable} objects:
     * Create a new collection and add each {@link #wrap(Callable) individually wrapped} object into it.
     *
     * @param tasks The tasks to be wrapped.
     * @param <T>   The common result type for the collection of tasks.
     * @return A collection with each individual task wrapped.
     * @see #wrap(Callable)
     */
    protected <T> Collection<? extends Callable<T>> wrapTasks(Collection<? extends Callable<T>> tasks) {
        return tasks == null ? null : tasks.stream().map(this::wrap).collect(toList());
    }

    /**
     * Default way of wrapping a {@link Collection} of {@link Future} objects:
     * Create a new list and add each {@link #wrap(Future) individually wrapped} object into it.
     *
     * @param futures The futures to be wrapped.
     * @param <T>     The common result type for the collection of futures.
     * @return A list with each individual future wrapped.
     * @see #wrap(Future)
     */
    protected <T> List<Future<T>> wrapFutures(Collection<? extends Future<T>> futures) {
        return futures == null ? null : futures.stream().map(this::wrap).collect(toList());
    }

    /**
     * Shuts down the executor service by shutting down the delegate.
     */
    public void shutdown() {
        delegate().shutdown();
    }

    /**
     * Shuts down the executor service now by shutting down the delegate.
     *
     * @return The tasks the delegate returned.
     */
    public List<Runnable> shutdownNow() {
        return delegate().shutdownNow();
    }

    /**
     * Returns whether the delegate is shut down.
     *
     * @return Whether the delegate is shut down.
     */
    public boolean isShutdown() {
        return delegate().isShutdown();
    }

    /**
     * Returns whether the delegate is terminated.
     *
     * @return Whether the delegate is terminated.
     */
    public boolean isTerminated() {
        return delegate().isTerminated();
    }

    /**
     * Await termination of the delegate.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return {@code true} if the delegate terminated, {@code false} if a timeout occurred.
     * @throws InterruptedException if interrupted while waiting.
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate().awaitTermination(timeout, unit);
    }

    /**
     * Submits a task to the delegate.
     *
     * <p>
     * This method allows subclasses to <em>wrap</em> both the {@linkplain #wrap(Callable) task}
     * and the {@linkplain #wrap(Future) result}.
     *
     * @param task the task to submit.
     * @param <T>  the type of the task's result.
     * @return a Future representing pending completion of the task.
     * @see #wrap(Callable)
     * @see #wrap(Future)
     */
    public <T> Future<T> submit(Callable<T> task) {
        return wrap(delegate().submit(wrap(task)));
    }

    /**
     * Submits a task to the delegate with a fixed result.
     *
     * <p>
     * This method allows subclasses to <em>wrap</em> both the {@linkplain #wrap(Runnable) task}
     * and the {@linkplain #wrap(Future) result}.
     *
     * @param task   the task to submit.
     * @param result the result to return.
     * @param <T>    the type of the result.
     * @return a Future representing pending completion of the task.
     * @see #wrap(Runnable)
     * @see #wrap(Future)
     */
    public <T> Future<T> submit(Runnable task, T result) {
        return wrap(delegate().submit(wrap(task), result));
    }

    /**
     * Submits a task to the delegate.
     *
     * <p>
     * This method allows subclasses to <em>wrap</em> both the {@linkplain #wrap(Runnable) task}
     * and the {@linkplain #wrap(Future) result}.
     *
     * @param task the task to submit.
     * @return a Future representing pending completion of the task.
     * @see #wrap(Runnable)
     * @see #wrap(Future)
     */
    public Future<?> submit(Runnable task) {
        return wrap(delegate().submit(wrap(task)));
    }

    /**
     * Submits several tasks to the delegate.
     *
     * <p>
     * This method allows subclasses to <em>wrap</em> both the {@linkplain #wrap(Callable) task}
     * and the {@linkplain #wrap(Future) result}.
     *
     * @param tasks the tasks to submit.
     * @param <T>   the type of the values returned from the tasks.
     * @return a Future representing pending completion of the task.
     * @see #wrap(Callable)
     * @see #wrap(Future)
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        final List<Future<T>> futures = delegate().invokeAll(wrapTasks(tasks));
        return wrapFutures(futures);
    }

    /**
     * Invokes several tasks to the delegate.
     *
     * <p>
     * This method allows subclasses to <em>wrap</em> both the {@linkplain #wrap(Runnable) task}
     * and the {@linkplain #wrap(Future) result}.
     *
     * @param tasks   the tasks to invoke.
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return a list of Futures representing pending completions of the tasks.
     * @see #wrap(Runnable)
     * @see #wrap(Future)
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        final List<Future<T>> futures = delegate().invokeAll(wrapTasks(tasks), timeout, unit);
        return wrapFutures(futures);
    }

    /**
     * Executes the given tasks on the delegate, returning the result of one that has completed successfully
     * (i. e., without throwing an exception), if any do.
     *
     * <p>
     * This method allows subclasses to {@linkplain #wrap(Callable) wrap} the tasks.
     *
     * @param tasks the tasks to execute.
     * @return the result returned by one of the tasks.
     * @see #wrap(Callable)
     */
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate().invokeAny(wrapTasks(tasks));
    }

    /**
     * Executes the given tasks on the delegate, returning the result of one that has completed successfully
     * (i. e., without throwing an exception), if any do before the timeout elapses.
     *
     * <p>
     * This method allows subclasses to {@linkplain #wrap(Callable) wrap} the tasks.
     *
     * @param tasks   the tasks to execute.
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return the result returned by one of the tasks.
     * @throws InterruptedException if interrupted while waiting.
     * @throws NullPointerException if tasks, or unit, or any element task subject to execution is null.
     * @throws TimeoutException     if the given timeout elapses before any task successfully completes.
     * @throws ExecutionException   if no task successfully completes.
     * @see #wrap(Callable)
     */
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate().invokeAny(wrapTasks(tasks), timeout, unit);
    }

    /**
     * Executes the given command on the delegate.
     *
     * <p>
     * This method allows subclasses to {@linkplain #wrap(Runnable) wrap} the command.
     *
     * @param command the command to execute.
     * @see #wrap(Runnable)
     */
    public void execute(Runnable command) {
        delegate().execute(wrap(command));
    }

}
