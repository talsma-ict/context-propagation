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

import nl.talsmasoftware.context.ContextManager;

/**
 * Observe context updates for a particular {@linkplain ContextManager}.
 *
 * <p>
 * Context observers can be registered / unregistered with the {@code ContextManagers} class.
 *
 * <p>
 * <strong>Implementation note:</strong> A correct {@link Object#equals(Object)} implementation is required
 * for reliable registration functionality.
 *
 * @author Sjoerd Talsma
 * @since 1.1.0
 */
public interface ContextObserver<T> {

    /**
     * Indicates that a context <em>was just activated</em>.
     *
     * @param activatedContextValue The now active context value.
     * @param previousContextValue  The previous context value or {@code null} if unknown or unsupported.
     */
    void onActivate(T activatedContextValue, T previousContextValue);

    /**
     * Indicates that a context <em>was just deactivated</em>.
     *
     * @param deactivatedContextValue The deactivated context value.
     * @param restoredContextValue    The now active restored context value or {@code null} if unknown or unsupported.
     */
    void onDeactivate(T deactivatedContextValue, T restoredContextValue);

}
