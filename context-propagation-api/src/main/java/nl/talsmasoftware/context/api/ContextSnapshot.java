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

/**
 * Snapshot capturing all active values from detected {@link ContextManager} implementations.
 *
 * <p>
 * Such a snapshot can be passed to another thread,
 * allowing all captured values to be reactivated in that other thread by a single method call.
 *
 * <p>
 * A context snapshot can be obtained from the {@linkplain #capture()} method.

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
 * @since 2.0.0
 */
public interface ContextSnapshot {
    /**
     * Captures a snapshot of the current
     * {@link ContextManager#getActiveContextValue() active context value}
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

}
