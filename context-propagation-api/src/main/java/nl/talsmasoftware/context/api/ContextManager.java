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
package nl.talsmasoftware.context.api;

/**
 * {@linkplain Context} manager service.
 *
 * <p>
 * Implementations must be made available through the {@linkplain java.util.ServiceLoader ServiceLoader}.<br>
 * For details how to make your implementation available, please see the documentation of {@link java.util.ServiceLoader}.
 *
 * @param <T> type of the context value
 * @author Sjoerd Talsma
 * @since 2.0.0
 */
public interface ContextManager<T> {

    /**
     * Initialize a new context containing the specified <code>value</code>.
     *
     * <p>
     * Whether the value is allowed to be <code>null</code> is up to the implementation.
     *
     * @param value The value to initialize a new context for.
     * @return The new <em>active</em> context containing the specified value
     * which should be closed by the caller at the end of its lifecycle from the same thread.
     */
    Context<T> initializeNewContext(T value);

    /**
     * The value of the currently active context, or {@code null} if no context is active.
     *
     * @return The value of the active context, or {@code null} if no context is active.
     * @since 2.0.0
     */
    T getActiveContextValue();

    /**
     * Clears the current context and any potential parent contexts that exist.
     *
     * <p>
     * This is an optional operation.<br>
     * When all initialized contexts are initialized in combination with try-with-resources blocks,
     * it is not necessary to call clear.
     *
     * <p>
     * The operation exists to allow thread pool management making sure
     * to clear all contexts before returning threads to the pool.
     *
     * <p>
     * This method normally should only get called by {@linkplain #clearAll()}.
     *
     * @since 2.0.0
     */
    void clear();

    /**
     * Clears all active contexts from the current thread.
     *
     * <p>
     * Contexts that are 'stacked' (i.e. restore the previous state upon close)
     * should be closed in a way that includes all 'parent' contexts as well.
     *
     * <p>
     * This operation is not intended to be used by general application code
     * as it likely breaks any 'stacked' active context that surrounding code may depend upon.
     *
     * <p>
     * Appropriate use includes thread management, where threads are reused by some pooling mechanism.<br>
     * For example, it is considered safe to clear the context when obtaining a 'fresh' thread from a
     * thread pool (as no context expectations should exist at that point).<br>
     * An even better strategy would be to clear the context right before returning a used thread
     * back to the pool as this will allow any unclosed contexts to be garbage collected.<br>
     * Besides preventing contextual issues, this reduces the risk of memory leaks by unbalanced context calls.
     *
     * @since 2.0.0
     */
    static void clearAll() {
        ContextSnapshotImpl.clearActiveContexts();
    }

    /**
     * Override the {@linkplain ClassLoader} used to lookup {@linkplain ContextManager context managers}.
     *
     * <p>
     * Normally, capturing a snapshot uses the {@linkplain Thread#getContextClassLoader() Context ClassLoader} from the
     * {@linkplain Thread#currentThread() current thread} to look up all {@linkplain ContextManager context managers}.
     * It is possible to configure a fixed, single classloader in your application for these lookups.
     *
     * <p>
     * Using this method to specify a fixed classloader will only impact
     * <strong>new</strong> {@linkplain ContextSnapshot context snapshots}.<br>
     * Existing snapshots will <strong>not</strong> be impacted.
     *
     * <p>
     * <strong>Notes:</strong><br>
     * <ul>
     * <li>Please be aware that this configuration is global!
     * <li>This will also affect the lookup of {@linkplain ContextTimer context timers}
     * </ul>
     *
     * @param classLoader The single, fixed ClassLoader to use for finding context managers.
     *                    Specify {@code null} to restore the default behaviour.
     * @since 2.0.0
     */
    static void useClassLoader(ClassLoader classLoader) {
        ServiceCache.useClassLoader(classLoader);
    }
}
