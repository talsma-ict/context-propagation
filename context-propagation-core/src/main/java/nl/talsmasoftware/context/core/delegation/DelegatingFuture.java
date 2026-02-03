/*
 * Copyright 2016-2026 Talsma ICT
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@linkplain Future future} that delegates all operations, allowing the result or error to be wrapped.
 *
 * <p>
 * This base class provides overridable wrapper methods for {@link #wrapResult(Object) result}
 * and {@link #wrapException(ExecutionException) exception} outcomes.
 *
 * <p>
 * Although this class implements <em>all</em> required methods, it is still declared <em>abstract</em>
 * because it only provides value if at least one method is overridden.
 *
 * @param <V> The type of the result of this future.
 * @author Sjoerd Talsma
 */
public abstract class DelegatingFuture<V> extends Wrapper<Future<V>> implements Future<V> {
    /**
     * Creates a new future that delegates all methods to the specified <code>delegate</code>.
     *
     * @param delegate The delegate Future being wrapped.
     *                 This may <strong>only</strong> be <code>null</code> if the <code>delegate()</code> method is
     *                 overridden to provide an alternative non-<code>null</code> result.
     * @see #delegate()
     */
    protected DelegatingFuture(Future<V> delegate) {
        super(delegate);
    }

    /**
     * Overridable method to wrap the result after it has been obtained from the delegate future.
     *
     * @param result The original result from the delegate.
     * @return The wrapped result.
     */
    protected V wrapResult(V result) {
        return result;
    }

    /**
     * Overridable method to wrap the {@link ExecutionException} after it has been thrown from the delegate future.
     *
     * @param exception The original execution exception from the delegate.
     * @return The wrapped exception.
     */
    protected ExecutionException wrapException(ExecutionException exception) {
        return exception;
    }

    /**
     * Attempts to cancel execution of this task by cancelling the delegate.
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete.
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed normally; {@code true} otherwise.
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate().cancel(mayInterruptIfRunning);
    }

    /**
     * Returns whether this or the delegate future was cancelled before it completed normally.
     *
     * @return {@code true} if this task or its delegate was cancelled before it completed.
     */
    public boolean isCancelled() {
        return delegate().isCancelled();
    }

    /**
     * Returns whether this task completed either normally or exceptionally.
     *
     * @return {@code true} if the delegate future completed normally or exceptionally.
     */
    public boolean isDone() {
        return delegate().isDone();
    }

    /**
     * Waits if necessary for the delegate future to complete and returns its result.
     *
     * <p>
     * This method allows subclasses to <em>wrap</em>
     * the {@linkplain #wrapResult(Object) result}
     * or {@linkplain #wrapException(ExecutionException) exception}.
     *
     * @return the result from the delegate future.
     * @throws ExecutionException   if the computation threw an exception.
     * @throws InterruptedException if the current thread was interrupted while waiting for the result.
     * @see #wrapResult(Object)
     * @see #wrapException(ExecutionException)
     */
    public V get() throws InterruptedException, ExecutionException {
        try {
            return wrapResult(delegate().get());
        } catch (ExecutionException ee) {
            throw wrapException(ee);
        }
    }

    /**
     * Waits if necessary for at most the given time for the delegate future to complete,
     * and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return the result from the delegate future.
     * @throws ExecutionException   if the computation threw an exception.
     * @throws InterruptedException if the current thread was interrupted while waiting for the result.
     * @throws TimeoutException     if the wait timed out.
     */
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return wrapResult(delegate().get(timeout, unit));
        } catch (ExecutionException ee) {
            throw wrapException(ee);
        }
    }
}
