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
package nl.talsmasoftware.context.threadlocal;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;
import nl.talsmasoftware.context.ContextManagers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Abstract base class maintaining a shared, static {@link ThreadLocal} instance for each concrete subclass.
 * This threadlocal can be accessed by subclasses through the protected method:
 * {@link #threadLocalInstanceOf(Class)}.
 *
 * @author Sjoerd Talsma
 * @deprecated Moved to package {@code nl.talsmasoftware.context.core.threadlocal}.
 */
@Deprecated
public abstract class AbstractThreadLocalContext<T> extends nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext<T> {
    private static final Logger LOGGER = Logger.getLogger(AbstractThreadLocalContext.class.getName());
    private static Class<? extends AbstractThreadLocalContext> contextType;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Class<? extends ContextManager<? super T>> contextManagerType;

    /**
     * The parent context that was active at the time this context was created (if any)
     * or <code>null</code> in case there was no active context when this context was created.
     */
    protected final Context<T> parentContext;

    /**
     * The actual value, so subclasses can still access it after the context has been closed,
     * because the default {@link #getValue()} implementation will return <code>null</code> in that case.<br>
     * Please be careful accessing the value after the context was closed:
     * There is no pre-defined meaningful way to handle this situation, as this depends heavily
     * on the desired features of the particular implementation.
     */
    protected final T value;

    /**
     * Instantiates a new context with the specified value.
     * The new context will be made the active context for the current thread.
     *
     * @param newValue The new value to become active in this new context
     *                 (or <code>null</code> to register a new context with 'no value').
     */
    protected AbstractThreadLocalContext(T newValue) {
        this(null, newValue);
    }

    /**
     * Instantiates a new context with the specified value.
     * The new context will be made the active context for the current thread.
     *
     * @param contextManagerType The context manager type (required to notify appropriate observers)
     * @param newValue           The new value to become active in this new context
     *                           (or <code>null</code> to register a new context with 'no value').
     */
    @SuppressWarnings("unchecked")
    protected AbstractThreadLocalContext(Class<? extends ContextManager<? super T>> contextManagerType, T newValue) {
        super(newValue);
        this.contextManagerType = contextManagerType;
        this.parentContext = super.parentContext;
        this.value = super.value;
        ContextManagers.onActivate(contextManagerType, value, parentContext == null ? null : parentContext.getValue());
    }

    /**
     * Returns whether this context is closed or not.
     *
     * @return Whether the context is already closed.
     */
    protected boolean isClosed() {
        return super.isClosed();
    }

    /**
     * Closes this context and in case this context is the active context,
     * restores the active context to the (unclosed) parent context.<br>
     * If no unclosed parent context exists, the 'active context' is cleared.
     * <p>
     * This method has no side-effects if the context was already closed (it is safe to call multiple times).
     */
    public synchronized void close() {
        final boolean observe = !super.isClosed();
        super.close();
        if (observe) ContextManagers.onDeactivate(contextManagerType, this.value, getValue());
    }

    /**
     * Returns the shared, static {@link ThreadLocal} instance for the specified context type.
     *
     * @param contextType The first concrete subclass of the abstract threadlocal context
     *                    (So values from separate subclasses do not get mixed up).
     * @param <T>         The type being managed by the context.
     * @param <CTX>       The first non-abstract context subclass of AbstractThreadLocalContext.
     * @return The non-<code>null</code> shared <code>ThreadLocal</code> instance to register these contexts on.
     */
    protected static <T, CTX extends nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext<T>> ThreadLocal<CTX> threadLocalInstanceOf(
            final Class<? extends CTX> contextType) {
        return nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext.threadLocalInstanceOf(contextType);
    }

    protected static <T, CTX extends nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext<T>> CTX current(Class<? extends CTX> contextType) {
        return nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext.current(contextType);
    }
}
