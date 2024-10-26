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
 * The service definition a {@linkplain Context} manager.
 *
 * <p>
 * Implementations must be made available as <em>service provider</em>.<br>
 * For details how to make your implementation available, please see the documentation of {@link java.util.ServiceLoader}.
 *
 * @param <T> type of the context value
 * @author Sjoerd Talsma
 */
public interface ContextManager<T> extends nl.talsmasoftware.context.ContextManager<T> {

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
     * The currently active context, or <code>null</code> if no context is active.
     *
     * @return The active context or <code>null</code> if there is none.
     * @deprecated In favour of {@link #getActiveContextValue}
     */
    @Deprecated
    Context<T> getActiveContext();

    /**
     * The value of the currently active context, or {@code null} if no context is active.
     *
     * @return The value of the active context, or {@code null} if no context is active.
     * @see Context#getValue()
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
     * This method normally should only get called by {@code ContextManagers.clearActiveContexts()}.
     */
    void clear();

}
