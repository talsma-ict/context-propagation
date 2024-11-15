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

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation for a current {@linkplain Locale} context.
 *
 * @author Sjoerd Talsma
 */
public final class LocaleContext implements Context<Locale> {
    private static final ThreadLocal<LocaleContext> LOCALE = new ThreadLocal<>();

    private final LocaleContext parent;
    private final Locale locale;
    private final AtomicBoolean closed;

    LocaleContext(Locale newValue) {
        this.parent = unwindClosed();
        this.locale = newValue;
        this.closed = new AtomicBoolean(false);
        LOCALE.set(this);
    }

    @Override
    public Locale getValue() {
        return locale;
    }

    private LocaleContext closeAndUnwind() {
        closed.set(true);
        return unwindClosed();
    }

    public void close() {
        closeAndUnwind();
    }

    public String toString() {
        return closed.get() ? "LocaleContext{closed}" : "LocaleContext{" + locale + "}";
    }

    /**
     * Current locale or {@code null} if none was set or its context was already closed.
     *
     * @return The current locale or {@code null}
     * @see #getOrDefault()
     * @see #set(Locale)
     */
    public static Locale get() {
        final LocaleContext current = unwindClosed();
        return current != null ? current.locale : null;
    }

    /**
     * Current locale or {@linkplain Locale#getDefault()} if none was set or its context was already closed.
     *
     * @return The current locale or {@code Locale.getDefault()}.
     * @see #get()
     * @see #set(Locale)
     */
    public static Locale getOrDefault() {
        Locale current = get();
        return current != null ? current : Locale.getDefault();
    }

    /**
     * Sets the current locale on the current thread until {@linkplain Context#close()} is called.
     *
     * @param locale The locale to become the current locale.
     * @return The context to restore the previous locale upon {@code close()}.
     */
    public static Context<Locale> set(Locale locale) {
        return new LocaleContext(locale);
    }

    /**
     * Unconditionally clears the entire {@link LocaleContext}.
     * This can be useful when returning threads to a thread pool.
     */
    static void clear() {
        LocaleContext current = unwindClosed();
        while (current != null) {
            current = current.closeAndUnwind();
        }
    }

    /**
     * Unwind closed contexts from the threadlocal until the first unclosed context is found.
     *
     * @return The current (unclosed) context or {@code null} if all contexts were closed.
     */
    private static LocaleContext unwindClosed() {
        // Find the first unclosed context.
        LocaleContext context = LOCALE.get();
        while (context != null && context.closed.get()) {
            context = context.parent;
        }

        // Set the found unclosed context and return it.
        if (context == null) LOCALE.remove();
        else LOCALE.set(context);
        return context;
    }
}
