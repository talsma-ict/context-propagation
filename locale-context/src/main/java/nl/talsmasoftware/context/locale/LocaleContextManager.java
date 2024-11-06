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
package nl.talsmasoftware.context.locale;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;

import java.util.Locale;

/**
 * Manager for a {@link Locale} bound to the current thread.
 *
 * @author Sjoerd Talsma
 */
public final class LocaleContextManager implements ContextManager<Locale> {
    /**
     * @return The {@code Locale} for the current thread, or {@code Locale.getDefault()} if no context was initialized.
     * @see Locale#getDefault()
     */
    public static Locale getCurrentLocaleOrDefault() {
        final Locale current = LocaleContext.currentValue();
        return current != null ? current : Locale.getDefault();
    }

    public static Context<Locale> setCurrentLocale(Locale locale) {
        return new LocaleContext(locale);
    }

    /**
     * Registers the given {@linkplain Locale} value as the current Locale for the active thread
     * until the returned context is {@link Context#close() closed} again.
     *
     * @param value The new current Locale.
     * @return The context to be closed again by the caller to remove this locale as current locale.
     */
    public Context<Locale> initializeNewContext(Locale value) {
        return setCurrentLocale(value);
    }

    /**
     * @return The active {@code Locale} context or {@code null} if no such context is active in the current thread.
     */
    public Locale getActiveContextValue() {
        return LocaleContext.currentValue();
    }

    /**
     * Unconditionally removes the active context (and any parents).
     * <p>
     * This is useful for boundary filters, whose Threads may be returned to some thread pool.
     */
    public void clear() {
        LocaleContext.clearAll();
    }

    /**
     * @return String representation for this context manager.
     */
    public String toString() {
        return getClass().getSimpleName();
    }

}
