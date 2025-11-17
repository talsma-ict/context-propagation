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
package nl.talsmasoftware.context.api;

import java.io.Closeable;
import java.util.concurrent.Callable;

/**
 * Captures active values from all detected {@linkplain ContextManager} implementations.
 *
 * <p>
 * A context snapshot can be obtained from the {@linkplain #capture()} method.<br>
 * The captured values in this snapshot can be reactivated in another thread.
 *
 * <p>
 * <strong>Important:</strong> Make sure to <strong>always</strong> call {@link Reactivation#close()}
 * in the same thread after calling {@linkplain #reactivate()}.
 *
 * <p>
 * The module {@code context-propagation-core} contains several utility classes
 * named {@code ContextAware...} or {@code ...WithContext} that will automatically capture a new snapshot
 * and reactivate it for a particular callable or runnable piece of code,
 * making sure the reactivation is properly closed again.
 *
 * @author Sjoerd Talsma
 * @implNote Context snapshots are {@link java.io.Serializable Serializable} since version 2.0.4
 * and will include all Serializable values (including {@code null}s) that were captured.
 * Captured values that are not Serializable are skipped and therefore <strong>not</strong> present
 * in the deserialized context snapshot.
 * If, upon deserialization, a given context manager is no longer available,
 * its captured value is also excluded from the deserialized snapshot.
 * @since 2.0.0
 */
public interface ContextSnapshot {
    /**
     * Captures a snapshot of the current {@link ContextManager#getActiveContextValue() active context value}
     * from <em>all known {@link ContextManager}</em> implementations.
     *
     * <p>
     * This snapshot with context values is returned as a single object and can be temporarily
     * {@link ContextSnapshot#reactivate() reactivated}.
     * Remember to {@link Context#close() close} the reactivated context once you're done,
     * preferably in a <code>try-with-resources</code> construct.
     *
     * @return A new context snapshot that can be reactivated in another thread,
     * preferably in a try-with-resources construct.
     */
    static ContextSnapshot capture() {
        return ContextSnapshotImpl.capture();
    }

    /**
     * Temporarily reactivates all captured context values that are in the snapshot.
     *
     * <p>
     * Each reactivation <strong>must</strong> be closed in the same thread it was created,
     * preferably using a try-with-resources code block.
     *
     * <p>
     * Using <em>ContextAware</em> and <em>WithContext</em> classes from this library is a transparent way
     * to propagate context snapshots without having to worry about closing them.
     *
     * @return A new reactivation of the captured snapshot values.
     */
    Reactivation reactivate();

    /**
     * Context snapshot reactivation.
     *
     * <p>
     * Every snapshot reactivation <strong>must</strong> be closed in the same thread when the context snapshot is
     * no longer needed.
     *
     * <p>
     * It is <strong>strongly advised</strong> to only use reactivation with try-with-resources code blocks.
     *
     * @since 2.0.0
     */
    interface Reactivation extends Closeable {
        /**
         * Ends the contexts from the reactivated context snapshot.
         *
         * <p>
         * Depending on the {@link ContextManager}, the active context is either cleared or restored to the state
         * before the snapshot was activated.
         */
        void close();
    }

    /**
     * Wrap a callable in this context snapshot, reactivating it during the call.
     *
     * <p>
     * This provides the code being called with the thread-local values captured in this snapshot,
     * even if it is executed in a different thread.
     *
     * <p>
     * The reactivation is closed after the call finishes, ensuring that all thread-local values are properly cleaned up.
     *
     * @param callable The callable to be called with all context values from this snapshot.
     * @param <T>      The result type returned by the callable.
     * @return The wrapped callable.
     */
    default <T> Callable<T> wrap(final Callable<T> callable) {
        return () -> {
            //noinspection unused
            try (Reactivation reactivation = this.reactivate()) {
                return callable.call();
            }
        };
    }

    /**
     * Wrap a runnable in this context snapshot, reactivating it during the run.
     *
     * <p>
     * This provides the code to run with the thread-local values captured in this snapshot,
     * even if executed in a different thread.
     *
     * <p>
     * The reactivation is closed after the run finishes, ensuring that all thread-local values are properly cleaned up.
     *
     * @param runnable The runnable to run with all context values from this snapshot.
     * @return The wrapped runnable.
     */
    default Runnable wrap(final Runnable runnable) {
        return () -> {
            //noinspection unused
            try (Reactivation reactivation = this.reactivate()) {
                runnable.run();
            }
        };
    }

    /**
     * Gets the captured value from the specified {@link ContextManager} in this snapshot.
     *
     * <p>
     * Conceptually, this method does the following:
     * <pre>{@code
     * return this.wrap(contextManager::getActiveContextValue).call();
     * }</pre>
     *
     * <p>
     * This is useful when you need to inspect or access a specific context value from a captured snapshot
     * without manually managing the reactivation lifecycle.
     *
     * @param contextManager The context manager to retrieve the captured value for.
     * @param <T>            The type of the context value.
     * @return The captured context value for the specified context manager in this snapshot.
     * @implNote For efficiency, the single captured value is actually just returned without needing to reactivate the entire snapshot.
     * @since 2.0.3
     */
    <T> T getCapturedValue(ContextManager<T> contextManager);
}
