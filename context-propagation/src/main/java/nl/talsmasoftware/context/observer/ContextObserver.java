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

/**
 * Observe context updates for a particular class of {@linkplain ContextManager}.
 *
 * <p>
 * Create your context observer by implementing this interface
 * and registering your class to the {@code ServiceLoader}
 * SPI by adding the fully qualified class name to the resource
 * {@code /META-INF/services/nl.talsmasoftware.context.observer.ContextObserver}.
 *
 * <p>
 * It is the responsibility of the context implementor to update observers of
 * context updates. SPI lookup of appropriate observers is facilitated
 * by the {@linkplain nl.talsmasoftware.context.ContextManagers#onActivate(Class, Object, Object)}
 * and {@linkplain nl.talsmasoftware.context.ContextManagers#onDeactivate(Class, Object, Object)}
 * utility methods.<br>
 * All subclasses of {@linkplain nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext}
 * are already observable.
 *
 * @author Sjoerd Talsma
 */
public interface ContextObserver<T> {

    /**
     * The observed context manager(s).
     *
     * <p>
     * Context observers can indicate which type of context manager must be observed using this method.
     * For instance, returning {@code LocaleContextManager.class} here, updates for the {@code LocaleContext} will
     * be offered to this observer.
     *
     * <p>
     * To observe <em>all</em> context updates, return the {@linkplain ContextManager} interface class itself,
     * since all context managers must implement it.
     *
     * <p>
     * Return {@code null} to disable the observer.
     *
     * @return The observed context manager class or {@code null} to disable this observer.
     */
    Class<? extends ContextManager<T>> getObservedContextManager();

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
