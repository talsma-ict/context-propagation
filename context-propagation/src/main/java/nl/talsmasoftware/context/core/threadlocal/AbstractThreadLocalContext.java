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
package nl.talsmasoftware.context.core.threadlocal;

import nl.talsmasoftware.context.Context;

import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class maintaining a shared, static {@link ThreadLocal} instance for each concrete subclass.
 * This threadlocal can be accessed by subclasses through the protected method:
 * {@link #threadLocalInstanceOf(Class)}.
 *
 * @author Sjoerd Talsma
 */
public abstract class AbstractThreadLocalContext<T> implements Context<T> {
    private static final Logger LOGGER = Logger.getLogger(AbstractThreadLocalContext.class.getName());

    /**
     * The constant of ThreadLocal context instances per subclass name so different types don't get mixed.
     */
    private static final ConcurrentMap<String, ThreadLocal<?>> INSTANCES =
            new ConcurrentHashMap<String, ThreadLocal<?>>();

    @SuppressWarnings("unchecked")
    private final ThreadLocal<AbstractThreadLocalContext<T>> sharedThreadLocalContext = threadLocalInstanceOf((Class) getClass());
    private volatile boolean closed = false;

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
        this.unwindIfNecessary(); // avoid unnecessary parentContexts
        this.parentContext = sharedThreadLocalContext.get();
        this.value = newValue;
        this.sharedThreadLocalContext.set(this);
        LOGGER.log(Level.FINEST, "Initialized new {0}.", this);
    }

    /**
     * Unwinds the sharedThreadLocalContext.
     *
     * @return The new 'current' context (not necessarily related to 'this' object).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private AbstractThreadLocalContext<T> unwindIfNecessary() {
        final AbstractThreadLocalContext<?> head = sharedThreadLocalContext.get();
        AbstractThreadLocalContext<?> current = head;
        while (current != null && current.closed) { // Current is closed: unwind!
            current = (AbstractThreadLocalContext<?>) current.parentContext;
        }
        if (current != head) { // refresh head if necessary.
            if (current == null) sharedThreadLocalContext.remove();
            else ((ThreadLocal) sharedThreadLocalContext).set(current);
        }
        return (AbstractThreadLocalContext<T>) current;
    }

    /**
     * Returns whether this context is closed or not.
     *
     * @return Whether the context is already closed.
     */
    protected boolean isClosed() {
        return closed;
    }

    /**
     * Returns the value of this context instance, or <code>null</code> if it was already closed.
     *
     * @return The value of this context.
     */
    public T getValue() {
        return closed ? null : value;
    }

    /**
     * Closes this context and in case this context is the active context,
     * restores the active context to the (unclosed) parent context.<br>
     * If no unclosed parent context exists, the 'active context' is cleared.
     * <p>
     * This method has no side-effects if the context was already closed (it is safe to call multiple times).
     */
    public void close() {
        closed = true;
        this.unwindIfNecessary(); // Remove this context created in the same thread.
        LOGGER.log(Level.FINEST, "Closed {0}.", this);
    }

    /**
     * Returns the classname of this context followed by <code>"{closed}"</code> if it has been closed already;
     * otherwise the contained {@link #getValue() value} by this context will be added.
     *
     * @return String representing this context class and either the current value or the fact that it was closed.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + (isClosed() ? "{closed}" : "{value=" + value + '}');
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
    @SuppressWarnings("unchecked")
    protected static <T, CTX extends AbstractThreadLocalContext<T>> ThreadLocal<CTX> threadLocalInstanceOf(
            final Class<? extends CTX> contextType) {
        if (contextType == null) throw new NullPointerException("The context type was <null>.");
        Class<?> type = contextType;
        String typeName = type.getName();
        if (!INSTANCES.containsKey(typeName)) {
            if (!AbstractThreadLocalContext.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Not a subclass of AbstractThreadLocalContext: " + type + '.');
            } else if (Modifier.isAbstract(contextType.getModifiers())) {
                throw new IllegalArgumentException("Context type was abstract: " + type + '.');
            }
            // Find the first non-abstract subclass of AbstractThreadLocalContext.
            while (!Modifier.isAbstract(type.getSuperclass().getModifiers())) {
                type = type.getSuperclass();
                typeName = type.getName();
            }
            // Atomically get-or-create the appropriate ThreadLocal instance.
            if (!INSTANCES.containsKey(typeName)) INSTANCES.putIfAbsent(typeName, new ThreadLocal());
        }
        return (ThreadLocal<CTX>) INSTANCES.get(typeName);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static <T, CTX extends AbstractThreadLocalContext<T>> CTX current(Class<? extends CTX> contextType) {
        final AbstractThreadLocalContext current = threadLocalInstanceOf(contextType).get();
        return (CTX) (current == null || !current.isClosed() ? current : current.unwindIfNecessary());
    }

}
