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
package nl.talsmasoftware.context.observer;

import nl.talsmasoftware.context.ContextManager;
import nl.talsmasoftware.context.ContextManagers;

/**
 * Utility class to assist Context implementors.
 * <p>
 * It implements the SPI behaviour, locating appropriate {@linkplain ContextObserver}
 * implementations to be notified of {@linkplain #onActivate(Class, Object, Object) activate}
 * and {@linkplain #onDeactivate(Class, Object, Object) deactivate} occurrances.
 *
 * @author Sjoerd Talsma
 * @see ContextManagers
 * @deprecated The (static) utility methods from this class were moved to the {@code ContextManagers} class.
 */
@Deprecated
public final class ContextObservers {

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private ContextObservers() {
        throw new UnsupportedOperationException();
    }

    /**
     * Notifies all {@linkplain ContextObserver context observers} for the specified {@code contextManager}
     * about the activated context value.
     *
     * @param contextManager        The context manager type that activated the context (required to observe).
     * @param activatedContextValue The activated context value or {@code null} if no value was activated.
     * @param previousContextValue  The previous context value or {@code null} if unknown or unsupported.
     * @param <T>                   The type managed by the context manager.
     * @see ContextManagers#onActivate(Class, Object, Object)
     * @deprecated This method was moved to the {@code ContextManagers} utility class.
     */
    @Deprecated
    public static <T> void onActivate(Class<? extends ContextManager<? super T>> contextManager,
                                      T activatedContextValue,
                                      T previousContextValue) {
        ContextManagers.onActivate(contextManager, activatedContextValue, previousContextValue);
    }

    /**
     * Notifies all {@linkplain ContextObserver context observers} for the specified {@code contextManager}
     * about the deactivated context value.
     *
     * @param contextManager          The context manager type that deactivated the context (required to observe).
     * @param deactivatedContextValue The deactivated context value
     * @param restoredContextValue    The restored context value or {@code null} if unknown or unsupported.
     * @param <T>                     The type managed by the context manager.
     * @see ContextManagers#onDeactivate(Class, Object, Object)
     * @deprecated This method was moved to the {@code ContextManagers} utility class.
     */
    @Deprecated
    public static <T> void onDeactivate(Class<? extends ContextManager<? super T>> contextManager,
                                        T deactivatedContextValue,
                                        T restoredContextValue) {
        ContextManagers.onDeactivate(contextManager, deactivatedContextValue, restoredContextValue);
    }

}
