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
package nl.talsmasoftware.context.managers.locale;

import nl.talsmasoftware.context.api.Context;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Holder for a current {@linkplain Locale}.
 *
 * <p>
 * A current locale can be <em>set</em> for the current thread as follows:
 * <pre>{@code
 * private void runWithLocale(Locale locale, Runnable someCode) {
 *     try (Context<Locale> ctx = CurrentLocaleHolder.set(locale)) {
 *         someCode.run();
 *     }
 * }
 * }</pre>
 *
 * <p>
 * Obtaining the current locale works similar:
 * <pre>{@code
 * private void someCode() {
 *     Optional<Locale> currentLocale = CurrentLocaleHolder.get();
 *     Locale currentOrDefaultLocale = CurrentLocaleHolder.getOrDefault(); // short for get().orElseGet(Locale::getDefault)
 *     // ...
 * }
 * }</pre>
 *
 * <p>
 * Code using {@code ContextAwareExecutorService} or {@code ContextAwareCompletableFuture} will automatically
 * have the current locale propagate to the background threads.
 *
 * @author Sjoerd Talsma
 */
public final class CurrentLocaleHolder implements Context {
    private static final Logger LOGGER = Logger.getLogger(CurrentLocaleHolder.class.getName());

    /**
     * ThreadLocal for the current locale.
     *
     * <p>
     * This constant is inheritable, meaning it will propagate into <em>new</em> Threads.<br>
     * Furthermore, the {@code set} method is made null-safe, removing the ThreadLocal when setting {@code null}.
     */
    private static final ThreadLocal<CurrentLocaleHolder> LOCALE = new InheritableThreadLocal<CurrentLocaleHolder>() {
        /**
         * Copies the current locale into a new holder for use in the new thread.
         *
         * @param parentValue The parent holder value.
         * @return A new holder for the child thread or {@code null} if the parent holder was completely closed.
         */
        @Override
        protected CurrentLocaleHolder childValue(CurrentLocaleHolder parentValue) {
            if (parentValue != null) parentValue = parentValue.unwind();
            return parentValue != null ? new CurrentLocaleHolder(null, parentValue.locale) : null;
        }

        /**
         * Null-safe set override that calls {@linkplain #remove()} when setting {@code null}.
         * @param holder the value to be stored in the thread-local or {@code null} to remove it.
         */
        @Override
        public void set(CurrentLocaleHolder holder) {
            if (holder == null) super.remove();
            else super.set(holder);
        }
    };

    private final CurrentLocaleHolder parent;
    private final Locale locale;
    private final AtomicBoolean closed;

    private CurrentLocaleHolder(CurrentLocaleHolder parent, Locale newValue) {
        this.parent = parent;
        this.locale = newValue;
        this.closed = new AtomicBoolean(false);
    }

    /**
     * Sets the current locale on the current thread until {@linkplain Context#close()} is called.
     *
     * @param locale The locale to become the current locale.
     * @return The context to restore the previous locale upon {@code close()}.
     */
    public static Context set(Locale locale) {
        CurrentLocaleHolder newHolder = new CurrentLocaleHolder(LOCALE.get(), locale);
        LOCALE.set(newHolder);
        return newHolder;
    }

    /**
     * Gets the current locale.
     *
     * @return the current locale.
     * @see #set(Locale)
     * @see #getOrDefault()
     */
    public static Optional<Locale> get() {
        return Optional.ofNullable(LOCALE.get()).map(holder -> holder.locale);
    }

    /**
     * Current locale or {@linkplain Locale#getDefault()} if none was set or its context was already closed.
     *
     * @return The current locale or {@code Locale.getDefault()}.
     * @see #set(Locale)
     * @see #get()
     * @see Locale#getDefault()
     */
    public static Locale getOrDefault() {
        return get().orElseGet(Locale::getDefault);
    }

    /**
     * Find the nearest unclosed holder from this holder.
     *
     * @return this holder if it is not closed, or the nearest unclosed parent. {@code null} if there aren't any.
     */
    private CurrentLocaleHolder unwind() {
        CurrentLocaleHolder holder = this;
        while (holder != null && holder.closed.get()) {
            holder = holder.parent;
        }
        return holder;
    }

    /**
     * Close the context. Restores the nearest unclosed parent locale as current.
     */
    public void close() {
        final boolean isCurrent = this == LOCALE.get();
        if (!closed.compareAndSet(false, true)) {
            LOGGER.finest("Current Locale holder closed repeatedly.");
        } else if (isCurrent) {
            LOCALE.set(this.unwind());
        } else {
            LOGGER.fine("Current Locale holder closed out-of-sequence! It was not the current locale.");
        }
    }

    /**
     * Return a string representation of this locale holder.
     *
     * @return String representation of this locale holder.
     */
    public String toString() {
        return getClass().getSimpleName() + (closed.get() ? "{closed}" : "{" + locale + "}");
    }

    /**
     * Unconditionally clears the entire {@link CurrentLocaleHolder} for the current thread.
     *
     * <p>
     * This can be useful when returning threads to a thread pool.
     */
    static void clear() {
        for (CurrentLocaleHolder current = LOCALE.get(); current != null; current = current.unwind()) {
            current.close();
        }
        LOCALE.remove();
    }
}
