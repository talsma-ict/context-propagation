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
package nl.talsmasoftware.context.managers.opentracing;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext;

/**
 * A {@linkplain ThreadLocal} based {@linkplain ScopeManager} implementation.
 *
 * <p>
 * Manages opentracing {@linkplain Scope} and allows it to be nested within another active scope,
 * taking care to restore the previous value when closing an active scope.
 *
 * <p>
 * This manager is based on our {@linkplain nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext}
 * implementation. Compared to the 'standard' {@linkplain io.opentracing.util.ThreadLocalScopeManager}
 * this implementation has the following advantages:
 * <ol>
 * <li>Close is explicitly idempotent; closing more than once has no additional side-effects
 * (even when finishOnClose is set to {@code true}).</li>
 * <li>More predictable behaviour for out-of-order closing of scopes.
 * Although this is explicitly unsupported by the opentracing specification,
 * we think having consistent and predictable behaviour is an advantage.
 * </ol>
 *
 * <p>
 * Please note that this scope manager is not somehow automatically enabled.
 * You will have to provide an instance to your tracer of choice when initializing it.
 *
 * <p>
 * The <em>active span</em> that is automatically propagated when using this
 * {@code opentracing-span-propagation} library in combination with
 * the context aware support classes is from the registered ScopeManager
 * from the {@linkplain io.opentracing.util.GlobalTracer}.
 *
 * @since 1.0.6
 */
public class ContextScopeManager implements ScopeManager, ContextManager<Span> {
    /**
     * Singleton instance of this class.
     */
    @SuppressWarnings("java:S1874") // This is the singleton instance the constructor was deprecated for.
    private static final ContextScopeManager INSTANCE = new ContextScopeManager();

    /**
     * Returns the singleton instance of the {@code ContextScopeManager}.
     *
     * <p>
     * The ServiceLoader supports a static {@code provider()} method to resolve services since Java 9.
     *
     * @return The OpenTracing scope manager.
     */
    public static ContextScopeManager provider() {
        return INSTANCE;
    }

    /**
     * Creates a new context manager.
     *
     * @see #provider()
     * @deprecated This constructor only exists for usage by Java 8 {@code ServiceLoader}. The singleton instance
     * obtained from {@link #provider()} should be used to avoid unnecessary instantiations.
     */
    @Deprecated
    @SuppressWarnings("java:S1133") // Code can only be removed if this library ever switches to Java 9+ compatibility.
    public ContextScopeManager() {
        super(); // no-op, default constructor for explicit deprecation.
    }

    /**
     * Set the specified Span as the active instance for the current context (usually a thread).
     *
     * @param span the {@link Span} that should become the {@link #activeSpan()}
     * @return The scope that <strong>must be</strong> closed in the same thread
     */
    @Override
    public ContextScope activate(Span span) {
        return new ThreadLocalSpanContext(span);
    }

    /**
     * Return the currently active {@linkplain Span}.
     *
     * @return The active {@linkplain Span}, or {@code null} if none could be found.
     */
    @Override
    public Span activeSpan() {
        return ThreadLocalSpanContext.currentSpan();
    }

    /**
     * Returns the {@linkplain #activeSpan() active Span}.
     *
     * @return The active {@linkplain Span} or {@code null} if none could be found.
     */
    @Override
    public Span getActiveContextValue() {
        return activeSpan();
    }

    /**
     * Clears <em>all</em> active spans from the current thread, including any parent contexts that may exist.
     */
    @Override
    public void clear() {
        ThreadLocalSpanContext.remove();
    }

    /**
     * String representation for this scope manager.
     *
     * @return String representation for this context manager.
     */
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * Interface implementing both {@link Context} and opentracing {@link Scope}.
     */
    public interface ContextScope extends Context, Scope {
    }

    private static final class ThreadLocalSpanContext extends AbstractThreadLocalContext<Span> implements ContextScope {
        private static final ThreadLocal<ThreadLocalSpanContext> SPAN_CONTEXT =
                AbstractThreadLocalContext.threadLocalInstanceOf(ThreadLocalSpanContext.class);

        private ThreadLocalSpanContext(Span newValue) {
            super(newValue);
        }

        private static Span currentSpan() {
            ThreadLocalSpanContext current = SPAN_CONTEXT.get();
            return current != null ? current.value : null;
        }

        private static void remove() {
            SPAN_CONTEXT.remove();
        }
    }
}
