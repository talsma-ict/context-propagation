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
package nl.talsmasoftware.context.managers.locale;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;

import java.util.Locale;

/**
 * Manager for a current {@link Locale} bound to the current thread.
 *
 * <p>
 * Interaction with the current locale is through the {@linkplain CurrentLocaleHolder} class.
 * This manager just takes care of propagating it into other threads.
 *
 * @author Sjoerd Talsma
 */
public final class CurrentLocaleManager implements ContextManager<Locale> {
    /**
     * Singleton instance of this class.
     */
    private static final CurrentLocaleManager INSTANCE = new CurrentLocaleManager();

    /**
     * Returns the singleton instance of the {@code LocaleContextManager}.
     *
     * <p>
     * The ServiceLoader supports a static {@code provider()} method to resolve services since Java 9.
     *
     * @return The LocaleContext manager.
     * @see CurrentLocaleHolder
     */
    public static CurrentLocaleManager provider() {
        return INSTANCE;
    }

    /**
     * Creates a new current locale manager.
     *
     * <p>
     * Don't.<br>
     * Instead, use the {@linkplain #provider()} method to obtain the singleton instance.
     *
     * @see #provider()
     * @deprecated This constructor only exists for usage by Java 8 {@code ServiceLoader}. The singleton instance
     * obtained from {@link #provider()} should be used to avoid unnecessary instantiations.
     */
    @Deprecated
    public CurrentLocaleManager() {
    }

    /**
     * Registers the given {@linkplain Locale} value as the current Locale for the active thread
     * until the returned context is {@link Context#close() closed} again.
     *
     * @param value The new current Locale.
     * @return The context to be closed again by the caller to remove this locale as current locale.
     * @see CurrentLocaleHolder#set(Locale)
     */
    public Context<Locale> initializeNewContext(Locale value) {
        return CurrentLocaleHolder.set(value);
    }

    /**
     * @return The active {@code Locale} context or {@code null} if no such context is active in the current thread.
     * @see CurrentLocaleHolder#get()
     */
    public Locale getActiveContextValue() {
        return CurrentLocaleHolder.get().orElse(null);
    }

    /**
     * Unconditionally removes the active context (and any parents).
     *
     * <p>
     * This is useful for boundary filters, whose Threads may be returned to some thread pool.
     */
    public void clear() {
        CurrentLocaleHolder.clear();
    }

    /**
     * @return String representation.
     */
    public String toString() {
        return getClass().getSimpleName();
    }

}
