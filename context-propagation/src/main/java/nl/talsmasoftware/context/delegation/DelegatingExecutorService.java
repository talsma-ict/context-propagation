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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Abstract baseclass that makes it a little easier to wrap existing {@link ExecutorService} implementations by
 * forwarding all methods to a {@link nl.talsmasoftware.context.core.delegation.Wrapper#delegate() delegate} executor service.<br>
 * The class also provides overridable <code>wrapper</code> methods for all complex input (e.g. {@link Callable}, {@link Runnable})
 * and result types (e.g. {@link Future}).
 * <p>
 * Although this class does implements <em>all</em> required methods of {@link ExecutorService} it is still declared
 * as an <em>abstract</em> class.<br>
 * This is because it does not provide any value in itself.
 *
 * @author Sjoerd Talsma
 * @deprecated Moved to package {@code nl.talsmasoftware.context.core.delegation}.
 */
@Deprecated
public abstract class DelegatingExecutorService extends nl.talsmasoftware.context.core.delegation.DelegatingExecutorService {

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
        return super.wrap(source);
    }

    protected Runnable wrap(Runnable source) {
        return super.wrap(source);
    }

    protected <T> Future<T> wrap(Future<T> source) {
        return super.wrap(source);
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
        return super.wrapTasks(tasks);
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
        return super.wrapFutures(futures);
    }
}
