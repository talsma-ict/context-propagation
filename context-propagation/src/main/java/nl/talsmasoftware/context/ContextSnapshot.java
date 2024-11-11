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
package nl.talsmasoftware.context;

import java.io.Closeable;

/**
 * Interface for a 'snapshot' that can capture the (then-) 'active context' from all known registered
 * {@link ContextManager} implementations.<br>
 * Obtain a new snapshot by calling {@link ContextManagers#createContextSnapshot()} which will include a snapshot
 * for all supported {@link nl.talsmasoftware.context.api.Context} types through their respective {@link ContextManager managers}.
 * <p>
 * This allows for a generic method to:
 * <ol>
 * <li>Create a new snapshot of the active context (for all registered ContextManagers).</li>
 * <li>Pass the returned <code>ContextSnapshot</code> along to a background job.</li>
 * <li>Allow the background job to (temporary) {@link #reactivate() reactivate} the snapshot
 * for some required code path.</li>
 * <li>The reactivation is also a {@link nl.talsmasoftware.context.api.Context} of its own. Although it does not return any specific
 * {@link nl.talsmasoftware.context.api.Context#getValue() value}, it must be {@link nl.talsmasoftware.context.api.Context#close() closed} when the work requiring
 * the context snapshot is done. This prevents context values leaking in case the used threads
 * are returned to some pool.</li>
 * </ol>
 *
 * @author Sjoerd Talsma
 * @deprecated Moved to api package.
 */
@Deprecated
public interface ContextSnapshot {

    /**
     * This method activates all contained values by the snapshot in their respective {@link nl.talsmasoftware.context.api.Context} implementations.
     * <p>
     * The reactivated context is of type {@code Void}, because it does not contain any value itself.
     * It closes all contained snapshot values collectively from its {@code close()} method.
     * <p>
     * <strong>Note:</strong> <em>The reactivated context <strong>must</strong> be closed from the same thread that
     * reactivated it. It is the responsibility of the caller to make sure this happens
     * (preferably in a try-with-resources block)</em><br>
     * Using the {@linkplain nl.talsmasoftware.context.executors.ContextAwareExecutorService ContextAwareExecutorService}
     * is a safe way to propagate context snapshots without having to worry about closing them.
     *
     * @return A new reactivation context with the snapshot values that will be valid until closed
     * (or new values are registered).
     */
    Closeable reactivate();

}
