/*
 * Copyright 2016-2017 Talsma ICT
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

/**
 * Interface for a 'snapshot' that can capture the (then-) 'active context' from all known registered
 * {@link ContextManager} implementations.<br>
 * Obtain a new snapshot by calling {@link ContextManagers#createContextSnapshot()} which will include a snapshot
 * for all supported {@link Context} types through their respective {@link ContextManager managers}.
 * <p>
 * This allows for a generic method to:
 * <ol>
 * <li>Create a new snapshot of the active context (for all registered ContextManagers).</li>
 * <li>Pass the returned <code>ContextSnapshot</code> along to a background job.</li>
 * <li>Allow the background job to (temporary) {@link #reactivate() reactivate} the snapshot
 * for some required code path.</li>
 * <li>The reactivation is also a {@link Context} of its own. Although it does not return any specific
 * {@link Context#getValue() value}, it must be {@link Context#close() closed} when the work requiring
 * the context snapshot is done. This prevents context values leaking in case the used threads
 * are returned to some pool.</li>
 * </ol>
 * <p>
 * <center><img src="ContextSnapshot.svg" alt="Context snapshot interface"></center>
 *
 * @author Sjoerd Talsma
 * @navassoc - reactivates * Context
 */
public interface ContextSnapshot {

    /**
     * This method activates all contained values by the snapshot in their respective {@link Context} implementations.
     * <p>
     * These reactivated contexts can all be closed at once by closing the returned <code>Void</code> context.
     * The resulting context is Void, because it does not contain any value itself.
     *
     * @return A new reactivationcontext with the snapshot values that will be valid until closed
     * (or new values are registered).
     */
    Context<Void> reactivate();

}
