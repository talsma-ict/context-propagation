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
import java.util.List;
import java.util.concurrent.*;

/**
 * Abstract baseclass that makes it a little easier to wrap existing {@link ExecutorService} implementations by
 * forwarding all methods to a <code>delegate</code> executor service.
 * <p>
 * Although this class implements all required methods of {@link ExecutorService} it is still declared as an
 * <em>abstract</em> class. This is because it does not provide any value in itself.
 *
 * @author Sjoerd Talsma
 */
public abstract class DelegatingExecutorService implements ExecutorService {

    /**
     * Subclass-accessible reference to the delegate executor service being wrapped.
     */
    protected final ExecutorService delegate;

    protected DelegatingExecutorService(ExecutorService delegate) {
        if (delegate == null) throw new IllegalArgumentException("No delegate executor service provided!");
        this.delegate = delegate;
    }

    public void shutdown() {
        delegate.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(task);
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(task, result);
    }

    public Future<?> submit(Runnable task) {
        return delegate.submit(task);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }

    public void execute(Runnable command) {
        delegate.execute(command);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other != null && getClass().equals(other.getClass())
                && delegate.equals(((DelegatingExecutorService) other).delegate));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + delegate + '}';
    }

}
