/*
 * Copyright 2016-2022 Talsma ICT
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
 * The contract for a ContextManager Service.
 *
 * <p>
 * Implementations can be registered by providing a fully qualified class name in a service file called:<br>
 * <code>"/META-INF/services/nl.talsmasoftware.context.ContextManager"</code><br>
 * That will take care of any active context being captured in {@link ContextSnapshot} instances
 * managed by the {@link ContextManagers} utility class.<br>
 * <b>Note:</b> <em>Make sure your implementation has a default (no-argument) constructor.</em>
 *
 * <p>
 * A context manager is required to notify
 * registered {@linkplain nl.talsmasoftware.context.observer.ContextObserver ContextObserver} instances
 * of context updates.<br>
 * The {@linkplain nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext AbstractThreadLocalContext}
 * already notifies these observers.<br>
 * Other implementations can use the {@linkplain ContextManagers#onActivate(Class, Object, Object)}
 * and {@linkplain ContextManagers#onDeactivate(Class, Object, Object)} methods
 * to notify the appropriate context observers.
 *
 * @param <T> type of the context value
 * @author Sjoerd Talsma
 */
public interface ContextManager<T> {

    /**
     * Initialize a new context containing the specified <code>value</code>.
     * <p>
     * Whether the value is allowed to be <code>null</code> is up to the implementation.
     *
     * @param value The value to initialize a new context for.
     * @return The new <em>active</em> context containing the specified value
     * which should be closed by the caller at the end of its lifecycle from the same thread.
     */
    Context<T> initializeNewContext(T value);

    /**
     * The currently active context, or <code>null</code> if no context is active.
     *
     * @return The active context or <code>null</code> if there is none.
     */
    Context<T> getActiveContext();

}
